package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * [INDIKATOR 1 - CAMERA]
 * Kelas kamera 3D yang terenkapsulasi: eye, target, up bersifat private,
 * diakses lewat getter (mengembalikan salinan) dan diubah lewat method terkontrol.
 * Matriks LookAt (View Matrix) dihitung MANUAL (n, u, v) sesuai Teori 1.2 dan 1.3.
 * Navigasi: kamera ORBITAL (turntable) memakai koordinat sferis (radius, azimuth, elevasi).
 */
public class Camera3D {

    public static final float MIN_RADIUS = 2.0f;
    public static final float MAX_RADIUS = 40.0f;
    public static final float MAX_ELEVATION = 89.0f; // dibatasi agar Up tidak sejajar n

    private final Vector3f eye;
    private final Vector3f target;
    private final Vector3f up;

    // State sferis untuk kamera orbital
    private float radius;
    private float azimuthDeg;
    private float elevationDeg;

    /** Konstruktor sesuai modul (Praktikum 1). State sferis diturunkan dari eye & target. */
    public Camera3D(Vector3f eye, Vector3f target, Vector3f up) {
        this.eye = new Vector3f(eye);
        this.target = new Vector3f(target);
        this.up = new Vector3f(up);

        Vector3f d = new Vector3f(eye).sub(target);
        this.radius = d.length();
        this.azimuthDeg = (float) Math.toDegrees(Math.atan2(d.x, d.z));
        this.elevationDeg = (float) Math.toDegrees(Math.asin(d.y / this.radius));
    }

    /** Konstruktor praktis untuk kamera orbital yang menatap titik target (0,0,0). */
    public Camera3D(float radius, float azimuthDeg, float elevationDeg) {
        this(new Vector3f(0, 0, radius), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        updateOrbitalPosition(radius, azimuthDeg, elevationDeg);
    }

    // ------------------------------------------------------------------
    // Perhitungan manual Matriks LookAt (View Matrix)  -> Teori 1.2 & 1.3
    // ------------------------------------------------------------------
    public Matrix4f calculateLookAtMatrix() {
        Vector3f n = new Vector3f();
        eye.sub(target, n).normalize();      // n = (E - At) / ||E - At||   (View Plane Normal)

        Vector3f u = new Vector3f();
        up.cross(n, u).normalize();          // u = (Up x n) / ||Up x n||   (Right Vector)

        Vector3f v = new Vector3f();
        n.cross(u, v);                       // v = n x u                   (True Up Vector)

        // Baris 1: u | -dot(u,E)   Baris 2: v | -dot(v,E)   Baris 3: n | -dot(n,E)
        Matrix4f viewMatrix = new Matrix4f();
        viewMatrix.m00(u.x).m10(u.y).m20(u.z).m30(-u.dot(eye));
        viewMatrix.m01(v.x).m11(v.y).m21(v.z).m31(-v.dot(eye));
        viewMatrix.m02(n.x).m12(n.y).m22(n.z).m32(-n.dot(eye));
        viewMatrix.m03(0.0f).m13(0.0f).m23(0.0f).m33(1.0f);
        return viewMatrix;
    }

    // ------------------------------------------------------------------
    // Navigasi kamera orbital (koordinat sferis)
    // ------------------------------------------------------------------
    public void updateOrbitalPosition(float radius, float azimuthDeg, float elevationDeg) {
        this.radius = clamp(radius, MIN_RADIUS, MAX_RADIUS);
        this.azimuthDeg = azimuthDeg % 360.0f;
        this.elevationDeg = clamp(elevationDeg, -MAX_ELEVATION, MAX_ELEVATION);

        float azRad = (float) Math.toRadians(this.azimuthDeg);
        float elRad = (float) Math.toRadians(this.elevationDeg);
        this.eye.x = target.x + this.radius * (float) (Math.cos(elRad) * Math.sin(azRad));
        this.eye.y = target.y + this.radius * (float) (Math.sin(elRad));
        this.eye.z = target.z + this.radius * (float) (Math.cos(elRad) * Math.cos(azRad));
    }

    /** Putar kamera mengelilingi target (dipanggil oleh listener mouse/keyboard). */
    public void orbit(float deltaAzimuthDeg, float deltaElevationDeg) {
        updateOrbitalPosition(radius, azimuthDeg + deltaAzimuthDeg, elevationDeg + deltaElevationDeg);
    }

    /** Dolly in/out: mengubah jarak kamera ke target. */
    public void zoom(float deltaRadius) {
        updateOrbitalPosition(radius + deltaRadius, azimuthDeg, elevationDeg);
    }

    public void setRadius(float radius) {
        updateOrbitalPosition(radius, azimuthDeg, elevationDeg);
    }

    // ------------------------------------------------------------------
    // Verifikasi: bandingkan hasil manual dengan JOML lookAt() -> selisih maksimum
    // ------------------------------------------------------------------
    public float verifyAgainstJoml() {
        float[] a = calculateLookAtMatrix().get(new float[16]);
        float[] b = new Matrix4f().lookAt(eye, target, up).get(new float[16]);
        float maxErr = 0f;
        for (int i = 0; i < 16; i++) maxErr = Math.max(maxErr, Math.abs(a[i] - b[i]));
        return maxErr;
    }

    // Getter (salinan -> enkapsulasi terjaga)
    public Vector3f getEye()    { return new Vector3f(eye); }
    public Vector3f getTarget() { return new Vector3f(target); }
    public Vector3f getUp()     { return new Vector3f(up); }
    public float getRadius()       { return radius; }
    public float getAzimuthDeg()   { return azimuthDeg; }
    public float getElevationDeg() { return elevationDeg; }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
