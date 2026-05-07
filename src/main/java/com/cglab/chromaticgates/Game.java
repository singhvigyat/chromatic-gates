package com.cglab.chromaticgates;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Core gameplay: rules, scoring, spawning, and drawing the world.
 * <p>
 * This is the class you can walk through verbally during the showcase: it connects user input,
 * simple collision logic, and OpenGL drawing without hiding details behind an engine.
 */
public final class Game {

    private final Renderer2D renderer;
    private final Input input;
    private final Random rng = new Random();

    /** The controllable ship. */
    private final Player player;

    /** Active obstacles scrolling downward. */
    private final List<Gate> gates = new ArrayList<>();

    /** Current gate fall speed; ramps gently to keep tension. */
    private float gateSpeedPxPerSec = GameConfig.GATE_START_SPEED_PX_PER_SEC;

    /** Highest gate bottom Y used to decide when to spawn the next row. */
    private float highestGateYBottom = -10_000f;

    /** Gameplay score (successful chromatic matches). */
    private int score;

    /** Remaining mistakes. */
    private int lives = GameConfig.MAX_LIVES;

    /** When positive, collisions are ignored to give the player breathing room. */
    private float hurtCooldownSeconds = 0f;

    /** True when the run ended; press R to reset. */
    private boolean gameOver;

    public Game(Renderer2D renderer, Input input) {
        this.renderer = renderer;
        this.input = input;
        float startX = GameConfig.WINDOW_WIDTH_PX * 0.5f;
        float startY = GameConfig.PLAYER_BASELINE_Y_PX;
        this.player = new Player(startX, startY);

        // Seed a couple of gates so the screen is not empty at t=0.
        float y = GameConfig.WINDOW_HEIGHT_PX + 40f;
        for (int i = 0; i < 4; i++) {
            Gate g = Gate.create(rng, GameConfig.WINDOW_WIDTH_PX, y);
            gates.add(g);
            highestGateYBottom = Math.max(highestGateYBottom, g.getYBottom());
            y += GameConfig.GATE_SPAWN_SPACING_PX;
        }
    }

    /**
     * Advances simulation by {@code deltaSeconds} and reacts to input.
     */
    public void update(float deltaSeconds) {
        if (input.isRestartJustPressed()) {
            resetRun();
        }

        if (hurtCooldownSeconds > 0f) {
            hurtCooldownSeconds -= deltaSeconds;
        }

        if (gameOver) {
            return;
        }

        // --- Color selection (1/2/3, keypad, or Q/W/E) ---
        if (input.isColorSelectPressed(0)) {
            player.setColorChannel(0);
        }
        if (input.isColorSelectPressed(1)) {
            player.setColorChannel(1);
        }
        if (input.isColorSelectPressed(2)) {
            player.setColorChannel(2);
        }

        // --- Horizontal motion ---
        float move = input.getMoveAxis() * GameConfig.PLAYER_MOVE_SPEED_PX_PER_SEC * deltaSeconds;
        player.applyHorizontalDelta(move, GameConfig.WINDOW_WIDTH_PX);

        // --- Move gates downward ---
        for (Gate gate : gates) {
            gate.moveDown(gateSpeedPxPerSec * deltaSeconds);
        }

        // --- Spawn new gates when the stack moves down ---
        maybeSpawnGate();

        // --- Collisions / scoring ---
        if (hurtCooldownSeconds <= 0f) {
            resolveGateInteractions();
        }

        // --- Cleanup gates that left the screen ---
        removeOffscreenGates();
    }

    /**
     * Renders the full frame: clear, world, HUD text via console is not possible — we draw simple quads as UI.
     */
    public void render() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        // Background vignette bands (purely decorative; demonstrates layered alpha quads).
        renderer.drawRect(0f, 0f, GameConfig.WINDOW_WIDTH_PX, GameConfig.WINDOW_HEIGHT_PX, 0.05f, 0.06f, 0.10f, 1f);

        // Draw gates: walls dark, gap glows with required color.
        for (Gate gate : gates) {
            drawGate(gate);
        }

        // Player ship: tinted by selected channel (outline so it separates from the playfield).
        Gate.Channel shipColor = channelFromIndex(player.getColorChannel());
        float pulse = 0.78f + 0.22f * (float) Math.sin(System.nanoTime() * 1e-9 * 6.0);
        float px0 = player.minX();
        float py0 = player.minY();
        float px1 = player.maxX();
        float py1 = player.maxY();
        float outline = 4f;
        renderer.drawRect(px0 - outline, py0 - outline, px1 + outline, py1 + outline, 0.02f, 0.02f, 0.04f, 0.92f);
        renderer.drawRect(
                px0,
                py0,
                px1,
                py1,
                shipColor.r * pulse,
                shipColor.g * pulse,
                shipColor.b * pulse,
                1f
        );

        // HUD panel + "text" using chunky quads (no font library — keeps the project portable).
        renderer.drawHudBackground();
        drawHudLegend();
        drawStatusBlocks();

