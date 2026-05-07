package com.cglab.chromaticgates;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

/**
 * 2D immediate-style renderer built on modern OpenGL (VAO + VBO + shaders).
 * <p>
 * Public API is intentionally tiny ({@link #drawRect}, {@link #drawHudBackground}) so the game code
 * stays readable. Behind the scenes we stream a per-frame orthographic projection and per-draw MVP.
 */
public final class Renderer2D {

    /**
     * Vertex shader: transforms 2D local coordinates into clip space using one combined MVP matrix.
     * {@code aPos} is in "unit quad" space from (0,0) to (1,1).
     */
    private static final String VS = """
            #version 330 core
            layout (location = 0) in vec2 aPos;
            uniform mat4 uMvp;
            void main() {
                gl_Position = uMvp * vec4(aPos, 0.0, 1.0);
            }
            """;

    /**
     * Fragment shader: solid color output (alpha supported for subtle UI overlays).
     */
    private static final String FS = """
            #version 330 core
            uniform vec4 uColor;
            out vec4 fragColor;
            void main() {
                fragColor = uColor;
            }
            """;

    /** GPU buffer holding two triangles (a square) in local coordinates. */
    private final int vaoId;
    private final int vboId;

    private final ShaderProgram shader;

    /** Reused buffer for 16 floats (4×4 matrix) to reduce allocations during the game loop. */
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    /** Current viewport width in pixels (may change on window resize). */
    private float viewportW;

    /** Current viewport height in pixels. */
    private float viewportH;

    public Renderer2D(int initialWidthPx, int initialHeightPx) {
        this.viewportW = initialWidthPx;
        this.viewportH = initialHeightPx;

        shader = new ShaderProgram(VS, FS);

        // Interleaved vertices are not needed for a static quad; we store just two components (x,y).
        float[] quadLocal = {
                0f, 0f,
                1f, 0f,
                1f, 1f,
                0f, 0f,
                1f, 1f,
                0f, 1f,
        };

        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();
        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, quadLocal, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    /**
     * Called from GLFW resize callback so our orthographic projection matches the framebuffer.
     */
    public void setViewportPixels(int width, int height) {
        this.viewportW = width;
        this.viewportH = height;
    }

    /**
     * Draws an axis-aligned rectangle in pixel space with bottom-left origin (y grows upward).
     *
     * @param xMin world x of left edge
     * @param yMin world y of bottom edge
     * @param xMax world x of right edge
     * @param yMax world y of top edge
     */
    public void drawRect(float xMin, float yMin, float xMax, float yMax, float r, float g, float b, float a) {
        float w = xMax - xMin;
        float h = yMax - yMin;
        float[] ortho = orthographicOffCenter(0f, viewportW, 0f, viewportH);
        float[] model = multiplyMat4(translateMat4(xMin, yMin, 0f), scaleMat4(w, h, 1f));
        float[] mvp = multiplyMat4(ortho, model);

        shader.use();
        shader.setUniformColor(r, g, b, a);
        putColumnMajor(mvp, matrixBuffer);
        shader.setUniformMvp(matrixBuffer);

        glBindVertexArray(vaoId);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }

    /**
     * Semi-transparent top bar for on-screen instructions (does not affect gameplay hitboxes).
     */
    public void drawHudBackground() {
        float h = 86f;
        drawRect(0f, viewportH - h, viewportW, viewportH, 0f, 0f, 0f, 0.35f);
    }

    public void dispose() {
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
        shader.dispose();
    }

    // ---------------------------------------------------------------------------------------------
    // Small linear algebra helpers (no external math library on purpose — easy to explain on a poster)
    // ---------------------------------------------------------------------------------------------

    private static void putColumnMajor(float[] m16, FloatBuffer dst) {
        dst.clear();
        dst.put(m16);
        dst.flip();
    }

    /** Right-handed orthographic matrix mapping [left,right]×[bottom,top] into NDC. */
    private static float[] orthographicOffCenter(float left, float right, float bottom, float top) {
        float near = -1f;
        float far = 1f;
        float rl = right - left;
        float tb = top - bottom;
        float fn = far - near;
        return new float[]{
                2f / rl, 0f, 0f, 0f,
                0f, 2f / tb, 0f, 0f,
                0f, 0f, -2f / fn, 0f,
                -(right + left) / rl, -(top + bottom) / tb, -(far + near) / fn, 1f
        };
    }

    private static float[] multiplyMat4(float[] a, float[] b) {
        float[] out = new float[16];
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                out[c * 4 + r] =
                        a[0 * 4 + r] * b[c * 4 + 0] +
                        a[1 * 4 + r] * b[c * 4 + 1] +
                        a[2 * 4 + r] * b[c * 4 + 2] +
                        a[3 * 4 + r] * b[c * 4 + 3];
            }
        }
        return out;
    }

    private static float[] translateMat4(float tx, float ty, float tz) {
        return new float[]{
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                tx, ty, tz, 1f
        };
    }

    private static float[] scaleMat4(float sx, float sy, float sz) {
        return new float[]{
                sx, 0f, 0f, 0f,
                0f, sy, 0f, 0f,
                0f, 0f, sz, 0f,
                0f, 0f, 0f, 1f
        };
    }
}
