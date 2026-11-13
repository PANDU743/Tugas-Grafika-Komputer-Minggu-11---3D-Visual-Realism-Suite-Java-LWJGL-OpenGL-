package com.grafkom.viewing3d;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import com.grafkom.viewing3d.ProjectionTaxonomyManager.ProjectionType;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

/**
 * 3D Viewing Interactive Visualizer  (Tugas Mandiri Minggu 10 - Grafika Komputer)
 *
 * Kelas utama: menyatukan keempat indikator.
 *   [1] Camera3D                    -> kamera orbital (mouse drag, scroll, tombol panah)
 *   [2] PerspectiveProjection       -> FOV / near / far / dolly zoom
 *   [3] StereoAnaglyphRenderer      -> tombol S (Anaglyph Red-Cyan), IPD dengan I / K
 *   [4] ProjectionTaxonomyManager   -> tombol 1-6 berpindah jenis proyeksi
 *
 * Minggu 11 (3D Visual Realism Suite): ditambah RealismScene (shading, HSR, tekstur, bayangan)
 * yang memakai kamera/proyeksi/stereo di atas. TAB berpindah antara adegan Realism dan Color Cube Minggu 10.
 */
public class ThreeDViewing {

    private static final float DEFAULT_RADIUS = 9.0f;   // Minggu 11: 6.0 -> 9.0 agar lantai & bayangan terlihat

    private long window;

    private Camera3D camera;                    // Indikator 1
    private PerspectiveProjection perspective; // Indikator 2
    private final StereoAnaglyphRenderer stereo = new StereoAnaglyphRenderer(); // Indikator 3
    private ProjectionType projectionType = ProjectionType.PERSPECTIVE_3POINT;   // Indikator 4

    private Mesh3D cube;
    private Mesh3D grid;

    private final RealismScene realism = new RealismScene();   // Minggu 11
    private boolean realismMode = true;                        // TAB: Realism <-> Color Cube Minggu 10
    private boolean vsync = true;                              // V
    private double fpsTimer = 0;
    private int fpsFrames = 0;
    private double fps = 0;

    private boolean stereoEnabled = false;
    private boolean dollyZoom = false;
    private float dollyConstant = 0.0f;   // radius * tan(FOV/2), dijaga konstan saat dolly zoom aktif

    private boolean dragging = false;
    private double lastMouseX;
    private double lastMouseY;

    private String lastTitle = "";

    public static void main(String[] args) {
        new ThreeDViewing().run();
    }

    public void run() {
        init();
        loop();
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        Callbacks.glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        if (callback != null) callback.free();
    }

    // ------------------------------------------------------------------
    // Inisialisasi
    // ------------------------------------------------------------------
    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal menginisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(1000, 700, "3D Visual Realism Suite", NULL, NULL);
        if (window == NULL) throw new IllegalStateException("Gagal membuat window GLFW");

