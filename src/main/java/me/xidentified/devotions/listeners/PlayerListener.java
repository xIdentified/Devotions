package me.xidentified.devotions.listeners;

import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.managers.DevotionManager;
import me.xidentified.devotions.managers.FavorManager;
import me.xidentified.devotions.storage.StorageManager;
import me.xidentified.devotions.storage.model.DevotionData;
import me.xidentified.devotions.storage.model.IStorage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerListener implements Listener {
    private final Devotions plugin;
    private final StorageManager storageManager;

    public PlayerListener(Devotions plugin, StorageManager storageManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUniqueId = event.getPlayer().getUniqueId();
        DevotionManager devotionManager = plugin.getDevotionManager();
        IStorage storage = storageManager.getStorage(); // Get the storage instance from the StorageManager

        // Check if player already has a FavorManager instance
        FavorManager favorManager = devotionManager.getPlayerDevotion(playerUniqueId);

        if (favorManager == null) {
            DevotionData devotionData = storage.getPlayerDevotion(playerUniqueId); // Use the storage to get player devotion data

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

    // Save player devotion when they leave the game
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        UUID playerUniqueId = event.getPlayer().getUniqueId();
        FavorManager favorManager = plugin.getDevotionManager().getPlayerDevotion(playerUniqueId);
        if (favorManager != null) {
            storageManager.getStorage().savePlayerDevotion(playerUniqueId, favorManager);
        }
    }
}
