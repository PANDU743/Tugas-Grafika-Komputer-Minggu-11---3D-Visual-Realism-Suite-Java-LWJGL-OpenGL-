package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * [MINGGU 11 - 3D VISUAL REALISM SUITE]
 * Adegan realism yang menumpang di atas kamera, proyeksi, dan stereo Minggu 10:
 * matriks View/Projection yang sudah dimuat Camera3D / ProjectionTaxonomyManager /
 * StereoAnaglyphRenderer ke stack OpenGL dibaca ulang lalu dikirim ke shader sebagai uniform,
 * sehingga ke-6 jenis proyeksi, FOV, near/far, dan Anaglyph Stereo ikut bekerja pada adegan ini.
 *
 * Kontrol (ditangani di handleKey):
 *   B = Phong <-> Blinn-Phong        (Indikator 1)
 *   F = Flat  <-> Smooth shading     (Indikator 2)
 *   Z = Z-Buffer on/off, C = Back-Face Culling on/off   (Indikator 3)
 *   T = Tekstur on/off, G = Nearest <-> Bilinear+Mipmap (Indikator 4)
 *   H = Shadow Mapping on/off, , dan . = Shadow Bias    (Indikator 5)
 *   O = ganti mesh, P = putar objek, L = cahaya mengorbit
 */
public class RealismScene {

    private boolean blinnPhong = true;
    private boolean smooth = true;
    private boolean depthTest = true;
    private boolean culling = true;
    private boolean useTexture = true;
    private boolean bilinearMip = true;
    private boolean shadows = true;
    private boolean autoRotate = true;
    private boolean orbitLight = true;
    private float biasMin = 0.001f;
    private int meshIndex = 0;

    private float objAngle = 0f;
    private float lightAngle = 0.6f;

    private final String[] meshNames = {"Sphere", "Rumah 3D", "Torus"};
    private Mesh[] meshes;
    private Mesh ground;
    private Texture2D brickTex, floorTex;
    private ShadowMapFBO shadowFbo;
    private ShaderProgram mainShader, depthShader;

    // matriks cahaya & model dihitung per frame di renderShadowPass(), dipakai ulang di draw()
    private final Matrix4f lightSpace = new Matrix4f();
    private final Vector3f lightPos = new Vector3f();
    private final Matrix4f groundModel = new Matrix4f();
    private final Matrix4f objModel = new Matrix4f();
    private final float[] glBuf = new float[16];

    /** Dipanggil setelah GL.createCapabilities(). */
    public void init() {
        mainShader = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        depthShader = new ShaderProgram("/shaders/depth.vert", "/shaders/depth.frag");
        meshes = new Mesh[]{
                new Mesh(MeshFactory.sphere(1f, 48, 64)),
                new Mesh(MeshFactory.house()),
                new Mesh(MeshFactory.torus(0.9f, 0.4f, 64, 32))
        };
        ground = new Mesh(MeshFactory.ground(6f, -1.5f, 6f));
        brickTex = new Texture2D("/textures/brick.png");
        floorTex = new Texture2D("/textures/floor.png");
        shadowFbo = new ShadowMapFBO();
    }

    public void update(float dt) {
        if (autoRotate) objAngle += dt * 0.6f;
        if (orbitLight) lightAngle += dt * 0.5f;
    }

    /** @return true bila tombol ini milik Realism Suite (sudah ditangani). */
    public boolean handleKey(int key) {
        switch (key) {
            case GLFW_KEY_B -> blinnPhong = !blinnPhong;
            case GLFW_KEY_F -> smooth = !smooth;
            case GLFW_KEY_Z -> depthTest = !depthTest;
            case GLFW_KEY_C -> culling = !culling;
            case GLFW_KEY_T -> useTexture = !useTexture;
            case GLFW_KEY_G -> {
                bilinearMip = !bilinearMip;
                brickTex.setFilterMode(bilinearMip);
                floorTex.setFilterMode(bilinearMip);
            }
            case GLFW_KEY_H -> shadows = !shadows;
            case GLFW_KEY_COMMA -> biasMin = Math.max(0.00005f, biasMin / 2f);
            case GLFW_KEY_PERIOD -> biasMin = Math.min(0.05f, biasMin * 2f);
            case GLFW_KEY_O -> meshIndex = (meshIndex + 1) % meshes.length;
            case GLFW_KEY_P -> autoRotate = !autoRotate;
            case GLFW_KEY_L -> orbitLight = !orbitLight;
            default -> { return false; }
        }
        System.out.println("[REALISM] " + statusText());
        return true;
    }

