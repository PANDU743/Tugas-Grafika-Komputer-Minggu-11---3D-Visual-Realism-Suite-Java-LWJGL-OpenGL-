package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

import org.joml.Matrix4f;

/** Pembantu: memuat matriks JOML hasil perhitungan manual ke stack matriks OpenGL. */
public final class GLMatrix {

    private static final float[] BUFFER = new float[16];

    private GLMatrix() { }

    public static void loadProjection(Matrix4f m) {
        glMatrixMode(GL_PROJECTION);
        glLoadMatrixf(m.get(BUFFER));
    }

    public static void loadModelView(Matrix4f m) {
        glMatrixMode(GL_MODELVIEW);
        glLoadMatrixf(m.get(BUFFER));
    }
}
