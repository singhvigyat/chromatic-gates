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

    /** Horizontal center of the gap opening (pixels). */
    public float getGapCenterX() {
        return gapLeft + gapWidth * 0.5f;
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
     * First row of a run: gap position is uniform (anywhere legal).
     *
     * @param worldWidthPx right edge of the 2D world in pixels
     * @param yBottom      bottom edge of the gate band in pixels
     */
    public static Gate create(Random rng, float worldWidthPx, float yBottom) {
        Channel need = Channel.random(rng);
        float gapW = randomGapWidth(rng);
        float gapLeft = randomGapLeftUniform(rng, worldWidthPx, gapW);
        return new Gate(yBottom, gapLeft, gapW, need);
    }

    /**
     * Later rows: gap center stays within {@code maxReachPx} of {@code previousGapCenterX}, biased
     * toward staying near that center; required color sometimes repeats the previous row so movement
     * and color changes are not both demanding every beat.
     */
    public static Gate create(
            Random rng,
            float worldWidthPx,
            float yBottom,
            float previousGapCenterX,
            float maxReachPx,
            Channel previousRowRequiredOrNull
    ) {
        Channel need;
        if (previousRowRequiredOrNull != null && rng.nextFloat() < GameConfig.GATE_SAME_COLOR_REPEAT_CHANCE) {
            need = previousRowRequiredOrNull;
        } else {
            need = Channel.random(rng);
        }
        float gapW = randomGapWidth(rng);
        float margin = 48f;
        float legalLMin = margin;
        float legalLMax = worldWidthPx - gapW - margin;
        float centerMin = legalLMin + gapW * 0.5f;
        float centerMax = legalLMax + gapW * 0.5f;

        float wantCMin = previousGapCenterX - maxReachPx;
        float wantCMax = previousGapCenterX + maxReachPx;
        float cLo = Math.max(centerMin, wantCMin);
        float cHi = Math.min(centerMax, wantCMax);

        float sticky = maxReachPx * GameConfig.GATE_NEXT_GAP_STICKY_FRACTION;
        float iLo = Math.max(cLo, previousGapCenterX - sticky);
        float iHi = Math.min(cHi, previousGapCenterX + sticky);

        float center;
        if (iLo <= iHi && (iHi - iLo) >= 6f) {
            center = iLo + rng.nextFloat() * (iHi - iLo);
        } else if (cLo <= cHi) {
            center = cLo + rng.nextFloat() * (cHi - cLo);
        } else {
            center = clamp(previousGapCenterX, centerMin, centerMax);
        }
        float gapLeft = center - gapW * 0.5f;
        return new Gate(yBottom, gapLeft, gapW, need);
    }

    private static float randomGapWidth(Random rng) {
        return GameConfig.GATE_GAP_MIN_WIDTH_PX
                + rng.nextFloat() * (GameConfig.GATE_GAP_MAX_WIDTH_PX - GameConfig.GATE_GAP_MIN_WIDTH_PX);
    }

    private static float randomGapLeftUniform(Random rng, float worldWidthPx, float gapW) {
        float margin = 48f;
        float maxLeft = worldWidthPx - gapW - margin;
        return margin + rng.nextFloat() * Math.max(1f, (maxLeft - margin));
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
