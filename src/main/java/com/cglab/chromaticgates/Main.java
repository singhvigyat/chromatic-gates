package com.cglab.chromaticgates;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

/**
 * Application entry point for Chromatic Gates.
 * <p>
 * This class is responsible only for bootstrapping: initializing GLFW, creating an OpenGL
 * context, wiring basic window behavior, and handing control to {@link Game}.
 * Keeping this thin makes the rest of the codebase easier to explain during a lab demo.
 */
public final class Main {

    /** GLFW window handle (opaque pointer stored as a long). */
    private long windowHandle;

    /** Core game object: owns update/render loop and game rules. */
    private Game game;

    /** Renders colored rectangles using a minimal shader pipeline. */
    private Renderer2D renderer;

    /** Polls keyboard state each frame for smooth movement. */
    private Input input;

    /**
     * Standard Java entry point — the JVM calls this method when you run the program.
     *
     * @param args unused command-line arguments (could add debug flags later)
     */
    public static void main(String[] args) {
        new Main().run();
    }

    /**
     * Runs the full application lifecycle: init → loop → cleanup.
     */
    private void run() {
        // Print LWJGL version so you can verify bindings during the showcase.
        System.out.println("LWJGL " + Version.getVersion());

        initGlfw();
        createWindow();
        initGl();
        initGameObjects();

        // Main loop: process events, simulate, draw, repeat until user closes window.
        gameLoop();

        // Free GPU and OS resources in reverse order of creation.
        dispose();
    }

    /**
     * Registers GLFW's error callback and initializes the GLFW library.
     * Must be called before any other GLFW function.
     */
    private void initGlfw() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
    }

    /**
     * Creates a window with an OpenGL context and sensible defaults for a student project.
     */
    private void createWindow() {
        // Request OpenGL 3.3 Core: modern programmable pipeline (vertex + fragment shaders).
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        // macOS compatibility would require forward-compatible true; Windows does not need it.

        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

        windowHandle = GLFW.glfwCreateWindow(
                GameConfig.WINDOW_WIDTH_PX,
                GameConfig.WINDOW_HEIGHT_PX,
                GameConfig.WINDOW_TITLE,
                MemoryUtil.NULL,
                MemoryUtil.NULL
        );

        if (windowHandle == MemoryUtil.NULL) {
            throw new IllegalStateException("Failed to create GLFW window");
        }

        // Center the window on the primary monitor (nice polish for demos).
        GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        if (vidmode != null) {
            GLFW.glfwSetWindowPos(
                    windowHandle,
                    (vidmode.width() - GameConfig.WINDOW_WIDTH_PX) / 2,
                    (vidmode.height() - GameConfig.WINDOW_HEIGHT_PX) / 2
            );
        }

        // Make the OpenGL context current on this thread (required before GL calls).
        GLFW.glfwMakeContextCurrent(windowHandle);
        // V-sync: limits frame rate to monitor refresh, reduces tearing.
        GLFW.glfwSwapInterval(1);

        // When framebuffer size changes (resize, DPI), tell the renderer to adjust the viewport.
        GLFW.glfwSetFramebufferSizeCallback(windowHandle, (win, width, height) -> {
            GL11.glViewport(0, 0, width, height);
            renderer.setViewportPixels(width, height);
        });
    }

    /**
     * Loads OpenGL function pointers for this context and sets baseline GL state.
     */
    private void initGl() {
        GL.createCapabilities();

        // Fragment colors come from our shader output (not fixed-function lighting).
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        // Alpha blending lets us draw soft glow quads over the background.
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glClearColor(0.04f, 0.05f, 0.08f, 1f);
        GL11.glViewport(0, 0, GameConfig.WINDOW_WIDTH_PX, GameConfig.WINDOW_HEIGHT_PX);
    }

    /**
     * Constructs game subsystems after the GL context exists.
     */
    private void initGameObjects() {
        renderer = new Renderer2D(GameConfig.WINDOW_WIDTH_PX, GameConfig.WINDOW_HEIGHT_PX);
        input = new Input(windowHandle);
        game = new Game(renderer, input);
    }

    /**
     * The classic real-time loop: time → input → update → render → swap buffers.
     */
    private void gameLoop() {
        double lastTimeSeconds = GLFW.glfwGetTime();

        while (!GLFW.glfwWindowShouldClose(windowHandle)) {
            double now = GLFW.glfwGetTime();
            float deltaSeconds = (float) (now - lastTimeSeconds);
            lastTimeSeconds = now;

            // Cap delta to avoid huge physics steps after pausing in a debugger.
            if (deltaSeconds > 0.05f) {
                deltaSeconds = 0.05f;
            }

            GLFW.glfwPollEvents();
            input.updateFrameStart();

            game.update(deltaSeconds);
            game.render();

            GLFW.glfwSwapBuffers(windowHandle);
        }
    }

    /**
     * Releases GLFW and LWJGL resources.
     */
    private void dispose() {
        if (renderer != null) {
            renderer.dispose();
        }
        GLFW.glfwDestroyWindow(windowHandle);
        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
    }
}