    /** Status singkat untuk judul window / konsol. */
    public String statusText() {
        return String.format("%s | %s | %s | Z:%s C:%s | Tex:%s(%s) | Shadow:%s bias=%.5f",
                meshNames[meshIndex],
                blinnPhong ? "Blinn-Phong" : "Phong",
                smooth ? "Smooth" : "Flat",
                depthTest ? "ON" : "OFF", culling ? "ON" : "OFF",
                useTexture ? "ON" : "OFF", bilinearMip ? "Bilinear+Mip" : "Nearest",
                shadows ? "ON" : "OFF", biasMin);
    }

    /** Z-Buffer & Back-Face Culling sesuai toggle (Indikator 3). Dipanggil sebelum menggambar adegan. */
    public void applyHsrState() {
        HSRManager.setupDepthTest(depthTest);
        HSRManager.setupBackFaceCulling(culling);
    }

    // ------------------------------------------------------------------------------------------
    // Render Pass 1 (Depth Pass) dari sudut pandang CAHAYA  (Indikator 5)
    // Dipanggil SEKALI per frame, di luar callback stereo.
    // ------------------------------------------------------------------------------------------
    public void renderShadowPass(int fbW, int fbH) {
        lightPos.set(5f * (float) Math.cos(lightAngle), 6f, 5f * (float) Math.sin(lightAngle));
        // Light Space: Ortografik -> P_light * V_light
        lightSpace.identity()
                .ortho(-8f, 8f, -8f, 8f, 1f, 22f)
                .lookAt(lightPos, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        groundModel.identity();
        objModel.identity().rotateY(objAngle);

        if (!shadows) return;

        shadowFbo.bindForWriting();
        HSRManager.setupDepthTest(true);        // pass bayangan selalu memakai depth test
        HSRManager.setupBackFaceCulling(false); // caster dirender utuh
        depthShader.use();
        depthShader.setMat4("lightSpaceMatrix", lightSpace);
        depthShader.setMat4("model", groundModel);
        ground.draw(false);
        depthShader.setMat4("model", objModel);
        meshes[meshIndex].draw(smooth);
        glUseProgram(0);
        shadowFbo.unbind(fbW, fbH);
    }

    // ------------------------------------------------------------------------------------------
    // Render Pass 2 (Lighting Pass): callback adegan. Dipanggil 1x (mono) atau 2x (stereo).
    // ------------------------------------------------------------------------------------------
    public void draw() {
        // Ambil View & Projection yang sudah dimuat oleh Camera3D / ProjectionTaxonomyManager / Stereo
        glGetFloatv(GL_PROJECTION_MATRIX, glBuf);
        Matrix4f projection = new Matrix4f().set(glBuf);
        glGetFloatv(GL_MODELVIEW_MATRIX, glBuf);
        Matrix4f view = new Matrix4f().set(glBuf);
        Vector3f eye = view.invert(new Matrix4f()).transformPosition(new Vector3f()); // posisi mata (benar utk stereo)

        mainShader.use();
        mainShader.setMat4("view", view);
        mainShader.setMat4("projection", projection);
        mainShader.setMat4("lightSpaceMatrix", lightSpace);
        mainShader.setVec3("lightPos", lightPos);
        mainShader.setVec3("viewPos", eye);
        mainShader.setVec3("lightColor", 1f, 1f, 1f);
        mainShader.setBool("useBlinnPhong", blinnPhong);   // B
        mainShader.setBool("useTexture", useTexture);      // T
        mainShader.setBool("useShadow", shadows);          // H
        mainShader.setFloat("biasMin", biasMin);
        mainShader.setInt("textureSampler", 0);
        mainShader.setInt("shadowMap", 1);

        shadowFbo.bindTexture(1);

        // Lantai
        floorTex.bind(0);
        mainShader.setMat4("model", groundModel);
        mainShader.setVec3("objectColor", 0.65f, 0.65f, 0.7f);
        mainShader.setFloat("specularStrength", 0.15f);
        ground.draw(false);

        // Objek utama
        brickTex.bind(0);
        mainShader.setMat4("model", objModel);
        mainShader.setVec3("objectColor", 0.9f, 0.45f, 0.25f);
        mainShader.setFloat("specularStrength", 0.8f);
        meshes[meshIndex].draw(smooth);                    // F (Flat/Smooth)

        glUseProgram(0);                 // kembali ke fixed-function agar kode Minggu 10 tetap normal
        glActiveTexture(GL_TEXTURE0);
    }
}
