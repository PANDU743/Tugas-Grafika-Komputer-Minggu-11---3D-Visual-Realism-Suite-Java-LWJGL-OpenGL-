package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;

/**
 * Pemuat tekstur 2D (Indikator 4) memakai STBImage.
 * Gambar PNG/JPG dibaca dari classpath (src/main/resources), lalu diunggah ke GPU.
 * Jika berkas tidak ditemukan, dipakai tekstur papan catur cadangan.
 */
public class Texture2D {
    private int textureID;

    public Texture2D(String resourcePath) {
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Wrap Parameters (Filtering diatur lewat setFilterMode)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        // v=0 di bawah gambar -> sesuai konvensi UV OpenGL (hindari tekstur terbalik arah V)
        stbi_set_flip_vertically_on_load(true);
        boolean loaded = false;
        ByteBuffer file = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            file = readResource(resourcePath);
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer data = stbi_load_from_memory(file, width, height, channels, 4);
            if (data != null) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0,
                        GL_RGBA, GL_UNSIGNED_BYTE, data);
                glGenerateMipmap(GL_TEXTURE_2D);
                stbi_image_free(data);
                loaded = true;
            } else {
                System.err.println("Failed to load texture: " + resourcePath + " (" + stbi_failure_reason() + ")");
            }
        } catch (IOException e) {
            System.err.println("Failed to load texture: " + resourcePath + " (" + e.getMessage() + ")");
        } finally {
            if (file != null) MemoryUtil.memFree(file);
        }
        if (!loaded) uploadFallbackChecker();

        setFilterMode(true);
    }

    /**
     * true  = Bilinear + Mipmapped (GL_LINEAR_MIPMAP_LINEAR / GL_LINEAR)
     * false = Nearest Neighbor     (GL_NEAREST / GL_NEAREST)
     */
    public void setFilterMode(boolean bilinearMipmapped) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (bilinearMipmapped) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void delete() {
        glDeleteTextures(textureID);
    }

    private static ByteBuffer readResource(String path) throws IOException {
        try (InputStream in = Texture2D.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("resource not found");
            byte[] bytes = in.readAllBytes();
            ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
            buf.put(bytes).flip();
            return buf;
        }
    }

    private void uploadFallbackChecker() {
        int size = 64;
        ByteBuffer px = MemoryUtil.memAlloc(size * size * 4);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean on = ((x / 8) + (y / 8)) % 2 == 0;
                byte c = (byte) (on ? 220 : 60);
                px.put(c).put(c).put(c).put((byte) 255);
            }
        }
        px.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size, size, 0, GL_RGBA, GL_UNSIGNED_BYTE, px);
        glGenerateMipmap(GL_TEXTURE_2D);
        MemoryUtil.memFree(px);
    }
}
