package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

/**
 * [SPESIFIKASI TUGAS no. 2 - MODEL 3D]
 * Kelas Mesh3D menyimpan geometri (vertex, face quad berwarna, edge) dan menggambarnya.
 * Factory method createColorCube() membuat Color Cube; createGrid() membuat lantai referensi
 * agar efek perspektif / titik hilang mudah diamati.
 */
public class Mesh3D {

    private final float[] vertices;    // x,y,z berurutan
    private final int[] quads;         // 4 indeks vertex per face
    private final float[] faceColors;  // r,g,b per face
    private final int[] edges;         // pasangan indeks vertex per garis
    private final float[] edgeColor;
    private final float lineWidth;

    public Mesh3D(float[] vertices, int[] quads, float[] faceColors, int[] edges, float[] edgeColor, float lineWidth) {
        this.vertices = vertices.clone();
        this.quads = quads.clone();
        this.faceColors = faceColors.clone();
        this.edges = edges.clone();
        this.edgeColor = edgeColor.clone();
        this.lineWidth = lineWidth;
    }

    /** Color Cube: tiap sisi berwarna berbeda, berpusat di (0,0,0). */
    public static Mesh3D createColorCube(float size) {
        float s = size / 2.0f;
        float[] v = {
            -s, -s, -s,   // 0
             s, -s, -s,   // 1
             s,  s, -s,   // 2
            -s,  s, -s,   // 3
            -s, -s,  s,   // 4
             s, -s,  s,   // 5
             s,  s,  s,   // 6
            -s,  s,  s    // 7
        };
        int[] q = {
            4, 5, 6, 7,   // depan  (+z)
            1, 0, 3, 2,   // belakang (-z)
            5, 1, 2, 6,   // kanan  (+x)
            0, 4, 7, 3,   // kiri   (-x)
            7, 6, 2, 3,   // atas   (+y)
            0, 1, 5, 4    // bawah  (-y)
        };
        float[] c = {
            1.0f, 0.15f, 0.15f,   // depan  : merah
            0.1f, 0.9f, 0.9f,     // belakang: cyan
            0.2f, 0.9f, 0.2f,     // kanan  : hijau
            0.9f, 0.2f, 0.9f,     // kiri   : magenta
            0.25f, 0.4f, 1.0f,    // atas   : biru
            1.0f, 0.9f, 0.1f      // bawah  : kuning
        };
        int[] e = {
            0, 1, 1, 2, 2, 3, 3, 0,   // sisi belakang
            4, 5, 5, 6, 6, 7, 7, 4,   // sisi depan
            0, 4, 1, 5, 2, 6, 3, 7    // penghubung
        };
        return new Mesh3D(v, q, c, e, new float[] {0f, 0f, 0f}, 2.0f);
    }

    /** Grid lantai pada ketinggian y, membentang -halfSize..+halfSize. */
    public static Mesh3D createGrid(float halfSize, int divisions, float y) {
        int lines = divisions + 1;
        float[] v = new float[lines * 4 * 3];
        int[] e = new int[lines * 4];
        float step = (2.0f * halfSize) / divisions;
        int vi = 0;
        int ei = 0;
        for (int i = 0; i < lines; i++) {
            float p = -halfSize + i * step;
            // garis sejajar sumbu-z (x = p)
            v[vi++] = p; v[vi++] = y; v[vi++] = -halfSize;
            v[vi++] = p; v[vi++] = y; v[vi++] = halfSize;
            e[ei++] = (i * 4);     e[ei++] = (i * 4) + 1;
            // garis sejajar sumbu-x (z = p)
            v[vi++] = -halfSize; v[vi++] = y; v[vi++] = p;
            v[vi++] = halfSize;  v[vi++] = y; v[vi++] = p;
            e[ei++] = (i * 4) + 2; e[ei++] = (i * 4) + 3;
        }
        return new Mesh3D(v, new int[0], new float[0], e, new float[] {0.5f, 0.5f, 0.5f}, 1.0f);
    }

    /** Menggambar mesh (OpenGL fixed-function). */
    public void draw() {
        int faceCount = quads.length / 4;
        if (faceCount > 0) {
            glEnable(GL_POLYGON_OFFSET_FILL);   // dorong face sedikit ke belakang agar edge tidak z-fighting
            glPolygonOffset(1.0f, 1.0f);
            glBegin(GL_QUADS);
            for (int f = 0; f < faceCount; f++) {
                glColor3f(faceColors[f * 3], faceColors[f * 3 + 1], faceColors[f * 3 + 2]);
                for (int k = 0; k < 4; k++) {
                    int idx = quads[f * 4 + k] * 3;
                    glVertex3f(vertices[idx], vertices[idx + 1], vertices[idx + 2]);
                }
            }
            glEnd();
            glDisable(GL_POLYGON_OFFSET_FILL);
        }

        if (edges.length > 0) {
            glLineWidth(lineWidth);
            glColor3f(edgeColor[0], edgeColor[1], edgeColor[2]);
            glBegin(GL_LINES);
            for (int idx : edges) {
                glVertex3f(vertices[idx * 3], vertices[idx * 3 + 1], vertices[idx * 3 + 2]);
            }
            glEnd();
        }
    }
}
