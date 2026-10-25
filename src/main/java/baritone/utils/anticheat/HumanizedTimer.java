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

import java.util.Random;

/**
 * Adds human-like timing jitter to Baritone actions to avoid anti-cheat detection.
 * <p>
 * Anti-cheat systems look for perfectly consistent timing in:
 * <ul>
 *   <li>Block breaking intervals</li>
 *   <li>Rotation speed per tick</li>
 *   <li>Sprint toggling patterns</li>
 *   <li>Inventory interaction delays</li>
 * </ul>
 * <p>
 * This class provides random delay generators that mimic natural human variance.
 *
 * @author Baritone Contributors
 */
public final class HumanizedTimer {

    private static final Random RANDOM = new Random();

    /**
     * Minimum jitter in ticks (applied to delays).
     */
    private static int minJitterTicks = 0;

    /**
     * Maximum jitter in ticks (applied to delays).
     */
    private static int maxJitterTicks = 3;

    /**
     * Whether humanization is currently active.
     */
    private static boolean enabled = true;

    private HumanizedTimer() {}

    /**
     * Enable or disable humanized timing.
     */
    public static void setEnabled(boolean enabled) {
        HumanizedTimer.enabled = enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Configure the jitter range.
     *
     * @param min minimum extra ticks (inclusive)
     * @param max maximum extra ticks (inclusive)
     */
    public static void setJitterRange(int min, int max) {
        minJitterTicks = Math.max(0, min);
        maxJitterTicks = Math.max(minJitterTicks, max);
    }

    public static int getMinJitterTicks() {
        return minJitterTicks;
    }

    public static int getMaxJitterTicks() {
        return maxJitterTicks;
    }

    // ======================================================================
    //  Delay generators
    // ======================================================================

    /**
     * Add human-like jitter to a base tick delay.
     *
     * @param baseTicks the baseline delay in ticks
     * @return baseTicks + random jitter (or just baseTicks if disabled)
     */
    public static int jitter(int baseTicks) {
        if (!enabled || maxJitterTicks <= 0) {
            return baseTicks;
        }
        return baseTicks + minJitterTicks + RANDOM.nextInt(maxJitterTicks - minJitterTicks + 1);
    }

    /**
     * Generate a humanized delay for block breaking (tick count).
     * <p>
     * The delay is based on the setting {@code blockBreakSpeed} but with random variance
     * so the rhythm is not perfectly regular.
     */
    public static int blockBreakDelay() {
        // Always read enabled flag from settings so GUI toggles take effect immediately
        enabled = Baritone.settings().antiCheatHumanizedTiming.value;
        int base = Baritone.settings().blockBreakSpeed.value;
        return jitter(base);
    }

    /**
     * Generate a humanized delay between inventory moves.
     */
    public static int inventoryMoveDelay() {
        enabled = Baritone.settings().antiCheatHumanizedTiming.value;
        int base = Baritone.settings().ticksBetweenInventoryMoves.value;
        return jitter(base);
    }

    /**
     * Generate a humanized delay for right-click actions.
     */
    public static int rightClickDelay() {
        enabled = Baritone.settings().antiCheatHumanizedTiming.value;
        int base = Baritone.settings().rightClickSpeed.value;
        return jitter(base);
    }

    /**
     * Decide randomly whether to add a micro-pause before an action.
     * Useful for making interactions feel less robotic.
     *
     * @param chancePercent 0–100 probability of pausing
     * @return true if a micro-pause should occur
     */
    public static boolean shouldMicroPause(int chancePercent) {
        if (!enabled) return false;
        return RANDOM.nextInt(100) < chancePercent;
    }

    /**
     * Generate a small random rotation offset in degrees.
     * Anti-cheats flag perfectly aligned rotations.
     *
     * @param maxDegrees maximum offset in degrees
     * @return a random value in [-maxDegrees, +maxDegrees]
     */
    public static float rotationJitter(float maxDegrees) {
        if (!enabled) return 0f;
        return (RANDOM.nextFloat() * 2f - 1f) * maxDegrees;
    }
}
