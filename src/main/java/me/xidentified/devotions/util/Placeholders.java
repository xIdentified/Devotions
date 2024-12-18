package me.xidentified.devotions.util;

import java.util.List;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.managers.FavorManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class Placeholders extends PlaceholderExpansion {

    private final Devotions plugin;

    public Placeholders(Devotions plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getAuthor() {
        return "xIdentified";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "devotions";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        // Placeholder for the player's current deity
        if (params.equalsIgnoreCase("deity")) {
            FavorManager favorManager = plugin.getDevotionManager().getPlayerDevotion(player.getUniqueId());
            if (favorManager == null) {
                return "None";
            }
            Deity deity = favorManager.getDeity();
            return deity != null ? "§e" + deity.getName() : "\u00A0"; // Non-breaking space
        }

        // Placeholder for deity's configured chat icon
        if (params.equalsIgnoreCase("icon")) {
            FavorManager favorManager = plugin.getDevotionManager().getPlayerDevotion(player.getUniqueId());
            if (favorManager == null) {
                return plugin.getDevotionsConfig().getDeityIcon("None");
            }
            Deity deity = favorManager.getDeity();
            String deityName = (deity != null) ? deity.getName() : "None";
            return plugin.getDevotionsConfig().getDeityIcon(deityName);
        }

        // Placeholder for the player's favor value
        if (params.equalsIgnoreCase("favor")) {
            FavorManager favorManager = plugin.getDevotionManager().getPlayerDevotion(player.getUniqueId());
            if (favorManager == null) {
                return "0";
            }
            return String.valueOf(favorManager.getFavor());
        }

        // Placeholder for the top players in favor
        if (params.equalsIgnoreCase("favor_top")) {
            List<FavorManager> sortedFavorData = plugin.getDevotionManager().getSortedFavorData();
            StringBuilder topPlayers = new StringBuilder();

            for (int i = 0; i < Math.min(3, sortedFavorData.size()); i++) {
                FavorManager data = sortedFavorData.get(i);
                String playerName = Bukkit.getOfflinePlayer(data.getUuid()).getName();
                playerName = (playerName != null) ? playerName : "Unknown";
                topPlayers.append("§6").append(i + 1).append(". §a").append(playerName).append(" §7- ")
                        .append(data.getFavor()).append("\n");
            }
            return topPlayers.toString().trim();
        }

        // Placeholder for the top players in favor for a specific deity
        if (params.startsWith("favor_top_")) {
            try {
                String[] parts = params.split("_");
                if (parts.length == 4) {
                    String deityName = parts[2];
                    int rank;

                    // Parse rank
                    try {
                        rank = Integer.parseInt(parts[3]);
                    } catch (NumberFormatException e) {
                        return "Invalid Integer";
                    }

                    // Validate deity
                    Deity deity = plugin.getDevotionManager().getDeityByName(deityName);
                    if (deity == null) {
                        return "Unknown Deity";
                    }

                    // Retrieve sorted data
                    List<FavorManager> sortedFavorData = plugin.getDevotionManager().getSortedFavorDataByDeity(deity);
                    if (rank <= 0 || rank > sortedFavorData.size()) {
                        return "No Data";
                    }

                    // Show player info
                    FavorManager favorManager = sortedFavorData.get(rank - 1);
                    String playerName = Bukkit.getOfflinePlayer(favorManager.getUuid()).getName();
                    playerName = (playerName != null) ? playerName : "Unknown";
                    return "§a" + playerName + " §7- " + favorManager.getFavor();
                }
            } catch (Exception e) {
                return "Error Processing Placeholder";
            }
        }

        return null; // Placeholder is unknown
    }

}