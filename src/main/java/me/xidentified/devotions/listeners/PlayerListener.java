package me.xidentified.devotions.listeners;

import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.managers.DevotionManager;
import me.xidentified.devotions.managers.FavorManager;
import me.xidentified.devotions.storage.DevotionData;
import me.xidentified.devotions.storage.DevotionStorage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerListener implements Listener {
    private final Devotions plugin;

    public PlayerListener(Devotions plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUniqueId = event.getPlayer().getUniqueId();
        DevotionManager devotionManager = plugin.getDevotionManager();
        DevotionStorage devotionStorage = plugin.getDevotionStorage();

        // Check if player already has a FavorManager instance
        FavorManager favorManager = devotionManager.getPlayerDevotion(playerUniqueId);

        if (favorManager == null) {
            DevotionData devotionData = devotionStorage.getPlayerDevotion(playerUniqueId);

            if (devotionData != null) {
                Deity deity = devotionManager.getDeityByName(devotionData.getDeityName());
                if (deity != null) {
                    favorManager = new FavorManager(plugin, playerUniqueId, deity);
                    favorManager.setFavor(devotionData.getFavor());
                    devotionManager.setPlayerDevotion(playerUniqueId, favorManager);
                } else {
                    plugin.debugLog("Player '" + event.getPlayer().getName() + " does not have devotion set: " + devotionData.getDeityName());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        UUID playerUniqueId = event.getPlayer().getUniqueId();
        FavorManager favorManager = plugin.getDevotionManager().getPlayerDevotion(playerUniqueId);
        if (favorManager != null) {
            plugin.getDevotionStorage().savePlayerDevotion(playerUniqueId, favorManager);
        }
    }

}
