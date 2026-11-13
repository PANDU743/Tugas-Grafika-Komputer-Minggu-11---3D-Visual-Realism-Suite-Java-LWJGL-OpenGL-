package com.grafkom.viewing3d;

/**
 * Pabrik mesh 3D (Spesifikasi Tugas butir a): Sphere, Rumah 3D, dan Torus.
 * Semua mesh punya posisi, koordinat UV, dan winding CCW dari luar sehingga
 * normal (flat/smooth) dan Back-Face Culling bekerja benar.
 */
public final class MeshFactory {
    private MeshFactory() {}

    /** Bola: UV = (sector/sectors, 1 - stack/stacks); kutub atas v=1 (tekstur dimuat dengan flip vertikal). */
    public static MeshData sphere(float radius, int stacks, int sectors) {
        MeshData m = new MeshData();
        int[][] idx = new int[stacks + 1][sectors + 1];
        for (int i = 0; i <= stacks; i++) {
            double phi = Math.PI * i / stacks;
            for (int j = 0; j <= sectors; j++) {
                double theta = 2.0 * Math.PI * j / sectors;
                float x = (float) (Math.sin(phi) * Math.cos(theta));
                float y = (float) Math.cos(phi);
                float z = (float) (Math.sin(phi) * Math.sin(theta));
                idx[i][j] = m.addVertex(radius * x, radius * y, radius * z,
                        (float) j / sectors, 1f - (float) i / stacks);
            }
        }
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < sectors; j++) {
                int p00 = idx[i][j], p10 = idx[i + 1][j], p01 = idx[i][j + 1], p11 = idx[i + 1][j + 1];
                m.addTriangle(p00, p01, p10);
                m.addTriangle(p10, p01, p11);
            }
        }
        return m;
    }

    /** Torus (donat): sumbu utama Y, R = jari-jari cincin, r = jari-jari tabung. */
    public static MeshData torus(float R, float r, int rings, int sides) {
        MeshData m = new MeshData();
        int[][] idx = new int[rings + 1][sides + 1];
        for (int i = 0; i <= rings; i++) {
            double u = 2.0 * Math.PI * i / rings;
            for (int j = 0; j <= sides; j++) {
                double v = 2.0 * Math.PI * j / sides;
                float x = (float) ((R + r * Math.cos(v)) * Math.cos(u));
                float y = (float) (r * Math.sin(v));
                float z = (float) ((R + r * Math.cos(v)) * Math.sin(u));
                idx[i][j] = m.addVertex(x, y, z, 4f * i / rings, (float) j / sides);
            }
        }
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < sides; j++) {
                int a = idx[i][j], b = idx[i + 1][j], c = idx[i][j + 1], d = idx[i + 1][j + 1];
                m.addTriangle(a, c, b);
                m.addTriangle(b, c, d);
            }
        }
        return m;
    }

    /** Rumah 3D: badan kotak (alas y=-1, atap mulai y=0.2) + atap pelana (punggung atap y=1.2 sepanjang sumbu Z). */
    public static MeshData house() {
        MeshData m = new MeshData();
        float y0 = -1f, y1 = 0.2f, yr = 1.2f;

        // Dinding depan (+Z)
        quad(m, -1, y0, 1, 1, y0, 1, 1, y1, 1, -1, y1, 1);
        // Dinding belakang (-Z)
        quad(m, 1, y0, -1, -1, y0, -1, -1, y1, -1, 1, y1, -1);
        // Dinding kanan (+X)
        quad(m, 1, y0, 1, 1, y0, -1, 1, y1, -1, 1, y1, 1);
        // Dinding kiri (-X)
        quad(m, -1, y0, -1, -1, y0, 1, -1, y1, 1, -1, y1, -1);
        // Alas (-Y)
        quad(m, -1, y0, -1, 1, y0, -1, 1, y0, 1, -1, y0, 1);
        // Atap: lereng kanan & kiri
        quad(m, 1, y1, 1, 1, y1, -1, 0, yr, -1, 0, yr, 1);
        quad(m, -1, y1, -1, -1, y1, 1, 0, yr, 1, 0, yr, -1);
        // Segitiga pelana depan (+Z) dan belakang (-Z)
        int a = m.addVertex(-1, y1, 1, 0f, 0f), b = m.addVertex(1, y1, 1, 1f, 0f), c = m.addVertex(0, yr, 1, 0.5f, 1f);
        m.addTriangle(a, b, c);
        a = m.addVertex(1, y1, -1, 0f, 0f); b = m.addVertex(-1, y1, -1, 1f, 0f); c = m.addVertex(0, yr, -1, 0.5f, 1f);
        m.addTriangle(a, b, c);
        return m;
    }

    /** Bidang lantai (ground plane) berukuran 2*half, normal +Y, UV diulang 'tiles' kali. */
    public static MeshData ground(float half, float y, float tiles) {
        MeshData m = new MeshData();
        int a = m.addVertex(-half, y, -half, 0f, 0f);
        int b = m.addVertex(-half, y, half, 0f, tiles);
        int c = m.addVertex(half, y, half, tiles, tiles);
        int d = m.addVertex(half, y, -half, tiles, 0f);
        m.addTriangle(a, b, c);
        m.addTriangle(a, c, d);
        return m;
    }

    private static void quad(MeshData m, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        int a = m.addVertex(ax, ay, az, 0f, 0f);
        int b = m.addVertex(bx, by, bz, 1f, 0f);
        int c = m.addVertex(cx, cy, cz, 1f, 1f);
        int d = m.addVertex(dx, dy, dz, 0f, 1f);
        m.addQuad(a, b, c, d);
    }
}
