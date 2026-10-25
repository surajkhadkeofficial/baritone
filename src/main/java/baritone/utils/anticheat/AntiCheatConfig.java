/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils.anticheat;

import baritone.Baritone;
import baritone.api.Settings;

/**
 * Centralized anti-cheat configuration manager.
 * <p>
 * Provides presets for common anti-cheat plugins and applies safe defaults
 * to all relevant Baritone settings in one step.
 * <p>
 * The goal is to make Baritone's movement, rotation, and interaction patterns
 * indistinguishable from a regular human player, thereby avoiding detection
 * and kicks from server-side anti-cheat systems.
 *
 * @author Baritone Contributors
 */
public final class AntiCheatConfig {

    /**
     * Known anti-cheat presets
     */
    public enum Preset {
        /** No anti-cheat evasion – raw performance */
        NONE,
        /** Safe defaults that work on most servers */
        SAFE,
        /** Tuned for NoCheatPlus */
        NCP,
        /** Tuned for Advanced Anti-Cheat (AAC) */
        AAC,
        /** Tuned for Vulcan Anti-Cheat */
        VULCAN,
        /** Tuned for Grim Anti-Cheat */
        GRIM,
        /** Tuned for Matrix Anti-Cheat */
        MATRIX,
        /** Maximum stealth – slowest but hardest to detect */
        PARANOID
    }

    private static Preset activePreset = Preset.SAFE;

    private AntiCheatConfig() {}

    /**
     * @return the currently active preset
     */
    public static Preset getActivePreset() {
        return activePreset;
    }

    /**
     * Apply an anti-cheat preset, adjusting all relevant settings at once.
     *
     * @param preset the preset to apply
     */
    public static void applyPreset(Preset preset) {
        activePreset = preset;
        final Settings s = Baritone.settings();

        // ------------------------------------------------------------------
        // 1. Reset everything to factory defaults first
        // ------------------------------------------------------------------
        resetToDefaults(s);

        switch (preset) {
            case NONE:
                // Nothing extra – use factory defaults
                break;

            case SAFE:
                applySafe(s);
                break;

            case NCP:
                applyNCP(s);
                break;

            case AAC:
                applyAAC(s);
                break;

            case VULCAN:
                applyVulcan(s);
                break;

            case GRIM:
                applyGrim(s);
                break;

            case MATRIX:
                applyMatrix(s);
                break;

            case PARANOID:
                applyParanoid(s);
                break;
        }

        // Keep HumanizedTimer in sync with the newly applied settings
        HumanizedTimer.setEnabled(s.antiCheatHumanizedTiming.value);
        HumanizedTimer.setJitterRange(s.antiCheatTimingJitterMin.value, s.antiCheatTimingJitterMax.value);
    }

    // ======================================================================
    //  Preset implementations
    // ======================================================================

    /**
     * SAFE – works on the vast majority of servers.
     * <p>
     * Key ideas:
     * <ul>
     *   <li>Enable anti-cheat compatibility flag</li>
     *   <li>Smooth look rotations so they appear human</li>
     *   <li>Use freeLook so the client-visible head doesn't snap</li>
     *   <li>Randomize look slightly each tick</li>
     *   <li>Limit sprint/parkour to avoid speed flags</li>
     *   <li>Use legit mining (no X-ray)</li>
     *   <li>Slow down inventory moves</li>
     * </ul>
     */
    private static void applySafe(Settings s) {
        // -- Rotation --
        s.antiCheatCompatibility.value = true;
        s.freeLook.value = true;
        s.smoothLook.value = true;
        s.smoothLookTicks.value = 6;
        s.randomLooking.value = 0.02d;
        s.randomLooking113.value = 3d;
        s.remainWithExistingLookDirection.value = true;

        // -- Movement --
        s.allowSprint.value = true;
        s.allowParkour.value = false;
        s.allowParkourPlace.value = false;
        s.sprintAscends.value = false;
        s.sprintJumping.value = false;
        s.headHitters.value = false;
        s.allowDiagonalDescend.value = false;
        s.allowDiagonalAscend.value = false;
        s.overshootTraverse.value = false;

        // -- Mining --
        s.legitMine.value = true;
        s.allowOnlyExposedOres.value = true;
        s.walkWhileBreaking.value = false;
        s.blockBreakSpeed.value = 6;

        // -- Inventory --
        s.inventoryMoveOnlyIfStationary.value = true;
        s.ticksBetweenInventoryMoves.value = 3;

        // -- Reach --
        s.blockReachDistance.value = 4.5f;

        // -- Path rendering (minimal to avoid visual artifacts that mods can detect) --
        s.renderPath.value = false;
        s.renderGoal.value = false;
    }

