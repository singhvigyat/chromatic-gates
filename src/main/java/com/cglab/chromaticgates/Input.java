package com.cglab.chromaticgates;

import org.lwjgl.glfw.GLFW;

/**
 * Thin wrapper around GLFW keyboard polling.
 * <p>
 * We poll keys each frame instead of using key callbacks for movement so that holding
 * {@code A}/{@code D} produces smooth continuous motion — callbacks are better for one-shot actions.
 */
public final class Input {

    /** GLFW window handle used to query keyboard focus state. */
    private final long windowHandle;

    /** Movement intent for this frame (set by polling). */
    private float moveAxis;

    /** True while the restart key is held. */
    private boolean restartHeld;

    /** Previous frame's restart key state so we can detect a rising edge (press, not hold). */
    private boolean restartPrev;

    public Input(long windowHandle) {
        this.windowHandle = windowHandle;
    }

    /**
     * Call once per frame at the start of the frame, before {@link Game#update(float)}.
     */
    public void updateFrameStart() {
        moveAxis = 0f;
        if (isPressed(GLFW.GLFW_KEY_A) || isPressed(GLFW.GLFW_KEY_LEFT)) {
            moveAxis -= 1f;
        }
        if (isPressed(GLFW.GLFW_KEY_D) || isPressed(GLFW.GLFW_KEY_RIGHT)) {
            moveAxis += 1f;
        }
        restartPrev = restartHeld;
        restartHeld = isPressed(GLFW.GLFW_KEY_R);
    }

    /**
     * Horizontal movement axis in range roughly [-1, 1]. Both keys pressed cancel out.
     */
    public float getMoveAxis() {
        return moveAxis;
    }

    /** Player requests an immediate restart (new run). */
    public boolean isRestartHeld() {
        return restartHeld;
    }

    /**
     * True only on the first frame after R transitions from released → pressed.
     * Prevents spawning hundreds of resets while the key is held down.
     */
    public boolean isRestartJustPressed() {
        return restartHeld && !restartPrev;
    }

    /**
     * Returns true if the given GLFW key token is currently down.
     *
     * @param glfwKeyCode e.g. {@link GLFW#GLFW_KEY_SPACE}
     */
    public boolean isPressed(int glfwKeyCode) {
        return GLFW.glfwGetKey(windowHandle, glfwKeyCode) == GLFW.GLFW_PRESS;
    }

    /**
     * Color selection keys: 1 = red channel, 2 = green, 3 = blue.
     * These are read directly in {@link Game} so we can detect "just pressed" with edge logic if we extend later.
     */
    public boolean isColorKeyJustPressed(int channelIndex) {
        int key = switch (channelIndex) {
            case 0 -> GLFW.GLFW_KEY_1;
            case 1 -> GLFW.GLFW_KEY_2;
            case 2 -> GLFW.GLFW_KEY_3;
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };
        if (key == GLFW.GLFW_KEY_UNKNOWN) {
            return false;
        }
        // GLFW_PRESS fires every frame while held; for color switching, holding is OK (idempotent).
        return isPressed(key);
    }
}
