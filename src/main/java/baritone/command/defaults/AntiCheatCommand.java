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
import baritone.api.command.exception.CommandInvalidTypeException;
import baritone.utils.anticheat.AntiCheatConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Command to apply anti-cheat presets from chat.
 * <p>
 * Usage:
 * <pre>
 *   #anticheat          – show current preset
 *   #anticheat safe     – apply the SAFE preset
 *   #anticheat grim     – apply the GRIM preset
 *   #anticheat list     – list all presets
 * </pre>
 */
public class AntiCheatCommand extends Command {

    public AntiCheatCommand(IBaritone baritone) {
        super(baritone, "anticheat", "ac");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            // Show current preset
            AntiCheatConfig.Preset current = AntiCheatConfig.getActivePreset();
            logDirect("Current anti-cheat preset: §a" + current.name());
            logDirect("§7" + AntiCheatConfig.getPresetDescription(current));
            logDirect("Use §b#anticheat <preset>§r to change. Use §b#anticheat list§r to see all.");
            return;
        }

        String arg = args.getString().toUpperCase(Locale.ROOT);

        if (arg.equals("LIST")) {
            logDirect("§6Available anti-cheat presets:");
            for (AntiCheatConfig.Preset preset : AntiCheatConfig.Preset.values()) {
                boolean active = preset == AntiCheatConfig.getActivePreset();
                String marker = active ? " §a✔" : "";
                logDirect("  §b" + preset.name() + marker + " §7- " + AntiCheatConfig.getPresetDescription(preset));
            }
            return;
        }

        try {
            AntiCheatConfig.Preset preset = AntiCheatConfig.Preset.valueOf(arg);
            AntiCheatConfig.applyPreset(preset);
            logDirect("§aApplied anti-cheat preset: §e" + preset.name());
            logDirect("§7" + AntiCheatConfig.getPresetDescription(preset));
        } catch (IllegalArgumentException e) {
            logDirect("§cUnknown preset: §e" + arg);
            logDirect("Use §b#anticheat list§r to see all available presets.");
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        if (args.hasExactlyOne()) {
            String typed = args.peekString().toUpperCase(Locale.ROOT);
            return Stream.concat(
                    Stream.of("list"),
                    Arrays.stream(AntiCheatConfig.Preset.values()).map(p -> p.name().toLowerCase(Locale.ROOT))
            ).filter(s -> s.toUpperCase(Locale.ROOT).startsWith(typed));
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Apply anti-cheat configuration presets";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Apply pre-configured anti-cheat profiles that tune all of Baritone's",
                "settings to avoid detection by specific anti-cheat plugins.",
                "",
                "Presets: NONE, SAFE, NCP, AAC, VULCAN, GRIM, MATRIX, PARANOID",
                "",
                "Usage:",
                "> anticheat         - show current preset",
                "> anticheat safe    - apply SAFE preset",
                "> anticheat grim    - apply GRIM preset",
                "> anticheat list    - list all presets"
        );
    }
}
