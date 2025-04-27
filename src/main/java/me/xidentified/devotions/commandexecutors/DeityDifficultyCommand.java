package me.xidentified.devotions.commandexecutors;

import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DeityDifficultyCommand implements CommandExecutor, TabCompleter {

    private final Devotions plugin;

    public DeityDifficultyCommand(Devotions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Devotions.sendMessage(sender, Messages.GENERAL_PLAYER_NOT_FOUND);
            return true;
        }

        if (!player.hasPermission("devotions.admin")) {
            // Use a message from our own Messages class instead of GlobalMessages
            Devotions.sendMessage(player, Messages.NO_PERM_CMD);
            return true;
        }

        if (args.length < 1) {
            Devotions.sendMessage(player, Messages.DEITY_CMD_USAGE);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        if (subCommand.equals("list")) {
            return listDeityDifficulties(player);
        } else if (subCommand.equals("info") && args.length > 1) {
            return showDeityDifficultyInfo(player, args[1]);
        }
        
        Devotions.sendMessage(player, Messages.DEITY_CMD_USAGE);
        return true;
    }

    private boolean listDeityDifficulties(Player player) {
        List<Deity> deities = plugin.getDevotionManager().getAllDeities();
        
        if (deities.isEmpty()) {
            Devotions.sendMessage(player, Messages.DEITY_NO_DEITY_FOUND);
            return true;
        }
        
        player.sendMessage("§6=== Deity Difficulty Levels ===");
        
        for (Deity deity : deities) {
            String difficultyLevel = getDifficultyLevel(deity);
            String personalityInfo = "§7Personality: §f" + deity.getPersonality();
            
            player.sendMessage("§e" + deity.getName() + " §7- " + difficultyLevel + " §7- " + personalityInfo);
        }
        
        return true;
    }
    
    private boolean showDeityDifficultyInfo(Player player, String deityName) {
        Deity deity = plugin.getDevotionManager().getDeityByInput(deityName);
        
        if (deity == null) {
            Devotions.sendMessage(player, Messages.DEITY_NOT_FOUND);
            return true;
        }
        
        String difficultyLevel = getDifficultyLevel(deity);
        
        player.sendMessage("§6=== " + deity.getName() + " Difficulty Info ===");
        player.sendMessage("§7Difficulty: §f" + difficultyLevel);
        player.sendMessage("§7Personality: §f" + deity.getPersonality());
        player.sendMessage("§7Initial Favor: §f" + deity.getInitialFavor());
        player.sendMessage("§7Max Favor: §f" + deity.getMaxFavor());
        player.sendMessage("§7Favor Decay Rate: §f" + deity.getFavorDecayRate());
        player.sendMessage("§7Blessing Threshold: §f" + deity.getBlessingThreshold());
        player.sendMessage("§7Curse Threshold: §f" + deity.getCurseThreshold());
        player.sendMessage("§7Miracle Threshold: §f" + deity.getMiracleThreshold());
        player.sendMessage("§7Blessing Chance: §f" + deity.getBlessingChance());
        player.sendMessage("§7Curse Chance: §f" + deity.getCurseChance());
        player.sendMessage("§7Miracle Chance: §f" + deity.getMiracleChance());
        
        return true;
    }
    
    private String getDifficultyLevel(Deity deity) {
        // Calculate difficulty based on various factors
        int difficultyScore = 0;
        
        // Higher thresholds = harder
        difficultyScore += (deity.getBlessingThreshold() - 150) / 10;
        difficultyScore += (deity.getMiracleThreshold() - 210) / 10;
        
        // Lower curse threshold = harder
        difficultyScore += (35 - deity.getCurseThreshold()) / 5;
        
        // Higher decay rate = harder
        difficultyScore += (deity.getFavorDecayRate() - 5) * 2;
        
        // Lower initial favor = harder
        difficultyScore += (50 - deity.getInitialFavor()) / 5;
        
        // Personality factor
        if (deity.getPersonality().equalsIgnoreCase("vengeful")) {
            difficultyScore += 5;
        } else if (deity.getPersonality().equalsIgnoreCase("demanding")) {
            difficultyScore += 3;
        } else if (deity.getPersonality().equalsIgnoreCase("generous")) {
            difficultyScore -= 3;
        } else if (deity.getPersonality().equalsIgnoreCase("forgiving")) {
            difficultyScore -= 5;
        }
        
        // Determine difficulty level
        if (difficultyScore > 10) {
            return "§4Hard";
        } else if (difficultyScore > 0) {
            return "§6Medium";
        } else if (difficultyScore > -10) {
            return "§2Easy";
        } else {
            return "§aVery Easy";
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("list", "info"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            completions.addAll(plugin.getDevotionManager().getAllDeities().stream()
                    .map(Deity::getName)
                    .collect(Collectors.toList()));
        }
        
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
