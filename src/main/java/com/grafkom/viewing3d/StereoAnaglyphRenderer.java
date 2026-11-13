package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

import org.joml.Matrix4f;

/**
 * [INDIKATOR 3 - STEREO VIEW]
 * Anaglyph Red-Cyan dengan:
 *   - Off-Axis Projection (frustum ASIMETRIS) per mata  -> PerspectiveProjection.calculateOffAxisMatrix()
 *   - Color Masking: mata kiri = kanal Merah, mata kanan = kanal Hijau+Biru (Cyan)
 *   - IPD dinamis (setIpd) dan callback Runnable untuk menggambar adegan.
 */
public class StereoAnaglyphRenderer {

    public static final float MIN_IPD = 0.0f;
    public static final float MAX_IPD = 1.0f;

    private float ipd = 0.25f;           // Interpupillary Distance (satuan dunia)
    private float focalLength = 6.0f;    // jarak bidang zero-parallax (bidang layar)

    /**
     * @param camera              kamera orbital (sumber View Matrix)
     * @param projection          parameter frustum (FOV, aspect, near, far)
     * @param renderSceneCallback menggambar adegan; dipanggil dua kali (mata kiri, mata kanan)
     */
    public void renderAnaglyph(Camera3D camera, PerspectiveProjection projection, Runnable renderSceneCallback) {
        Matrix4f view = camera.calculateLookAtMatrix();

        // Bersihkan SEMUA kanal warna + depth satu kali (color mask harus penuh saat clear).
        glColorMask(true, true, true, true);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // PASS 1: Mata Kiri -> hanya kanal Merah
        glColorMask(true, false, false, true);
        renderEye(-ipd / 2.0f, view, projection, renderSceneCallback);

        // PASS 2: Mata Kanan -> kanal Hijau + Biru (Cyan)
        glColorMask(false, true, true, true);
        glClear(GL_DEPTH_BUFFER_BIT);   // hanya depth yang dibersihkan; color buffer ditumpuk (blend kanal)
        renderEye(+ipd / 2.0f, view, projection, renderSceneCallback);

        // Reset color mask ke kondisi normal
        glColorMask(true, true, true, true);
    }

    private void renderEye(float eyeOffsetX, Matrix4f view, PerspectiveProjection projection, Runnable scene) {
        // Proyeksi asimetris untuk mata ini
        GLMatrix.loadProjection(projection.calculateOffAxisMatrix(eyeOffsetX, focalLength));

        // Mata digeser sejauh eyeOffsetX pada sumbu-x kamera => dunia digeser -eyeOffsetX di view space
        Matrix4f eyeView = new Matrix4f().translation(-eyeOffsetX, 0.0f, 0.0f).mul(view);
        GLMatrix.loadModelView(eyeView);

        scene.run();
    }

    public void setIpd(float ipd)               { this.ipd = Math.max(MIN_IPD, Math.min(MAX_IPD, ipd)); }
    public void setFocalLength(float focalLength) { this.focalLength = Math.max(0.5f, focalLength); }
    public float getIpd()          { return ipd; }
    public float getFocalLength()  { return focalLength; }
}
