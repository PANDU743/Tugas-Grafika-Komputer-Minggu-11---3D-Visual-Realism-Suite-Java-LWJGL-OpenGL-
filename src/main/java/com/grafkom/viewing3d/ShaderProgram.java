package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL30.*;

/** Pembungkus program GLSL (compile, link, set uniform). Sumber shader dibaca dari classpath. */
public class ShaderProgram {
    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    public ShaderProgram(String vertexPath, String fragmentPath) {
        int vs = compile(GL_VERTEX_SHADER, readResource(vertexPath), vertexPath);
        int fs = compile(GL_FRAGMENT_SHADER, readResource(fragmentPath), fragmentPath);
        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Link error: " + glGetProgramInfoLog(programId));
        }
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    public void use() { glUseProgram(programId); }

    public void setInt(String name, int v) { glUniform1i(loc(name), v); }

    public void setBool(String name, boolean v) { glUniform1i(loc(name), v ? 1 : 0); }

    public void setFloat(String name, float v) { glUniform1f(loc(name), v); }

    public void setVec3(String name, Vector3f v) { glUniform3f(loc(name), v.x, v.y, v.z); }

    public void setVec3(String name, float x, float y, float z) { glUniform3f(loc(name), x, y, z); }

    public void setMat4(String name, Matrix4f m) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(loc(name), false, m.get(stack.mallocFloat(16)));
        }
    }

    private int loc(String name) {
        return uniformCache.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }

    private static int compile(int type, String src, String label) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader compile error in " + label + ":\n" + glGetShaderInfoLog(id));
        }
        return id;
    }

    private static String readResource(String path) {
        try (InputStream in = ShaderProgram.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("Resource not found: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
