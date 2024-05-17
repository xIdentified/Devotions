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
            Deity deity = plugin.getDevotionManager().getPlayerDevotion(player.getUniqueId()).getDeity();
            return deity != null ? "§e" + deity.getName() : "\u00A0"; // Non-breaking space
        }

        // Placeholder for the player's favor value
        if (params.equalsIgnoreCase("favor")) {
            FavorManager favorManager = plugin.getDevotionManager().getPlayerDevotion(player.getUniqueId());
            return favorManager.getFavor() + "";
        }

        // Placeholder for the top players in favor
        if (params.equalsIgnoreCase("favor_top")) {
            List<FavorManager> sortedFavorData = plugin.getDevotionManager().getSortedFavorData();
            StringBuilder topPlayers = new StringBuilder();
            for (int i = 0; i < Math.min(3, sortedFavorData.size()); i++) {
                FavorManager data = sortedFavorData.get(i);
                String playerName = Bukkit.getOfflinePlayer(data.getUuid()).getName();
                topPlayers.append("§6").append(i + 1).append(". §a").append(playerName).append(" §7- ")
                        .append(data.getFavor()).append("\n");
            }
            return topPlayers.toString().trim();
        }

        // Placeholder for the top players in favor for a specific deity
        if (params.startsWith("favor_top_")) {
            String[] parts = params.split("_");
            if (parts.length == 4) {
                String deityName = parts[2];
                int rank = Integer.parseInt(parts[3]);

                Deity deity = plugin.getDevotionManager().getDeityByName(deityName);
                if (deity == null) {
                    return "Unknown Deity";
                }

                List<FavorManager> sortedFavorData = plugin.getDevotionManager().getSortedFavorDataByDeity(deity);

                if (rank <= 0 || rank > sortedFavorData.size()) {
                    return "No Data";
                }

                FavorManager favorManager = sortedFavorData.get(rank - 1);
                String playerName = Bukkit.getOfflinePlayer(favorManager.getUuid()).getName();
                return "§a" + playerName + " §7- " + favorManager.getFavor();
            }
        }

        return null; // Placeholder is unknown
    }

}