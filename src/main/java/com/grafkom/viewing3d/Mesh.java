package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL30.*;

/**
 * Mesh di GPU. Menyimpan DUA VAO dari MeshData yang sama:
 * versi Flat (normal per-face) dan versi Smooth (normal per-vertex hasil averaging).
 * Layout: location 0 = posisi, 1 = normal, 2 = UV.
 */
public class Mesh {
    private final int vaoFlat, vaoSmooth;
    private final int countFlat, countSmooth;

    public Mesh(MeshData data) {
        float[] flat = data.buildFlat();
        float[] smooth = data.buildSmooth();
        vaoFlat = upload(flat);
        vaoSmooth = upload(smooth);
        countFlat = flat.length / MeshData.FLOATS_PER_VERTEX;
        countSmooth = smooth.length / MeshData.FLOATS_PER_VERTEX;
    }

    private static int upload(float[] buf) {
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);
        int stride = MeshData.FLOATS_PER_VERTEX * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glBindVertexArray(0);
        return vao;
    }

    public void draw(boolean smooth) {
        glBindVertexArray(smooth ? vaoSmooth : vaoFlat);
        glDrawArrays(GL_TRIANGLES, 0, smooth ? countSmooth : countFlat);
        glBindVertexArray(0);
    }
}
