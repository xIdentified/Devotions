package me.xidentified.devotions.commandexecutors;

import de.cubbossa.tinytranslations.GlobalMessages;
import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.Shrine;
import me.xidentified.devotions.managers.FavorManager;
import me.xidentified.devotions.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MigrationCommand implements CommandExecutor {
    private final Devotions plugin;

    public MigrationCommand(Devotions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("devotions.admin")) {
            plugin.sendMessage(sender, GlobalMessages.NO_PERM_CMD);
            return true;
        }

        // Read data from YAML files
        YamlConfiguration playerdataConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "storage/playerdata.yml"));
        YamlConfiguration shrinesConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "storage/shrines.yml"));

        // Migrate player data
        migratePlayerData(playerdataConfig);

        // Migrate shrine data
        migrateShrineData(shrinesConfig);

        // Delete YAML files
        // deleteYamlFiles();

        plugin.sendMessage(sender, Messages.MIGRATION_COMPLETE);
        return true;
    }

    public void migratePlayerData(YamlConfiguration playerdataConfig) {
        Set<UUID> migratedPlayers = new HashSet<>();
        for (String playerUUIDString : playerdataConfig.getKeys(false)) {
            if (!playerUUIDString.equalsIgnoreCase("playerdata")) {
                try {
                    UUID playerUUID = UUID.fromString(playerUUIDString);
                    if (migratedPlayers.contains(playerUUID)) {
                        continue; // Skip if player data is already migrated
                    }
                    migratedPlayers.add(playerUUID);

                    // Read player data from YAML
                    String deityName = playerdataConfig.getString(playerUUIDString + ".deity");

                    // Save player data to new db
                    FavorManager favorManager = new FavorManager(plugin, playerUUID, plugin.getDevotionManager().getDeityByName(deityName));
                    plugin.getStorageManager().getStorage().savePlayerDevotion(playerUUID, favorManager);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID string found in playerdata.yml: " + playerUUIDString);
                }
            }
        }
    }

    public void migrateShrineData(YamlConfiguration shrinesConfig) {
        for (String locationStr : shrinesConfig.getKeys(false)) {
            // Read shrine data from YAML
            String[] parts = locationStr.split(":", 2); // Limit split to 2 parts
            if (parts.length != 2) {
                plugin.debugLog("Invalid shrine data: " + locationStr);
                continue;
            }
            String[] locationParts = parts[0].split(",");
            String world = locationParts[0];
            int x = Integer.parseInt(locationParts[1]);
            int y = Integer.parseInt(locationParts[2]);
            int z = Integer.parseInt(locationParts[3]);
            String[] secondPartParts = parts[1].split(",", 2);
            if (secondPartParts.length != 2) {
                plugin.debugLog("Invalid shrine data: " + locationStr);
                continue;
            }
            String deityName = secondPartParts[1];
            UUID ownerUUID = UUID.fromString(secondPartParts[0]);

            // Save shrine data to new storage
            Location location = new Location(Bukkit.getWorld(world), x, y, z);
            Deity deity = plugin.getDevotionManager().getDeityByName(deityName);
            if (deity != null) {
                Shrine shrine = new Shrine(location, deity, ownerUUID);
                plugin.getStorageManager().getStorage().saveShrine(shrine);
            } else {
                plugin.debugLog("Deity not found: " + deityName);
            }
        }
    }

    private void deleteYamlFiles() {
        // Delete the YAML files
        File playerdataFile = new File(plugin.getDataFolder(), "storage/playerdata.yml");
        File shrinesFile = new File(plugin.getDataFolder(), "storage/shrines.yml");
        playerdataFile.delete();
        shrinesFile.delete();
    }
}
