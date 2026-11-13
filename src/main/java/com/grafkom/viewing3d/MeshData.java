package com.grafkom.viewing3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data mesh murni di sisi CPU (tanpa OpenGL): posisi vertex, koordinat UV, dan segitiga.
 * Dari data yang sama dibangun DUA versi buffer:
 *  - buildFlat()   : normal per-FACE        (Flat Shading, Indikator 2)
 *  - buildSmooth() : normal per-VERTEX rata-rata dari semua face yang berbagi posisi
 *                    N_vertex = (sum N_face_i) / ||sum N_face_i||  (Smooth Shading, Indikator 2)
 * Format buffer interleaved per vertex: [px py pz | nx ny nz | u v]  (8 float).
 * Kontrak winding: segitiga CCW dilihat dari luar (cocok dengan glFrontFace(GL_CCW)).
 */
public class MeshData {
    public static final int FLOATS_PER_VERTEX = 8;

    private final List<float[]> verts = new ArrayList<>(); // {x,y,z,u,v}
    private final List<int[]> tris = new ArrayList<>();

    public int addVertex(float x, float y, float z, float u, float v) {
        verts.add(new float[]{x, y, z, u, v});
        return verts.size() - 1;
    }

    /** Menambah segitiga (CCW dari luar). Segitiga degenerate (luas 0) dibuang. */
    public void addTriangle(int a, int b, int c) {
        float[] n = faceNormalRaw(a, b, c);
        if (length(n) < 1e-9f) return;
        tris.add(new int[]{a, b, c});
    }

    /** Quad a,b,c,d berurutan CCW dari luar -> (a,b,c) + (a,c,d). */
    public void addQuad(int a, int b, int c, int d) {
        addTriangle(a, b, c);
        addTriangle(a, c, d);
    }

    public int triangleCount() { return tris.size(); }

    public float[] getVertex(int i) { return verts.get(i); }

    public int[] getTriangle(int t) { return tris.get(t); }

    // ---------------------------------------------------------------- Flat

    public float[] buildFlat() {
        float[] out = new float[tris.size() * 3 * FLOATS_PER_VERTEX];
        int o = 0;
        for (int[] t : tris) {
            float[] n = normalize(faceNormalRaw(t[0], t[1], t[2]));
            for (int k = 0; k < 3; k++) o = put(out, o, verts.get(t[k]), n);
        }
        return out;
    }

    // -------------------------------------------------------------- Smooth

    public float[] buildSmooth() {
        // 1. Jumlahkan normal tiap face ke setiap posisi vertex yang berbagi (weld berdasarkan posisi,
        //    sehingga vertex duplikat akibat seam UV tetap mendapat normal yang sama).
        Map<String, float[]> acc = new HashMap<>();
        for (int[] t : tris) {
            float[] n = normalize(faceNormalRaw(t[0], t[1], t[2]));
            for (int k = 0; k < 3; k++) {
                float[] s = acc.computeIfAbsent(key(verts.get(t[k])), kk -> new float[3]);
                s[0] += n[0]; s[1] += n[1]; s[2] += n[2];
            }
        }
        // 2. Normalisasi hasil penjumlahan dan tulis ke buffer.
        float[] out = new float[tris.size() * 3 * FLOATS_PER_VERTEX];
        int o = 0;
        for (int[] t : tris) {
            for (int k = 0; k < 3; k++) {
                float[] v = verts.get(t[k]);
                o = put(out, o, v, normalize(acc.get(key(v))));
            }
        }
        return out;
    }

    // ------------------------------------------------------------- helpers

    private static String key(float[] v) {
        return Math.round(v[0] * 10000f) + "," + Math.round(v[1] * 10000f) + "," + Math.round(v[2] * 10000f);
    }

    private float[] faceNormalRaw(int a, int b, int c) {
        float[] p0 = verts.get(a), p1 = verts.get(b), p2 = verts.get(c);
        float ux = p1[0] - p0[0], uy = p1[1] - p0[1], uz = p1[2] - p0[2];
        float vx = p2[0] - p0[0], vy = p2[1] - p0[1], vz = p2[2] - p0[2];
        return new float[]{uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx};
    }

    private static float length(float[] n) {
        return (float) Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
    }

    private static float[] normalize(float[] n) {
        float l = length(n);
        if (l < 1e-9f) return new float[]{0f, 1f, 0f};
        return new float[]{n[0] / l, n[1] / l, n[2] / l};
    }

    private static int put(float[] out, int o, float[] v, float[] n) {
        out[o++] = v[0]; out[o++] = v[1]; out[o++] = v[2];
        out[o++] = n[0]; out[o++] = n[1]; out[o++] = n[2];
        out[o++] = v[3]; out[o++] = v[4];
        return o;
    }
}
