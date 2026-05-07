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
     * Color selection: top-row digits, keypad digits, or Q/W/E (same order as 1/2/3).
     * <p>
     * Numpad and Q/W/E are included because laptop layouts and Num Lock can make “1–3 do nothing”
     * when only the main-row codes are polled.
     */
    public boolean isColorSelectPressed(int channelIndex) {
        int digit;
        int keypad;
        int letter;
        switch (channelIndex) {
            case 0 -> {
                digit = GLFW.GLFW_KEY_1;
                keypad = GLFW.GLFW_KEY_KP_1;
                letter = GLFW.GLFW_KEY_Q;
            }
            case 1 -> {
                digit = GLFW.GLFW_KEY_2;
                keypad = GLFW.GLFW_KEY_KP_2;
                letter = GLFW.GLFW_KEY_W;
            }
            case 2 -> {
                digit = GLFW.GLFW_KEY_3;
                keypad = GLFW.GLFW_KEY_KP_3;
                letter = GLFW.GLFW_KEY_E;
            }
            default -> {
                return false;
            }
        }
        return isPressed(digit) || isPressed(keypad) || isPressed(letter);
    }
}