    /**
     * NCP (NoCheatPlus) – the most common anti-cheat.
     * <p>
     * NCP mainly checks:
     * <ul>
     *   <li>Speed / fly distance per tick</li>
     *   <li>Illegal rotations (snap turns)</li>
     *   <li>Reach distance</li>
     *   <li>Sprinting in invalid directions</li>
     * </ul>
     */
    private static void applyNCP(Settings s) {
        applySafe(s); // start from safe baseline

        // NCP is lenient on sprint if direction matches
        s.allowSprint.value = true;
        s.sprintAscends.value = true;

        // NCP checks rotation delta – use more smoothing
        s.smoothLookTicks.value = 5;
        s.randomLooking.value = 0.015d;
        s.randomLooking113.value = 2.5d;

        // NCP tolerates normal block break speed
        s.blockBreakSpeed.value = 6;
        s.walkWhileBreaking.value = true;
    }

    /**
     * AAC (Advanced Anti-Cheat) – stricter rotation checks.
     */
    private static void applyAAC(Settings s) {
        applySafe(s);

        // AAC detects sprinting sideways aggressively
        s.antiCheatCompatibility.value = true;
        s.allowSprint.value = true;
        s.sprintAscends.value = false;

        // Tighter rotation smoothing
        s.smoothLookTicks.value = 8;
        s.randomLooking.value = 0.03d;
        s.randomLooking113.value = 3.5d;

        // AAC is picky about reach
        s.blockReachDistance.value = 4.25f;

        // Slower inventory actions
        s.ticksBetweenInventoryMoves.value = 4;
    }

    /**
     * Vulcan – very strict on movement speed and packets.
     */
    private static void applyVulcan(Settings s) {
        applySafe(s);

        // Vulcan is harsh on any speed exploit
        s.allowSprint.value = true;
        s.sprintAscends.value = false;
        s.sprintJumping.value = false;
        s.headHitters.value = false;

        // Slow, smooth rotations
        s.smoothLookTicks.value = 8;
        s.randomLooking.value = 0.02d;
        s.randomLooking113.value = 2d;

        // Conservative reach
        s.blockReachDistance.value = 4.2f;

        // Vulcan checks inventory timing
        s.inventoryMoveOnlyIfStationary.value = true;
        s.ticksBetweenInventoryMoves.value = 5;

        // No walk-while-break
        s.walkWhileBreaking.value = false;
        s.blockBreakSpeed.value = 7;
    }

    /**
     * Grim – prediction-based anti-cheat, very hard to bypass.
     */
    private static void applyGrim(Settings s) {
        applySafe(s);

        // Grim simulates client movement – don't do anything unusual
        s.allowSprint.value = true;
        s.sprintAscends.value = false;
        s.sprintJumping.value = false;
        s.allowParkour.value = false;
        s.allowParkourPlace.value = false;
        s.allowDiagonalDescend.value = false;
        s.allowDiagonalAscend.value = false;

        // Very smooth rotations
        s.smoothLookTicks.value = 10;
        s.randomLooking.value = 0.025d;
        s.randomLooking113.value = 3d;

        // Conservative
        s.blockReachDistance.value = 4.0f;
        s.walkWhileBreaking.value = false;
        s.blockBreakSpeed.value = 7;

        // Grim is strict with inventory
        s.inventoryMoveOnlyIfStationary.value = true;
        s.ticksBetweenInventoryMoves.value = 5;
    }