        if (gameOver) {
            // Dim overlay + big "GAME OVER" bars as a primitive font.
            renderer.drawRect(0f, 0f, GameConfig.WINDOW_WIDTH_PX, GameConfig.WINDOW_HEIGHT_PX, 0f, 0f, 0f, 0.45f);
            drawGameOverBanner();
        }
    }

    private void maybeSpawnGate() {
        float topTarget = GameConfig.WINDOW_HEIGHT_PX + GameConfig.GATE_SPAWN_SPACING_PX;
        if (highestGateYBottom < topTarget) {
            float newY = highestGateYBottom + GameConfig.GATE_SPAWN_SPACING_PX;
            Gate g = Gate.create(rng, GameConfig.WINDOW_WIDTH_PX, newY);
            gates.add(g);
            highestGateYBottom = newY;
        }
    }

    /**
     * Updates {@link #highestGateYBottom} to track the topmost surviving gate.
     */
    private void recomputeHighestGateY() {
        highestGateYBottom = -10_000f;
        for (Gate gate : gates) {
            highestGateYBottom = Math.max(highestGateYBottom, gate.getYBottom());
        }
    }

    private void removeOffscreenGates() {
        Iterator<Gate> it = gates.iterator();
        boolean removed = false;
        while (it.hasNext()) {
            Gate g = it.next();
            if (g.getYTop() < -120f) {
                it.remove();
                removed = true;
            }
        }
        if (removed) {
            recomputeHighestGateY();
        }
    }

    /**
     * Axis-aligned overlap test between player and gate band, then partitions horizontal contact into
     * "wall" vs "gap" regions.
     */
    private void resolveGateInteractions() {
        for (Gate gate : gates) {
            if (gate.isDisposed()) {
                continue;
            }

            float gateBottom = gate.getYBottom();
            float gateTop = gate.getYTop();

            // Vertical overlap between player and the gate band?
            boolean yOverlap = player.maxY() >= gateBottom && player.minY() <= gateTop;
            if (!yOverlap) {
                continue;
            }

            float gapL = gate.getGapLeft();
            float gapR = gate.getGapRight();
            float w = GameConfig.WINDOW_WIDTH_PX;

            // Axis-aligned overlap against the two solid wall slabs (everything outside the gap).
            boolean hitsLeftWall = Math.min(player.maxX(), gapL) > Math.max(player.minX(), 0f);
            boolean hitsRightWall = Math.min(player.maxX(), w) > Math.max(player.minX(), gapR);

            if (hitsLeftWall || hitsRightWall) {
                applyHurt();
                gate.markDisposed();
                continue;
            }

            // Horizontally contained within the gap opening (no wall contact).
            int need = gate.getRequired().ordinal();
            if (player.getColorChannel() == need) {
                if (!gate.isScoreAwarded()) {
                    gate.setScoreAwarded(true);
                    score++;
                    // Slightly raise difficulty so the demo does not stall visually.
                    gateSpeedPxPerSec = Math.min(
                            GameConfig.GATE_MAX_SPEED_PX_PER_SEC,
                            gateSpeedPxPerSec + GameConfig.GATE_SPEED_STEP_PX_PER_SEC
                    );
                }
            } else {
                if (!gate.isWrongColorPenaltyApplied()) {
                    gate.setWrongColorPenaltyApplied(true);
                    applyHurt();
                }
            }
        }
    }

    private void applyHurt() {
        lives--;
        hurtCooldownSeconds = GameConfig.HURT_INVULN_SECONDS;
        if (lives <= 0) {
            gameOver = true;
        }
    }

    private void resetRun() {
        gameOver = false;
        score = 0;
        lives = GameConfig.MAX_LIVES;
        gateSpeedPxPerSec = GameConfig.GATE_START_SPEED_PX_PER_SEC;
        gates.clear();
        highestGateYBottom = -10_000f;
        float y = GameConfig.WINDOW_HEIGHT_PX + 40f;
        for (int i = 0; i < 4; i++) {
            Gate g = Gate.create(rng, GameConfig.WINDOW_WIDTH_PX, y);
            gates.add(g);
            highestGateYBottom = Math.max(highestGateYBottom, g.getYBottom());
            y += GameConfig.GATE_SPAWN_SPACING_PX;
        }
        player.setColorChannel(0);
        // Re-center the ship so each run starts fair after horizontal drift.
        player.applyHorizontalDelta(
                GameConfig.WINDOW_WIDTH_PX * 0.5f - player.getX(),
                GameConfig.WINDOW_WIDTH_PX
        );
    }

    private void drawGate(Gate gate) {
        float y0 = gate.getYBottom();
        float y1 = gate.getYTop();
        float gapL = gate.getGapLeft();
        float gapR = gate.getGapRight();
        float w = GameConfig.WINDOW_WIDTH_PX;

        // Left wall slab.
        renderer.drawRect(0f, y0, gapL, y1, 0.12f, 0.12f, 0.14f, 1f);
        // Right wall slab.
        renderer.drawRect(gapR, y0, w, y1, 0.12f, 0.12f, 0.14f, 1f);

        Gate.Channel c = gate.getRequired();
        float t = (float) (System.nanoTime() * 1e-9);
        float glowPulse = 0.88f + 0.12f * (float) Math.sin(t * 5.5f);

        // Wide additive halo so the required hue reads as a "glow" against dark walls.
        float expand = 24f;
        renderer.setBlendAdditive();
        renderer.drawRect(
                gapL - expand,
                y0 - 6f,
                gapR + expand,
                y1 + 6f,
                c.r * 0.65f,
                c.g * 0.65f,
                c.b * 0.65f,
                0.32f * glowPulse);
        renderer.drawRect(
                gapL - 12f,
                y0 - 3f,
                gapR + 12f,
                y1 + 3f,
                c.r,
                c.g,
                c.b,
                0.38f * glowPulse);
        renderer.setBlendNormal();

        // Core fill: bright enough that red vs green vs blue is obvious.
        renderer.drawRect(
                gapL,
                y0,
                gapR,
                y1,
                c.r * 0.55f * glowPulse,
                c.g * 0.55f * glowPulse,
                c.b * 0.55f * glowPulse,
                0.78f);
        float inset = 5f;
        renderer.drawRect(
                gapL + inset,
                y0 + inset,
                gapR - inset,
                y1 - inset,
                c.r * glowPulse,
                c.g * glowPulse,
                c.b * glowPulse,
                0.98f);

        // Thin light bands on the gap — marks the "safe slot" even on muted displays.
        float band = 3.5f;
        float br = Math.min(1f, c.r + 0.35f);
        float bg = Math.min(1f, c.g + 0.35f);
        float bb = Math.min(1f, c.b + 0.35f);
        renderer.drawRect(gapL, y0, gapR, y0 + band, br, bg, bb, 0.75f);
        renderer.drawRect(gapL, y1 - band, gapR, y1, br, bg, bb, 0.65f);
    }

    private Gate.Channel channelFromIndex(int idx) {
        return Gate.Channel.values()[idx];
    }

    /**
     * Draws chunky color key reminders at the top (R/G/B blocks + key labels as three small squares).
     */
    private void drawHudLegend() {
        float top = GameConfig.WINDOW_HEIGHT_PX - 72f;
        float x = 18f;
        drawKeyChip(x, top, Gate.Channel.RED, player.getColorChannel() == 0);
        drawKeyChip(x + 120f, top, Gate.Channel.GREEN, player.getColorChannel() == 1);
        drawKeyChip(x + 240f, top, Gate.Channel.BLUE, player.getColorChannel() == 2);
    }

    private void drawKeyChip(float x, float y, Gate.Channel ch, boolean active) {
        float s = 50f;
        float pad = active ? 9f : 4f;
        if (active) {
            renderer.drawRect(x - pad - 3f, y - pad - 3f, x + s + pad + 3f, y + s + pad + 3f, 1f, 0.78f, 0.1f, 1f);
            renderer.drawRect(x - pad, y - pad, x + s + pad, y + s + pad, 0.06f, 0.06f, 0.09f, 1f);
        } else {
            renderer.drawRect(x - 4f, y - 4f, x + s + 4f, y + s + 4f, 0.32f, 0.32f, 0.36f, 0.92f);
        }
        renderer.drawRect(x, y, x + s, y + s, ch.r, ch.g, ch.b, 1f);
    }

    private void drawStatusBlocks() {
        float top = GameConfig.WINDOW_HEIGHT_PX - 30f;
        float x = GameConfig.WINDOW_WIDTH_PX - 260f;
        renderer.drawRect(x, top, x + 240f, top + 22f, 0f, 0f, 0f, 0.35f);
        // Score bar grows with score (visual only; capped for layout).
        float frac = Math.min(1f, score / 30f);
        renderer.drawRect(x + 2f, top + 2f, x + 2f + frac * 236f, top + 20f, 0.85f, 0.85f, 0.25f, 0.9f);

        // Lives: three small squares.
        float lx = x - 70f;
        for (int i = 0; i < GameConfig.MAX_LIVES; i++) {
            float alpha = i < lives ? 1f : 0.15f;
            renderer.drawRect(lx + i * 22f, top + 4f, lx + i * 22f + 16f, top + 18f, 1f, 0.35f, 0.35f, alpha);
        }
    }

    private void drawGameOverBanner() {
        float cx = GameConfig.WINDOW_WIDTH_PX * 0.5f;
        float cy = GameConfig.WINDOW_HEIGHT_PX * 0.5f;
        // Simple cross-bar "banner".
        renderer.drawRect(cx - 260f, cy - 40f, cx + 260f, cy + 40f, 0.1f, 0.1f, 0.12f, 0.95f);
        renderer.drawRect(cx - 250f, cy - 30f, cx + 250f, cy + 30f, 0.85f, 0.2f, 0.2f, 0.95f);
    }
}
