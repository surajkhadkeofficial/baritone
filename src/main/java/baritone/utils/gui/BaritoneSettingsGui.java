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

package baritone.utils.gui;

import baritone.Baritone;
import baritone.api.Settings;
import baritone.utils.anticheat.AntiCheatConfig;
import baritone.utils.anticheat.HumanizedTimer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A modern, clean GUI for configuring Baritone's anti-cheat settings and modules.
 * <p>
 * Provides tabbed panels for:
 * <ul>
 *   <li><b>Anti-Cheat</b> – preset selection and fine-tuning</li>
 *   <li><b>Movement</b> – sprint, parkour, reach, etc.</li>
 *   <li><b>Mining</b> – legit mine, block break speed, ore settings</li>
 *   <li><b>Rendering</b> – path / goal / selection visibility</li>
 *   <li><b>Misc</b> – inventory, elytra, chat control</li>
 * </ul>
 *
 * @author Baritone Contributors
 */
public class BaritoneSettingsGui extends Screen {

    // ======================================================================
    //  Tab definitions
    // ======================================================================

    private enum Tab {
        ANTI_CHEAT("Anti-Cheat"),
        MOVEMENT("Movement"),
        MINING("Mining"),
        BUILD("Build"),
        FINDER("Finder"),
        RENDERING("Rendering"),
        MISC("Misc");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private Tab activeTab = Tab.ANTI_CHEAT;

    // -- layout constants --
    private static final int TAB_HEIGHT = 24;
    private static final int LEFT_MARGIN = 20;
    private static final int TOGGLE_WIDTH = 40;
    private static final int TOGGLE_HEIGHT = 16;
    private static final int ROW_HEIGHT = 22;

    // -- colors --
    private static final int COLOR_BG           = 0xCC101020;
    private static final int COLOR_TAB_ACTIVE   = 0xFF3A7BD5;
    private static final int COLOR_TAB_INACTIVE = 0xFF1E1E30;
    private static final int COLOR_TAB_HOVER    = 0xFF2C3E6B;
    private static final int COLOR_PANEL_BG     = 0xDD15152A;
    private static final int COLOR_TOGGLE_ON    = 0xFF00C853;
    private static final int COLOR_TOGGLE_OFF   = 0xFFB71C1C;
    private static final int COLOR_TOGGLE_KNOB  = 0xFFFFFFFF;
    private static final int COLOR_TEXT          = 0xFFE0E0E0;
    private static final int COLOR_HEADER       = 0xFF90CAF9;
    private static final int COLOR_DESC         = 0xFF888888;
    private static final int COLOR_PRESET_SEL   = 0xFF4CAF50;
    private static final int COLOR_PRESET_NORM  = 0xFF2E2E4A;

    // -- scroll --
    private int scrollOffset = 0;
    private int maxScroll = 0;

    // -- toggle entries for the current tab (rebuilt on tab switch) --
    private final List<ToggleEntry> toggleEntries = new ArrayList<>();

    // ======================================================================
    //  Entry model
    // ======================================================================

    private static class ToggleEntry {
        final String label;
        final String description;
        final Settings.Setting<Boolean> setting;
        // calculated screen positions (set during render)
        int y;

        ToggleEntry(String label, String description, Settings.Setting<Boolean> setting) {
            this.label = label;
            this.description = description;
            this.setting = setting;
        }
    }

    // ======================================================================
    //  Constructor
    // ======================================================================

    public BaritoneSettingsGui() {
        super(Component.literal("Baritone Settings"));
    }

    // ======================================================================
    //  init – called when the screen is created / resized
    // ======================================================================

    @Override
    protected void init() {
        super.init();
        rebuildEntries();
    }

    // ======================================================================
    //  Rendering
    // ======================================================================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // -- full-screen dark backdrop --
        graphics.fill(0, 0, this.width, this.height, COLOR_BG);

        // -- title bar --
        drawCenteredText(graphics, "BARITONE+  CONTROL PANEL  v1.13.1", this.width / 2, 8, 0xFFFFFFFF);