        // Listener keyboard / mouse (Spesifikasi Tugas no. 3)
        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> onKey(key, action));
        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) dragging = (action == GLFW_PRESS);
        });
        glfwSetCursorPosCallback(window, (w, x, y) -> {
            if (dragging) {
                camera.orbit(-(float) (x - lastMouseX) * 0.4f, (float) (y - lastMouseY) * 0.4f);
            }
            lastMouseX = x;
            lastMouseY = y;
        });
        glfwSetScrollCallback(window, (w, dx, dy) -> {
            camera.zoom(-(float) dy * 0.5f);
            if (dollyZoom) dollyConstant = camera.getRadius() * tanHalfFov();
        });

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);
        glClearColor(0.08f, 0.08f, 0.10f, 1.0f);
        realism.init();                                         // Minggu 11: shader, mesh, tekstur, shadow FBO

        // Objek-objek aplikasi
        camera = new Camera3D(DEFAULT_RADIUS, projectionType.getAzimuthDeg(), projectionType.getElevationDeg());
        perspective = new PerspectiveProjection(45.0f, 1000.0f / 700.0f, 0.1f, 50.0f);
        cube = Mesh3D.createColorCube(2.0f);
        grid = Mesh3D.createGrid(6.0f, 12, -1.01f);
        selectProjection(projectionType);

        printHelp();
        System.out.println("Verifikasi LookAt manual vs JOML  : selisih maks = " + camera.verifyAgainstJoml());
        System.out.println("Verifikasi Perspektif manual vs JOML: selisih maks = " + perspective.verifyAgainstJoml());
    }

    // ------------------------------------------------------------------
    // Loop utama
    // ------------------------------------------------------------------
    private void loop() {
        long last = System.nanoTime();
        while (!glfwWindowShouldClose(window)) {
            long now = System.nanoTime();
            float dt = (now - last) / 1.0e9f;
            last = now;

            pollContinuousInput(dt);
            realism.update(dt);
            render();
            updateFps(dt);
            updateTitle();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    /** Tombol panah = orbit halus ; PageUp/PageDown = zoom. */
    private void pollContinuousInput(float dt) {
        float rot = 70.0f * dt;
        if (glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS)  camera.orbit(-rot, 0);
        if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) camera.orbit(rot, 0);
        if (glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS)    camera.orbit(0, rot);
        if (glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS)  camera.orbit(0, -rot);
        if (glfwGetKey(window, GLFW_KEY_PAGE_UP) == GLFW_PRESS)   camera.zoom(-4.0f * dt);
        if (glfwGetKey(window, GLFW_KEY_PAGE_DOWN) == GLFW_PRESS) camera.zoom(4.0f * dt);
    }

    // ------------------------------------------------------------------
    // Event keyboard
    // ------------------------------------------------------------------
    private void onKey(int key, int action) {
        boolean press = (action == GLFW_PRESS);
        boolean pressOrRepeat = (action == GLFW_PRESS || action == GLFW_REPEAT);
        if (!pressOrRepeat) return;

        // [INDIKATOR 4] tombol 1-6 -> ProjectionTaxonomyManager.ProjectionType
        if (key >= GLFW_KEY_1 && key <= GLFW_KEY_6 && press) {
            selectProjection(ProjectionType.values()[key - GLFW_KEY_1]);
            return;
        }

        // [MINGGU 11] tombol Realism Suite: B F Z C T G H O P L , .   (Z dipakai Z-Buffer, bukan FOV lagi)
        if (press && realism.handleKey(key)) return;

        switch (key) {
            case GLFW_KEY_TAB:                     // [MINGGU 11] Realism <-> Color Cube Minggu 10
                if (press) realismMode = !realismMode;
                break;
            case GLFW_KEY_V:                       // [MINGGU 11] VSync on/off (untuk uji FPS)
                if (press) { vsync = !vsync; glfwSwapInterval(vsync ? 1 : 0); }
                break;
            case GLFW_KEY_ESCAPE:
                glfwSetWindowShouldClose(window, true);
                break;
            case GLFW_KEY_S:                       // [INDIKATOR 3] toggle Anaglyph Stereo
                if (press) stereoEnabled = !stereoEnabled;
                break;
            case GLFW_KEY_I:                       // [INDIKATOR 3] IPD dinamis
                stereo.setIpd(stereo.getIpd() + 0.1f);
                break;
            case GLFW_KEY_K:
                stereo.setIpd(stereo.getIpd() - 0.1f);
                break;
            case GLFW_KEY_Q:                       // [INDIKATOR 2] FOV  (dipindah dari Z karena Z = Z-Buffer)
                changeFov(-2.0f);
                break;
            case GLFW_KEY_E:                       // [INDIKATOR 2] FOV  (dipindah dari X)
                changeFov(+2.0f);
                break;
            case GLFW_KEY_N:                       // [INDIKATOR 2] near plane (frustum clipping)
                perspective.setZNear(perspective.getZNear() - 1.0f);
                break;
            case GLFW_KEY_M:
                perspective.setZNear(perspective.getZNear() + 1.0f);
                break;
            case GLFW_KEY_D:                       // [INDIKATOR 2] Dolly Zoom
                if (press) {
                    dollyZoom = !dollyZoom;
                    if (dollyZoom) dollyConstant = camera.getRadius() * tanHalfFov();
                }
                break;
            case GLFW_KEY_R:                       // reset ke preset mode proyeksi
                if (press) {
                    perspective.setFovDegree(45.0f);
                    perspective.setZNear(0.1f);
                    dollyZoom = false;
                    selectProjection(projectionType);
                }
                break;
            default:
                break;
        }
    }

    private void selectProjection(ProjectionType type) {
        projectionType = type;
        camera.updateOrbitalPosition(DEFAULT_RADIUS, type.getAzimuthDeg(), type.getElevationDeg());
        if (dollyZoom) dollyConstant = camera.getRadius() * tanHalfFov();
    }

    private void changeFov(float delta) {
        perspective.setFovDegree(perspective.getFovDegree() + delta);
        if (dollyZoom) {
            // Dolly Zoom: ukuran objek tetap, hanya perspektif yang berubah
            camera.setRadius(dollyConstant / tanHalfFov());
        }
    }

    private float tanHalfFov() {
        return (float) Math.tan(Math.toRadians(perspective.getFovDegree()) / 2.0);
    }

    // ------------------------------------------------------------------
    // Render
    // ------------------------------------------------------------------
    private boolean stereoActive() {
        return stereoEnabled && projectionType.isPerspective();
    }

    private void render() {
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(window, w, h);
        if (w[0] == 0 || h[0] == 0) return;   // window diminimize

        glViewport(0, 0, w[0], h[0]);
        perspective.setAspectRatio((float) w[0] / (float) h[0]);

        if (realismMode) {
            realism.renderShadowPass(w[0], h[0]);               // [MINGGU 11] Render Pass 1 (dari sudut cahaya)
            glClearColor(0.53f, 0.68f, 0.85f, 1.0f);
        } else {
            glClearColor(0.08f, 0.08f, 0.10f, 1.0f);
        }
        realism.applyHsrState();                                // [MINGGU 11] Z-Buffer & Back-Face Culling

        if (stereoActive()) {
            // [INDIKATOR 3] zero parallax tepat di pusat objek
            stereo.setFocalLength(camera.getRadius());
            stereo.renderAnaglyph(camera, perspective, this::drawScene);
        } else {
            glColorMask(true, true, true, true);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // [INDIKATOR 4 + 2] proyeksi sesuai mode terpilih
            float orthoHalfHeight = camera.getRadius() * tanHalfFov();
            ProjectionTaxonomyManager.applyProjection(projectionType, perspective, orthoHalfHeight);

            // [INDIKATOR 1] View Matrix hasil perhitungan manual Camera3D
            GLMatrix.loadModelView(camera.calculateLookAtMatrix());
            drawScene();
        }
    }

    /** Callback adegan: dipanggil sekali (mono) atau dua kali (stereo, per mata). */
    private void drawScene() {
        if (realismMode) {
            realism.draw();                                     // [MINGGU 11] Render Pass 2 (shader)
        } else {
            grid.draw();
            cube.draw();
        }
    }

    private void updateFps(float dt) {
        fpsTimer += dt;
        fpsFrames++;
        if (fpsTimer >= 0.5) {
            fps = fpsFrames / fpsTimer;
            fpsTimer = 0;
            fpsFrames = 0;
        }
    }

    // ------------------------------------------------------------------
    // HUD (di judul window) & bantuan
    // ------------------------------------------------------------------
    private void updateTitle() {
        String stereoInfo;
        if (stereoActive()) {
            stereoInfo = String.format("Stereo ON (IPD %.2f)", stereo.getIpd());
        } else if (stereoEnabled) {
            stereoInfo = "Stereo: hanya untuk mode perspektif (4-6)";
        } else {
            stereoInfo = "Stereo OFF";
        }
        String title;
        if (realismMode) {
            title = String.format("3D Visual Realism Suite | %s | %s | %s | VSync %s | FPS %.0f",
                    projectionType.getLabel(), realism.statusText(), stereoInfo, vsync ? "ON" : "OFF", fps);
        } else {
            title = String.format("3D Viewing (Color Cube) | %s | FOV %.0f | near %.2f far %.0f | jarak %.1f | Dolly %s | %s | FPS %.0f",
                    projectionType.getLabel(), perspective.getFovDegree(),
                    perspective.getZNear(), perspective.getZFar(), camera.getRadius(),
                    dollyZoom ? "ON" : "OFF", stereoInfo, fps);
        }
        if (!title.equals(lastTitle)) {
            glfwSetWindowTitle(window, title);
            lastTitle = title;
        }
    }

    private void printHelp() {
        System.out.println("=== 3D Viewing Interactive Visualizer ===");
        System.out.println("Mouse drag kiri / tombol panah : orbit kamera");
        System.out.println("Scroll / PageUp / PageDown     : zoom (dolly)");
        System.out.println("1-6  : Orthographic, Isometric, Oblique Cavalier, Persp 1/2/3 titik hilang");
        System.out.println("S    : Anaglyph Stereo Red-Cyan on/off (mode perspektif)   I / K : IPD +/-");
        System.out.println("Q/E  : FOV -/+    N/M : near plane -/+    D : Dolly Zoom on/off");
        System.out.println("R    : reset sudut pandang & FOV      ESC : keluar");
        System.out.println("--- Minggu 11 : 3D Visual Realism Suite ---");
        System.out.println("B : Phong <-> Blinn-Phong      F : Flat <-> Smooth");
        System.out.println("Z : Z-Buffer on/off            C : Back-Face Culling on/off");
        System.out.println("T : Tekstur on/off             G : Nearest <-> Bilinear+Mipmap");
        System.out.println("H : Shadow Mapping on/off      , / . : Shadow Bias kecil / besar");
        System.out.println("O : ganti mesh (Sphere/Rumah/Torus)   P : putar objek   L : cahaya mengorbit");
        System.out.println("TAB : Realism <-> Color Cube Minggu 10     V : VSync on/off");
    }
}
