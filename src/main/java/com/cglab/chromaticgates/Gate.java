package com.cglab.chromaticgates;

import java.util.Random;

/**
 * One horizontal "wall with a gap" that falls down the screen.
 * <p>
 * The unique mechanic: the gap glows a required color. The player must press 1/2/3 to match
 * that color before sliding through the gap. Wrong color through the gap, or touching the solid
 * wall segments, costs a life (during normal play).
 */
public final class Gate {

    /**
     * The three chromatic channels used by gameplay (order matches number keys 1..3).
     */
    public enum Channel {
        RED(0.95f, 0.20f, 0.22f),
        GREEN(0.25f, 0.92f, 0.35f),
        BLUE(0.28f, 0.45f, 0.98f);

        /** RGB triplet for OpenGL fragment color (linear-ish; good enough for the lab). */
        public final float r;
        public final float g;
        public final float b;

        Channel(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        /** Picks a random channel (uniform). */
        public static Channel random(Random rng) {
            Channel[] values = values();
            return values[rng.nextInt(values.length)];
        }
    }

    /** Bottom Y of the gate band in pixel space (moves downward over time). */
    private float yBottom;

    /** Left edge of the gap opening (player must align here horizontally). */
    private float gapLeft;

    /** Width of the gap opening. */
    private float gapWidth;

    /** Required color to pass safely through the gap. */
    private final Channel required;

    /** Once true, this gate no longer contributes collisions or scoring. */
    private boolean disposed;

    /** True after we already applied a "wrong color" penalty for this gate. */
    private boolean wrongColorPenaltyApplied;

    /** True after a successful pass added score. */
    private boolean scoreAwarded;

    public Gate(float yBottom, float gapLeft, float gapWidth, Channel required) {
        this.yBottom = yBottom;
        this.gapLeft = gapLeft;
        this.gapWidth = gapWidth;
        this.required = required;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public void markDisposed() {
        this.disposed = true;
    }

    public float getYBottom() {
        return yBottom;
    }

    public Channel getRequired() {
        return required;
    }

    /** Moves the gate downward. */
    public void moveDown(float deltaYPx) {
        yBottom -= deltaYPx;
    }

    /** Top Y of the solid/gap band. */
    public float getYTop() {
        return yBottom + GameConfig.GATE_BAND_HEIGHT_PX;
    }

    public float getGapLeft() {
        return gapLeft;
    }

    public float getGapRight() {
        return gapLeft + gapWidth;
    }

    public boolean isWrongColorPenaltyApplied() {
        return wrongColorPenaltyApplied;
    }

    public void setWrongColorPenaltyApplied(boolean wrongColorPenaltyApplied) {
        this.wrongColorPenaltyApplied = wrongColorPenaltyApplied;
    }

    public boolean isScoreAwarded() {
        return scoreAwarded;
    }

    public void setScoreAwarded(boolean scoreAwarded) {
        this.scoreAwarded = scoreAwarded;
    }

    /**
     * Factory that randomizes gap placement and required color for variety each spawn.
     *
     * @param worldWidthPx right edge of the 2D world in pixels
     * @param yBottom      bottom edge of the gate band in pixels
     */
    public static Gate create(Random rng, float worldWidthPx, float yBottom) {
        Channel need = Channel.random(rng);
        float gapW = GameConfig.GATE_GAP_MIN_WIDTH_PX
                + rng.nextFloat() * (GameConfig.GATE_GAP_MAX_WIDTH_PX - GameConfig.GATE_GAP_MIN_WIDTH_PX);
        float margin = 48f;
        float maxLeft = worldWidthPx - gapW - margin;
        float gapLeft = margin + rng.nextFloat() * Math.max(1f, (maxLeft - margin));
        return new Gate(yBottom, gapLeft, gapW, need);
    }
}
