package me.xidentified.devotions.rituals;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class RitualOutcome {

    private final String type;
    private final List<String> commands;

    // I was going to have built in outcomes at first, but I realized almost all of them can be done with commands.
    // I might add more dynamic outcome types again later so let's keep things open-ended

    public RitualOutcome(String type, List<String> commands) {
        this.type = type;
        this.commands = commands;
        validateOutcome(); // Validate outcome data upon object creation
    }

    private void validateOutcome() {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("RitualOutcome type cannot be null or empty");
        }
        if (!"RUN_COMMAND".equals(type)) {
            throw new IllegalArgumentException("Currently, only 'RUN_COMMAND' type is supported");
        }
        if (commands == null || commands.isEmpty()) {
            throw new IllegalArgumentException("Commands list cannot be null or empty for 'RUN_COMMAND' type");
        }
    }

    public void executeOutcome(Player player) {
        if ("RUN_COMMAND".equals(type)) {
            for (String command : commands) {
                String processedCommand = command.replace("{player}", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
            }
        }
    }
}
