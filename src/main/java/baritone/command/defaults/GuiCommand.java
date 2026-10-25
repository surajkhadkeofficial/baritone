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

package baritone.command.defaults;

import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.utils.gui.BaritoneSettingsGui;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Opens the Baritone Settings GUI with module toggles and anti-cheat presets.
 */
public class GuiCommand extends Command {

    public GuiCommand(IBaritone baritone) {
        super(baritone, "gui", "settings", "menu");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(0);
        BaritoneSettingsGui.open();
        logDirect("Opening Baritone Settings GUI...");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Open Baritone settings GUI";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Opens the Baritone Settings GUI where you can:",
                "  - Apply anti-cheat presets (NCP, AAC, Vulcan, Grim, etc.)",
                "  - Toggle movement settings (sprint, parkour, etc.)",
                "  - Configure mining behavior (legit mine, auto tool, etc.)",
                "  - Adjust rendering options (path, goal, selections)",
                "  - Manage miscellaneous settings (chat, inventory, elytra)",
                "",
                "Usage:",
                "> gui",
                "> settings",
                "> menu"
        );
    }
}
