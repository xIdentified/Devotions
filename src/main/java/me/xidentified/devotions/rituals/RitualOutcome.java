package me.xidentified.devotions.rituals;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public record RitualOutcome(String type, String command) {
    public RitualOutcome(String type, String command) {
        this.type = type;
        this.command = command;
        validateOutcome(); // Validate outcome data upon object creation
    }

    private void validateOutcome() {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("RitualOutcome type cannot be null or empty");
        }
        if (!"RUN_COMMAND".equals(type)) {
            throw new IllegalArgumentException("Currently, only 'RUN_COMMAND' type is supported");
        }
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("Command cannot be null or empty for 'RUN_COMMAND' type");
        }
    }

    public void executeOutcome(Player player) {
        if ("RUN_COMMAND".equals(type())) {
            String processedCommand = command.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
        }
    }
}
