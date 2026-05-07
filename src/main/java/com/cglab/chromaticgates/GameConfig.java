package com.cglab.chromaticgates;

/**
 * Central place for tunable numbers and strings.
 * <p>
 * Isolating constants here avoids "magic numbers" scattered through gameplay and rendering code,
 * which makes last-minute tweaks before a lab presentation much safer.
 */
public final class GameConfig {

    private GameConfig() {
        // Utility class: no instances.
    }

    /** Window width in screen pixels (framebuffer space). */
    public static final int WINDOW_WIDTH_PX = 960;

    /** Window height in screen pixels. */
    public static final int WINDOW_HEIGHT_PX = 540;

    /** Title shown in the OS window chrome. */
    public static final String WINDOW_TITLE = "Chromatic Gates — CG Lab (OpenGL)";

    /** Player horizontal speed in pixels per second when holding a movement key. */
    public static final float PLAYER_MOVE_SPEED_PX_PER_SEC = 420f;

    /** Vertical size of the player quad. */
    public static final float PLAYER_HEIGHT_PX = 28f;

    /** Horizontal size of the player quad. */
    public static final float PLAYER_WIDTH_PX = 44f;

    /** Player stays near this Y (bottom area of the screen). */
    public static final float PLAYER_BASELINE_Y_PX = 72f;

    /** Initial downward speed of the first gate (difficulty ramps slightly over time). */
    public static final float GATE_START_SPEED_PX_PER_SEC = 110f;

    /** How much gate speed increases after each successful pass. */
    public static final float GATE_SPEED_STEP_PX_PER_SEC = 4f;

    /** Maximum gate fall speed so the game stays fair on long runs. */
    public static final float GATE_MAX_SPEED_PX_PER_SEC = 220f;

    /** Vertical thickness of the solid "wall" segments on each gate row. */
    public static final float GATE_BAND_HEIGHT_PX = 36f;

    /** Minimum width of the colored gap the player must align with. */
    public static final float GATE_GAP_MIN_WIDTH_PX = 130f;

    /** Maximum width of the gap (early gates are more forgiving). */
    public static final float GATE_GAP_MAX_WIDTH_PX = 200f;

    /** Vertical distance between spawned gate rows (larger = more time to move + change color). */
    public static final float GATE_SPAWN_SPACING_PX = 265f;

    /**
     * Upper bound on horizontal travel budget (fraction of theoretical reach) so gap centers stay
     * realistically reachable once ship width and alignment are accounted for in code.
     */
    public static final float GATE_GAP_REACH_SAFETY = 0.68f;

    /**
     * When picking the next gap center, prefer staying within this fraction of {@code maxReachPx}
     * of the previous center so rows rarely land on opposite screen edges in one beat.
     */
    public static final float GATE_NEXT_GAP_STICKY_FRACTION = 0.48f;

    /** Chance the next row reuses the previous row's required color (less frantic 1/2/3 tapping). */
    public static final float GATE_SAME_COLOR_REPEAT_CHANCE = 0.4f;

    /** Pixels reserved inside the reach budget (ship width + a little slack to sit inside the hole). */
    public static final float GATE_GAP_REACH_LAYOUT_FUDGE_PX = PLAYER_WIDTH_PX + 56f;

    /** Seconds of invulnerability after a mistake (prevents double penalties). */
    public static final float HURT_INVULN_SECONDS = 1.1f;

    /** Maximum mistakes before game over. */
    public static final int MAX_LIVES = 3;
}
