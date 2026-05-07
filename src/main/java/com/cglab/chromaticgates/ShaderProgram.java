package com.cglab.chromaticgates;

import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;

/**
 * Minimal OpenGL shader program wrapper: compiles GLSL sources, links, and exposes uniform locations.
 * <p>
 * This is the smallest "real" graphics pipeline building block: vertex stage transforms geometry,
 * fragment stage outputs per-pixel color. Your lab report can reference these two stages directly.
 */
public final class ShaderProgram {

    /** OpenGL name (integer id) for the linked program object on the GPU. */
    private final int programId;

    /** Cached uniform location for MVP matrix (world → clip space). */
    private final int locMvp;

    /** Cached uniform location for RGBA tint. */
    private final int locColor;

    public ShaderProgram(String vertexGlsl, String fragmentGlsl) {
        int vs = compile(GL_VERTEX_SHADER, vertexGlsl);
        int fs = compile(GL_FRAGMENT_SHADER, fragmentGlsl);
        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);
        checkLinkOk(programId);
        glDeleteShader(vs);
        glDeleteShader(fs);

        locMvp = glGetUniformLocation(programId, "uMvp");
        locColor = glGetUniformLocation(programId, "uColor");
        if (locMvp < 0 || locColor < 0) {
            throw new IllegalStateException("Missing expected uniforms in shader (uMvp, uColor).");
        }
    }

    /** Makes this program active for subsequent draw calls. */
    public void use() {
        glUseProgram(programId);
    }

    /** Uploads a 4×4 column-major matrix (16 floats) to {@code uMvp}. */
    public void setUniformMvp(FloatBuffer columnMajor16) {
        glUniformMatrix4fv(locMvp, false, columnMajor16);
    }

    /** Uploads an RGBA color to {@code uColor}. */
    public void setUniformColor(float r, float g, float b, float a) {
        glUniform4f(locColor, r, g, b, a);
    }

    public void dispose() {
        glDeleteProgram(programId);
    }

    private static int compile(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        int[] ok = new int[1];
        glGetShaderiv(shader, GL_COMPILE_STATUS, ok);
        if (ok[0] == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("Shader compile failed: " + log);
        }
        return shader;
    }

    private static void checkLinkOk(int program) {
        int[] ok = new int[1];
        glGetProgramiv(program, GL_LINK_STATUS, ok);
        if (ok[0] == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            throw new IllegalStateException("Program link failed: " + log);
        }
    }
}