        // -- tabs --
        renderTabs(graphics, mouseX, mouseY);

        int contentTop = 32 + TAB_HEIGHT;
        int contentBottom = this.height - 10;

        // -- panel background --
        graphics.fill(LEFT_MARGIN - 4, contentTop - 4, this.width - LEFT_MARGIN + 4, contentBottom + 4, COLOR_PANEL_BG);

        // -- enable scissoring so content clips inside the panel --
        enableScissor(graphics, LEFT_MARGIN - 4, contentTop, this.width - LEFT_MARGIN + 4, contentBottom);

        // -- render content depending on active tab --
        if (activeTab == Tab.ANTI_CHEAT) {
            renderAntiCheatTab(graphics, mouseX, mouseY, contentTop, contentBottom);
        } else {
            renderToggleList(graphics, mouseX, mouseY, contentTop, contentBottom);
        }

        disableScissor(graphics);

        // -- scroll indicator --
        if (maxScroll > 0) {
            int barH = Math.max(10, (contentBottom - contentTop) * (contentBottom - contentTop) / ((contentBottom - contentTop) + maxScroll));
            int barY = contentTop + (int) ((contentBottom - contentTop - barH) * ((float) scrollOffset / maxScroll));
            graphics.fill(this.width - LEFT_MARGIN, barY, this.width - LEFT_MARGIN + 3, barY + barH, 0x80FFFFFF);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // ======================================================================
    //  Tab bar
    // ======================================================================

    private void renderTabs(GuiGraphics g, int mx, int my) {
        Tab[] tabs = Tab.values();
        int tabW = (this.width - 2 * LEFT_MARGIN) / tabs.length;
        int y = 30;

        for (int i = 0; i < tabs.length; i++) {
            int x = LEFT_MARGIN + i * tabW;
            boolean hovered = mx >= x && mx < x + tabW && my >= y && my < y + TAB_HEIGHT;
            int bg = tabs[i] == activeTab ? COLOR_TAB_ACTIVE : (hovered ? COLOR_TAB_HOVER : COLOR_TAB_INACTIVE);
            g.fill(x, y, x + tabW - 1, y + TAB_HEIGHT, bg);
            drawCenteredText(g, tabs[i].label, x + tabW / 2, y + (TAB_HEIGHT - 8) / 2, 0xFFFFFFFF);
        }
    }

    // ======================================================================
    //  Anti-Cheat tab (preset selection + humanized timer toggles)
    // ======================================================================

    private void renderAntiCheatTab(GuiGraphics g, int mx, int my, int top, int bottom) {
        int y = top - scrollOffset + 6;

        // Section: Presets
        drawText(g, "§l§nAnti-Cheat Presets", LEFT_MARGIN, y, COLOR_HEADER);
        y += 16;
        drawText(g, "Select a preset to auto-configure all settings for a specific anti-cheat:", LEFT_MARGIN, y, COLOR_DESC);
        y += 14;

        AntiCheatConfig.Preset activePreset = AntiCheatConfig.getActivePreset();
        for (AntiCheatConfig.Preset preset : AntiCheatConfig.Preset.values()) {
            boolean selected = preset == activePreset;
            boolean hovered = mx >= LEFT_MARGIN && mx < this.width - LEFT_MARGIN && my >= y && my < y + 22;
            int bg = selected ? COLOR_PRESET_SEL : (hovered ? COLOR_TAB_HOVER : COLOR_PRESET_NORM);
            g.fill(LEFT_MARGIN, y, this.width - LEFT_MARGIN, y + 20, bg);

            String prefix = selected ? " ✔ " : "   ";
            drawText(g, prefix + preset.name(), LEFT_MARGIN + 4, y + 5, 0xFFFFFFFF);
            drawText(g, AntiCheatConfig.getPresetDescription(preset), LEFT_MARGIN + 120, y + 5, COLOR_DESC);
            y += 24;
        }

        y += 10;

        // Section: Humanized Timer
        drawText(g, "§l§nHumanized Timer", LEFT_MARGIN, y, COLOR_HEADER);
        y += 16;
        drawText(g, "Adds random delays to actions so timing looks human", LEFT_MARGIN, y, COLOR_DESC);
        y += 14;

        // Toggle for enabled
        drawToggle(g, "Enabled", HumanizedTimer.isEnabled(), LEFT_MARGIN, y, mx, my);
        y += ROW_HEIGHT;

        // Jitter range display
        drawText(g, "Jitter Range: " + HumanizedTimer.getMinJitterTicks() + " – " + HumanizedTimer.getMaxJitterTicks() + " ticks", LEFT_MARGIN + 10, y + 3, COLOR_TEXT);
        y += ROW_HEIGHT;

        y += 10;

        // Section: Current Active Settings Overview
        drawText(g, "§l§nCurrent Key Settings", LEFT_MARGIN, y, COLOR_HEADER);
        y += 16;

        Settings s = Baritone.settings();
        String[][] info = {
                {"Anti-Cheat Compat", bool(s.antiCheatCompatibility)},
                {"Free Look", bool(s.freeLook)},
                {"Smooth Look", bool(s.smoothLook) + " (" + s.smoothLookTicks.value + " ticks)"},
                {"Random Look", String.format("%.3f / %.1f", s.randomLooking.value, s.randomLooking113.value)},
                {"Sprint", bool(s.allowSprint)},
                {"Sprint Ascend", bool(s.sprintAscends)},
                {"Parkour", bool(s.allowParkour)},
                {"Legit Mine", bool(s.legitMine)},
                {"Block Reach", String.format("%.1f", s.blockReachDistance.value)},
                {"Block Break Speed", s.blockBreakSpeed.value + " ticks"},
                {"Walk While Breaking", bool(s.walkWhileBreaking)},
                {"Inventory Stationary Only", bool(s.inventoryMoveOnlyIfStationary)},
                {"Inventory Move Delay", s.ticksBetweenInventoryMoves.value + " ticks"},
        };

        for (String[] row : info) {
            drawText(g, "  " + row[0] + ":", LEFT_MARGIN, y + 3, COLOR_TEXT);
            drawText(g, row[1], LEFT_MARGIN + 200, y + 3,
                    row[1].startsWith("ON") ? COLOR_TOGGLE_ON : (row[1].startsWith("OFF") ? COLOR_TOGGLE_OFF : 0xFFCCCCCC));
            y += ROW_HEIGHT - 4;
        }

        maxScroll = Math.max(0, (y + scrollOffset) - bottom + 10);
    }

    // ======================================================================
    //  Generic toggle list (for Movement, Mining, Rendering, Misc)
    // ======================================================================

    private void renderToggleList(GuiGraphics g, int mx, int my, int top, int bottom) {
        int y = top - scrollOffset + 6;
        for (ToggleEntry entry : toggleEntries) {
            entry.y = y;
            drawToggle(g, entry.label, entry.setting.value, LEFT_MARGIN, y, mx, my);
            drawText(g, entry.description, LEFT_MARGIN + TOGGLE_WIDTH + 90, y + 3, COLOR_DESC);
            y += ROW_HEIGHT;
        }
        maxScroll = Math.max(0, (y + scrollOffset) - bottom + 10);
    }

    // ======================================================================
    //  Toggle widget drawing
    // ======================================================================

    private void drawToggle(GuiGraphics g, String label, boolean value, int x, int y, int mx, int my) {
        // label
        drawText(g, label, x + TOGGLE_WIDTH + 6, y + 3, COLOR_TEXT);

        // track
        int tx = x;
        int ty = y + 2;
        int trackColor = value ? COLOR_TOGGLE_ON : COLOR_TOGGLE_OFF;
        g.fill(tx, ty, tx + TOGGLE_WIDTH, ty + TOGGLE_HEIGHT, trackColor);

        // knob
        int knobX = value ? tx + TOGGLE_WIDTH - TOGGLE_HEIGHT : tx;
        g.fill(knobX + 2, ty + 2, knobX + TOGGLE_HEIGHT - 2, ty + TOGGLE_HEIGHT - 2, COLOR_TOGGLE_KNOB);
    }

    // ======================================================================
    //  Mouse handling
    // ======================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // -- Tab clicks --
        Tab[] tabs = Tab.values();
        int tabW = (this.width - 2 * LEFT_MARGIN) / tabs.length;
        int tabY = 30;
        for (int i = 0; i < tabs.length; i++) {
            int tx = LEFT_MARGIN + i * tabW;
            if (mouseX >= tx && mouseX < tx + tabW && mouseY >= tabY && mouseY < tabY + TAB_HEIGHT) {
                activeTab = tabs[i];
                scrollOffset = 0;
                rebuildEntries();
                return true;
            }
        }

        // -- Anti-Cheat tab: preset clicks --
        if (activeTab == Tab.ANTI_CHEAT) {
            int contentTop = 32 + TAB_HEIGHT;
            int y = contentTop - scrollOffset + 6 + 16 + 14; // after header + description
            for (AntiCheatConfig.Preset preset : AntiCheatConfig.Preset.values()) {
                if (mouseX >= LEFT_MARGIN && mouseX < this.width - LEFT_MARGIN && mouseY >= y && mouseY < y + 20) {
                    AntiCheatConfig.applyPreset(preset);
                    return true;
                }
                y += 24;
            }

            // Humanized timer toggle
            y += 10 + 16 + 14; // section header + description
            if (isToggleClicked(LEFT_MARGIN, y, mouseX, mouseY)) {
                HumanizedTimer.setEnabled(!HumanizedTimer.isEnabled());
                return true;
            }
        }

        // -- Toggle list clicks --
        if (activeTab != Tab.ANTI_CHEAT) {
            for (ToggleEntry entry : toggleEntries) {
                if (isToggleClicked(LEFT_MARGIN, entry.y, mouseX, mouseY)) {
                    entry.setting.value = !entry.setting.value;
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isToggleClicked(int x, int y, double mx, double my) {
        return mx >= x && mx < x + TOGGLE_WIDTH && my >= y + 2 && my < y + 2 + TOGGLE_HEIGHT;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (verticalAmount * 10)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // Escape
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ======================================================================
    //  Build toggle entries for the active tab
    // ======================================================================

    private void rebuildEntries() {
        toggleEntries.clear();
        Settings s = Baritone.settings();

        switch (activeTab) {
            case MOVEMENT:
                add("Allow Sprint", "Let the bot sprint to move faster", s.allowSprint);
                add("Sprint Ascends", "Sprint-jump up single-block steps", s.sprintAscends);
                add("Sprint Jumping", "Sprint-jump along straight paths", s.sprintJumping);
                add("Head Hitters", "Sprint-jump in 1x2 corridors", s.headHitters);
                add("Allow Parkour", "Jump across 1-block gaps", s.allowParkour);
                add("Parkour Place", "Place blocks mid-parkour jump", s.allowParkourPlace);
                add("Parkour Ascend", "Jump up + forward simultaneously", s.allowParkourAscend);
                add("Diagonal Descend", "Move diagonally when descending", s.allowDiagonalDescend);
                add("Diagonal Ascend", "Move diagonally when ascending", s.allowDiagonalAscend);
                add("Allow Downward", "Mine the block directly beneath feet", s.allowDownward);
                add("Overshoot Traverse", "Accept landing 1 block past destination", s.overshootTraverse);
                add("Free Look", "Move without forcing client-side rotation", s.freeLook);
                add("Smooth Look", "Smooth client-side rotation transitions", s.smoothLook);
                add("Anti-Cheat Compat", "Silently set rotations for anti-cheat", s.antiCheatCompatibility);
                add("Walk On Bottom Slab", "Walk on bottom-half slabs", s.allowWalkOnBottomSlab);
                add("Allow Swimming", "Swim through water using sprint", s.allowSwimming);
                add("Sprint In Water", "Continue sprinting in water", s.sprintInWater);
                break;

            case MINING:
                add("Allow Break", "Allow breaking blocks during pathing", s.allowBreak);
                add("Allow Place", "Allow placing blocks during pathing", s.allowPlace);
                add("Auto Tool", "Automatically select the best tool", s.autoTool);
                add("Legit Mine", "Only mine ores you can see (no X-ray)", s.legitMine);
                add("Exposed Ores Only", "Only mine ores touching air/liquid", s.allowOnlyExposedOres);
                add("Walk While Breaking", "Don't stop walking to mine", s.walkWhileBreaking);
                add("Item Saver", "Stop using tools before they break", s.itemSaver);
                add("Prefer Silk Touch", "Always prefer silk touch tools", s.preferSilkTouch);
                add("Replant Crops", "Replant crops while farming", s.replantCrops);
                add("Force Internal Mining", "Mine two blocks at once when possible", s.forceInternalMining);
                add("Pause Mining Falling", "Wait for falling blocks to settle", s.pauseMiningForFallingBlocks);
                add("Backfill", "Fill in blocks behind you", s.backfill);
                add("Scan Dropped Items", "Consider dropped items as destinations", s.mineScanDroppedItems);
                add("Use Sword To Mine", "Use sword to mine certain blocks", s.useSwordToMine);
                break;

            case BUILD:
                add("Allow Place", "Allow placing blocks during building", s.allowPlace);
                add("Allow Break", "Allow breaking wrong blocks during build", s.allowBreak);
                add("Allow Only Exported", "Only build blocks from exported schematic", s.allowOnlyExposedOres);
                add("Auto Tool", "Auto-select best tool for each block", s.autoTool);
                add("Item Saver", "Don't fully use up tools", s.itemSaver);
                add("Prefer Silk Touch", "Prefer silk touch when breaking", s.preferSilkTouch);
                add("Backfill", "Fill in blocks behind you as you build", s.backfill);
                add("Walk While Breaking", "Don't stop walking to break blocks", s.walkWhileBreaking);
                add("Anti-Cheat Compat", "Prevent illegal rotation flags during build", s.antiCheatCompatibility);
                add("Humanized Timing", "Add random delays to place/break actions", s.antiCheatHumanizedTiming);
                add("Allow Inventory", "Let bot manage inventory for build materials", s.allowInventory);
                add("Inventory Stationary", "Only move items while stationary", s.inventoryMoveOnlyIfStationary);
                add("Render Path", "Render pathing during build", s.renderPath);
                add("Render Selection Boxes", "Highlight build region", s.renderSelectionBoxes);
                break;

            case FINDER:
                add("Legit Mine", "Only target visible/exposed ores (no X-ray)", s.legitMine);
                add("Exposed Ores Only", "Only mine ores touching air/water/lava", s.allowOnlyExposedOres);
                add("Scan Dropped Items", "Consider dropped items as ore locations", s.mineScanDroppedItems);
                add("Explore For Blocks", "Explore new chunks to find ores", s.exploreForBlocks);
                add("Chunk Caching", "Cache chunks to remember ore locations", s.chunkCaching);
                add("Avoidance", "Avoid spawners/mob positions near ores", s.avoidance);
                add("Pause Mining Falling", "Wait for gravel/sand to fall before continuing", s.pauseMiningForFallingBlocks);
                add("Walk While Breaking", "Keep moving while mining ores", s.walkWhileBreaking);
                add("Blacklist On Fail", "Blacklist unreachable ores instead of canceling", s.blacklistClosestOnFailure);
                add("Auto Tool", "Auto-select the fastest tool for each ore", s.autoTool);
                add("Use Sword To Mine", "Use sword to mine cobwebs/leaves etc.", s.useSwordToMine);
                add("Force Internal Mining", "Mine inward (ceiling/floor) when possible", s.forceInternalMining);
                add("Notify On Mine Fail", "Chat notification when finder gives up", s.notificationOnMineFail);
                add("Disconnect On Arrival", "Log off when target quantity is reached", s.disconnectOnArrival);
                break;

            case RENDERING:
                add("Render Path", "Draw the computed path in-world", s.renderPath);
                add("Render Path As Line", "Render path as a line instead of boxes", s.renderPathAsLine);
                add("Render Goal", "Draw the goal location in-world", s.renderGoal);
                add("Render Goal Animated", "Animate the goal marker", s.renderGoalAnimated);
                add("Render Selection Boxes", "Draw break/place/walk selection boxes", s.renderSelectionBoxes);
                add("Goal Ignore Depth", "Render goal through blocks", s.renderGoalIgnoreDepth);
                add("Goal XZ Beacon", "Render XZ goals as beacon beams", s.renderGoalXZBeacon);
                add("Selection Ignore Depth", "Render selections through blocks", s.renderSelectionBoxesIgnoreDepth);
                add("Path Ignore Depth", "Render path through blocks", s.renderPathIgnoreDepth);
                add("Fade Path", "Fade out distant path segments", s.fadePath);
                add("Cached Chunks", "Render cached chunks semi-transparent", s.renderCachedChunks);
                add("Render Selection", "Render selections", s.renderSelection);
                add("Selection Corners", "Render selection corners", s.renderSelectionCorners);
                break;

            case MISC:
                add("Chat Control", "Allow chat-based Baritone commands", s.chatControl);
                add("Chat Debug", "Print debug messages to chat", s.chatDebug);
                add("Log As Toast", "Show logs as advancement-style toasts", s.logAsToast);
                add("Prefix Control", "Enable command prefix in chat", s.prefixControl);
                add("Echo Commands", "Echo executed commands to chat", s.echoCommands);
                add("Censor Coordinates", "Hide coordinates in chat output", s.censorCoordinates);
                add("Inventory Stationary", "Only move inventory when standing still", s.inventoryMoveOnlyIfStationary);
                add("Allow Inventory", "Let bot move items in inventory", s.allowInventory);
                add("Chunk Caching", "Cache chunk data for long-range pathing", s.chunkCaching);
                add("Consider Potions", "Factor in potion effects for costs", s.considerPotionEffects);
                add("Disconnect On Arrival", "Disconnect after reaching goal", s.disconnectOnArrival);
                add("Avoidance", "Avoid mobs and spawners", s.avoidance);
                add("Desktop Notifications", "Show OS desktop notifications", s.desktopNotifications);
                add("Elytra Auto Jump", "Auto-jump to start elytra flight", s.elytraAutoJump);
                add("Elytra Auto Swap", "Auto-swap low-durability elytra", s.elytraAutoSwap);
                break;

            default:
                break;
        }
    }

    private void add(String label, String desc, Settings.Setting<Boolean> setting) {
        toggleEntries.add(new ToggleEntry(label, desc, setting));
    }

    // ======================================================================
    //  Utility draw helpers
    // ======================================================================

    private void drawText(GuiGraphics g, String text, int x, int y, int color) {
        g.drawString(this.font, text, x, y, color, false);
    }

    private void drawCenteredText(GuiGraphics g, String text, int cx, int y, int color) {
        int w = this.font.width(text);
        g.drawString(this.font, text, cx - w / 2, y, color, false);
    }

    private static String bool(Settings.Setting<Boolean> s) {
        return s.value ? "ON" : "OFF";
    }

    private void enableScissor(GuiGraphics g, int x1, int y1, int x2, int y2) {
        g.enableScissor(x1, y1, x2, y2);
    }

    private void disableScissor(GuiGraphics g) {
        g.disableScissor();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ======================================================================
    //  Static opener – can be called from chat command or keybind
    // ======================================================================

    /**
     * Open the settings GUI on the main Minecraft thread.
     */
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.setScreen(new BaritoneSettingsGui()));
    }
}
