package com.cglab.chromaticgates;

/**
 * The player's ship: position in pixel space + currently selected color channel.
 * <p>
 * Color is not RGB mixing here — it is one of three discrete channels that must match a gate's
 * requirement. That keeps shaders simple while still demonstrating clear color state in OpenGL.
 */
public final class Player {

    /** Center X in pixels (bottom-left origin world space). */
    private float x;

    /** Center Y in pixels. */
    private final float y;

    /** 0 = red, 1 = green, 2 = blue (indexes into {@link Gate.Channel} ordering). */
    private int colorChannel;

    public Player(float startX, float y) {
        this.x = startX;
        this.y = y;
        // Start on red so the first key press is not mandatory to have a valid state.
        this.colorChannel = 0;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getColorChannel() {
        return colorChannel;
    }

    /**
     * Clamps horizontal position so the whole ship stays inside the playfield.
     *
     * @param worldWidthPx right edge of the orthographic world
     */
    public void applyHorizontalDelta(float deltaXPx, float worldWidthPx) {
        x += deltaXPx;
        float halfW = GameConfig.PLAYER_WIDTH_PX * 0.5f;
        if (x < halfW) {
            x = halfW;
        }
        if (x > worldWidthPx - halfW) {
            x = worldWidthPx - halfW;
        }
    }

    /**
     * Sets the active color channel from user input.
     *
     * @param channelIndex 0..2
     */
    public void setColorChannel(int channelIndex) {
        if (channelIndex < 0 || channelIndex > 2) {
            return;
        }
        this.colorChannel = channelIndex;
    }

    /** Left edge of the player's axis-aligned bounding box. */
    public float minX() {
        return x - GameConfig.PLAYER_WIDTH_PX * 0.5f;
    }

    /** Right edge of the player's axis-aligned bounding box. */
    public float maxX() {
        return x + GameConfig.PLAYER_WIDTH_PX * 0.5f;
    }

    /** Bottom edge of the player's axis-aligned bounding box. */
    public float minY() {
        return y - GameConfig.PLAYER_HEIGHT_PX * 0.5f;
    }

    /** Top edge of the player's axis-aligned bounding box. */
    public float maxY() {
        return y + GameConfig.PLAYER_HEIGHT_PX * 0.5f;
    }
}
