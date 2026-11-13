package com.grafkom.viewing3d;

import org.joml.Matrix4f;

/**
 * [INDIKATOR 2 - PROYEKSI PERSPEKTIF]
 * Menghitung matriks proyeksi perspektif SECARA EKSPLISIT dari parameter frustum
 * (FOV_y, aspect ratio, near, far) sesuai Teori 2.2.
 * Juga menyediakan frustum ASIMETRIS (off-axis) yang dipakai oleh stereo view (Indikator 3).
 */
public class PerspectiveProjection {

    public static final float MIN_FOV = 20.0f;
    public static final float MAX_FOV = 90.0f;

    private float fovDegree;
    private float aspectRatio;
    private float zNear;
    private float zFar;

    public PerspectiveProjection(float fovDegree, float aspectRatio, float zNear, float zFar) {
        this.fovDegree = fovDegree;
        this.aspectRatio = aspectRatio;
        this.zNear = zNear;
        this.zFar = zFar;
    }

    /** Matriks perspektif simetris (rumus modul): f = 1/tan(FOV/2). */
    public Matrix4f calculateProjectionMatrix() {
        float fovRad = (float) Math.toRadians(fovDegree);
        float f = 1.0f / (float) Math.tan(fovRad / 2.0f);

        Matrix4f proj = new Matrix4f();
        proj.zero();
        proj.m00(f / aspectRatio);
        proj.m11(f);
        proj.m22(-(zFar + zNear) / (zFar - zNear));
        proj.m32(-(2.0f * zFar * zNear) / (zFar - zNear));
        proj.m23(-1.0f);
        proj.m33(0.0f);
        return proj;
    }

    /** Matriks frustum umum (setara glFrustum) dengan batas left/right/bottom/top pada bidang near. */
    public static Matrix4f calculateFrustumMatrix(float l, float r, float b, float t, float n, float f) {
        Matrix4f m = new Matrix4f();
        m.zero();
        m.m00(2.0f * n / (r - l));
        m.m11(2.0f * n / (t - b));
        m.m20((r + l) / (r - l));
        m.m21((t + b) / (t - b));
        m.m22(-(f + n) / (f - n));
        m.m32(-(2.0f * f * n) / (f - n));
        m.m23(-1.0f);
        return m;
    }

    /**
     * Frustum ASIMETRIS (Off-Axis) untuk satu mata.
     * @param eyeOffsetX  posisi mata terhadap pusat kamera pada sumbu-x (kiri: -IPD/2, kanan: +IPD/2)
     * @param focalLength jarak bidang zero-parallax (bidang layar) dari kamera
     * Tepi frustum digeser sebesar  -eyeOffsetX * near / focalLength  sehingga jendela pada bidang
     * zero-parallax sama untuk kedua mata (objek di bidang itu tidak punya disparitas).
     */
    public Matrix4f calculateOffAxisMatrix(float eyeOffsetX, float focalLength) {
        float top = zNear * (float) Math.tan(Math.toRadians(fovDegree) / 2.0);
        float bottom = -top;
        float halfWidth = top * aspectRatio;
        float shift = -eyeOffsetX * zNear / focalLength;
        return calculateFrustumMatrix(-halfWidth + shift, halfWidth + shift, bottom, top, zNear, zFar);
    }

    /** Verifikasi terhadap JOML perspective(): selisih maksimum elemen matriks. */
    public float verifyAgainstJoml() {
        float[] a = calculateProjectionMatrix().get(new float[16]);
        float[] b = new Matrix4f()
                .perspective((float) Math.toRadians(fovDegree), aspectRatio, zNear, zFar)
                .get(new float[16]);
        float maxErr = 0f;
        for (int i = 0; i < 16; i++) maxErr = Math.max(maxErr, Math.abs(a[i] - b[i]));
        return maxErr;
    }

    // Getter/Setter untuk pengujian interaktif FOV (Dolly Zoom) & frustum clipping
    public void setFovDegree(float fovDegree) {
        this.fovDegree = Math.max(MIN_FOV, Math.min(MAX_FOV, fovDegree));
    }
    public void setAspectRatio(float aspectRatio) { this.aspectRatio = aspectRatio; }
    public void setZNear(float zNear) { this.zNear = Math.max(0.1f, Math.min(zNear, zFar - 1.0f)); }
    public void setZFar(float zFar)   { this.zFar = Math.max(zFar, zNear + 1.0f); }

    public float getFovDegree()   { return fovDegree; }
    public float getAspectRatio() { return aspectRatio; }
    public float getZNear()       { return zNear; }
    public float getZFar()        { return zFar; }
}