    /**
     * Matrix – common on Asian servers, checks movement packets.
     */
    private static void applyMatrix(Settings s) {
        applySafe(s);

        s.allowSprint.value = true;
        s.sprintAscends.value = false;

        // Matrix doesn't like big rotation changes
        s.smoothLookTicks.value = 7;
        s.randomLooking.value = 0.02d;
        s.randomLooking113.value = 2.5d;

        s.blockReachDistance.value = 4.3f;
        s.walkWhileBreaking.value = false;
        s.blockBreakSpeed.value = 6;

        s.inventoryMoveOnlyIfStationary.value = true;
        s.ticksBetweenInventoryMoves.value = 4;
    }

    /**
     * PARANOID – maximum stealth, slowest but hardest to detect.
     */
    private static void applyParanoid(Settings s) {
        applySafe(s);

        // Almost no sprinting
        s.allowSprint.value = false;
        s.sprintAscends.value = false;
        s.sprintJumping.value = false;

        // No risky movement
        s.allowParkour.value = false;
        s.allowParkourPlace.value = false;
        s.allowParkourAscend.value = false;
        s.allowDiagonalDescend.value = false;
        s.allowDiagonalAscend.value = false;
        s.headHitters.value = false;
        s.overshootTraverse.value = false;

        // Maximum rotation smoothness
        s.smoothLookTicks.value = 12;
        s.randomLooking.value = 0.04d;
        s.randomLooking113.value = 4d;

        // Very short reach
        s.blockReachDistance.value = 3.8f;
        s.walkWhileBreaking.value = false;
        s.blockBreakSpeed.value = 8;

        // Very slow inventory
        s.inventoryMoveOnlyIfStationary.value = true;
        s.ticksBetweenInventoryMoves.value = 8;

        // Legit everything
        s.legitMine.value = true;
        s.allowOnlyExposedOres.value = true;

        // No rendering (stealth)
        s.renderPath.value = false;
        s.renderGoal.value = false;
        s.renderSelectionBoxes.value = false;
    }

    // ======================================================================
    //  Helpers
    // ======================================================================

    /**
     * Reset only the settings we care about to their declared defaults.
     */
    private static void resetToDefaults(Settings s) {
        s.antiCheatCompatibility.reset();
        s.freeLook.reset();
        s.smoothLook.reset();
        s.smoothLookTicks.reset();
        s.randomLooking.reset();
        s.randomLooking113.reset();
        s.remainWithExistingLookDirection.reset();
        s.allowSprint.reset();
        s.allowParkour.reset();
        s.allowParkourPlace.reset();
        s.allowParkourAscend.reset();
        s.sprintAscends.reset();
        s.sprintJumping.reset();
        s.headHitters.reset();
        s.allowDiagonalDescend.reset();
        s.allowDiagonalAscend.reset();
        s.overshootTraverse.reset();
        s.legitMine.reset();
        s.allowOnlyExposedOres.reset();
        s.walkWhileBreaking.reset();
        s.blockBreakSpeed.reset();
        s.inventoryMoveOnlyIfStationary.reset();
        s.ticksBetweenInventoryMoves.reset();
        s.blockReachDistance.reset();
        s.renderPath.reset();
        s.renderGoal.reset();
        s.renderSelectionBoxes.reset();
    }

    /**
     * Get a human-readable description of the given preset.
     */
    public static String getPresetDescription(Preset preset) {
        switch (preset) {
            case NONE:     return "No anti-cheat evasion. Maximum performance.";
            case SAFE:     return "Safe defaults for most servers. Balanced speed and stealth.";
            case NCP:      return "Optimized for NoCheatPlus. Allows sprint with safe rotations.";
            case AAC:      return "Optimized for Advanced Anti-Cheat. Strict rotation smoothing.";
            case VULCAN:   return "Optimized for Vulcan. Conservative speed and reach.";
            case GRIM:     return "Optimized for Grim. Very smooth, prediction-safe movement.";
            case MATRIX:   return "Optimized for Matrix. Careful packet and rotation handling.";
            case PARANOID: return "Maximum stealth. Slowest but virtually undetectable.";
            default:       return "";
        }
    }
}
