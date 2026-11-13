package com.grafkom.viewing3d;

import org.joml.Matrix4f;

/**
 * [INDIKATOR 4 - TAKSONOMI PROYEKSI]
 * Enum switcher 6 jenis proyeksi (>= 5 -> nilai "Sangat Baik" pada rubrik):
 *   Paralel   : ORTHOGRAPHIC, ISOMETRIC (aksonometri), OBLIQUE_CAVALIER
 *   Perspektif: PERSPECTIVE_1POINT, PERSPECTIVE_2POINT, PERSPECTIVE_3POINT
 * Dipilih dengan tombol keyboard 1-6 (lihat ThreeDViewing.onKey).
 *
 * Catatan: jumlah titik hilang pada perspektif ditentukan oleh ORIENTASI kamera terhadap sumbu objek,
 * bukan oleh matriksnya. Maka tiap mode membawa "preset sudut pandang" (azimuth, elevasi).
 */
public final class ProjectionTaxonomyManager {

    public enum ProjectionType {
        //            label                              azimuth elevasi  perspektif
        ORTHOGRAPHIC      ("1. Orthographic (tampak depan)", 0f,  0f,      false),
        ISOMETRIC         ("2. Isometric (aksonometri)",    45f, 35.264f,  false),
        OBLIQUE_CAVALIER  ("3. Oblique Cavalier",            0f,  0f,      false),
        PERSPECTIVE_1POINT("4. Perspektif 1 titik hilang",   0f,  0f,      true),
        PERSPECTIVE_2POINT("5. Perspektif 2 titik hilang",  35f,  0f,      true),
        PERSPECTIVE_3POINT("6. Perspektif 3 titik hilang",  35f, 30f,      true);

        private final String label;
        private final float azimuthDeg;
        private final float elevationDeg;
        private final boolean perspective;

        ProjectionType(String label, float azimuthDeg, float elevationDeg, boolean perspective) {
            this.label = label;
            this.azimuthDeg = azimuthDeg;
            this.elevationDeg = elevationDeg;
            this.perspective = perspective;
        }
        public String getLabel()        { return label; }
        public float getAzimuthDeg()    { return azimuthDeg; }
        public float getElevationDeg()  { return elevationDeg; }
        public boolean isPerspective()  { return perspective; }
    }

    private ProjectionTaxonomyManager() { }

    /** Menghitung matriks proyeksi untuk jenis yang dipilih. orthoHalfHeight = setengah tinggi volume pandang paralel. */
    public static Matrix4f getProjectionMatrix(ProjectionType type, PerspectiveProjection persp, float orthoHalfHeight) {
        float aspect = persp.getAspectRatio();
        float n = persp.getZNear();
        float f = persp.getZFar();
        float halfH = orthoHalfHeight;
        float halfW = orthoHalfHeight * aspect;

        switch (type) {
            case ORTHOGRAPHIC:
            case ISOMETRIC:
                return calculateOrthoMatrix(-halfW, halfW, -halfH, halfH, n, f);
            case OBLIQUE_CAVALIER:
                // Cavalier: skala kedalaman 1:1, sudut kemiringan 45 derajat.
                // P = Ortho x Shear  (shear dikerjakan lebih dulu pada koordinat view)
                return new Matrix4f(calculateOrthoMatrix(-halfW, halfW, -halfH, halfH, n, f))
                        .mul(calculateObliqueShear(1.0f, 45.0f));
            case PERSPECTIVE_1POINT:
            case PERSPECTIVE_2POINT:
            case PERSPECTIVE_3POINT:
            default:
                return persp.calculateProjectionMatrix();
        }
    }

    /** Memuat matriks proyeksi ke OpenGL (GL_PROJECTION). */
    public static void applyProjection(ProjectionType type, PerspectiveProjection persp, float orthoHalfHeight) {
        GLMatrix.loadProjection(getProjectionMatrix(type, persp, orthoHalfHeight));
    }

    /** Proyeksi ortografik (setara glOrtho), dihitung manual. */
    public static Matrix4f calculateOrthoMatrix(float l, float r, float b, float t, float n, float f) {
        Matrix4f m = new Matrix4f();
        m.m00(2.0f / (r - l));
        m.m11(2.0f / (t - b));
        m.m22(-2.0f / (f - n));
        m.m30(-(r + l) / (r - l));
        m.m31(-(t + b) / (t - b));
        m.m32(-(f + n) / (f - n));
        return m;
    }

    /**
     * Matriks shearing proyeksi oblique: titik yang makin jauh (z view makin negatif)
     * bergeser ke kanan-atas:  x' = x - k*cos(a)*z ,  y' = y - k*sin(a)*z.
     * k = 1.0 -> Cavalier ; k = 0.5 -> Cabinet.
     */
    public static Matrix4f calculateObliqueShear(float k, float angleDeg) {
        float a = (float) Math.toRadians(angleDeg);
        Matrix4f s = new Matrix4f();
        s.m20(-k * (float) Math.cos(a));
        s.m21(-k * (float) Math.sin(a));
        return s;
    }
}
