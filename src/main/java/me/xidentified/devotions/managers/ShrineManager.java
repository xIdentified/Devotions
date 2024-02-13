package me.xidentified.devotions.managers;

import lombok.Getter;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.Shrine;
import me.xidentified.devotions.storage.model.IStorage;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShrineManager {
    @Getter private final Devotions plugin;
    private IStorage storage;
    private final List<Shrine> allShrinesList;
    private final Map<Location, Shrine> shrines = new ConcurrentHashMap<>();

    public ShrineManager(Devotions plugin) {
        this.plugin = plugin;
        this.allShrinesList = new ArrayList<>();
        this.storage = plugin.getStorageManager().getStorage();
    }

    public void addShrine(Shrine newShrine) {
        if (newShrine != null) {
            if (getShrineAtLocation(newShrine.getLocation()) != null) {
                // Shrine already exists at the location, don't add a new one
                return;
            }

            this.allShrinesList.add(newShrine);
            this.shrines.put(newShrine.getLocation(), newShrine);
            storage.saveShrine(newShrine);
            plugin.debugLog("New shrine added for " + newShrine.getDeity().getName() + " at " + newShrine.getLocation());
        }
    }

    public boolean removeShrine(UUID playerId, Location location) {
        Shrine shrine = shrines.get(location);

        if (shrine == null) {
            plugin.debugLog("No shrine found at: " + location);
            return false;
        }

        plugin.debugLog("Attempting to remove shrine at: " + location + " owned by " + shrine.getOwner());
        if (!shrine.getOwner().equals(playerId)) {
            plugin.debugLog("Shrine ownership mismatch: " + playerId + " vs " + shrine.getOwner());
            return false;
        }

        shrines.remove(location);
        allShrinesList.remove(shrine);
        storage.removeShrine(location, playerId);
        plugin.debugLog("Shrine removed for " + shrine.getDeity().getName() + " at " + shrine.getLocation());
        return true;
    }
    public List<Shrine> getAllShrines() {
        return this.allShrinesList;
    }

    public Shrine getShrineAtLocation(Location location) {
        for (Shrine shrine : allShrinesList) {
            Location shrineLocation = shrine.getLocation();
            if (isSameBlockLocation(shrineLocation, location)) {
                plugin.debugLog("Shrine found at location: " + shrineLocation);
                return shrine;
            }
        }
        return null;
    }

    private boolean isSameBlockLocation(Location loc1, Location loc2) {
        return loc1.getWorld().equals(loc2.getWorld()) &&
                loc1.getBlockX() == loc2.getBlockX() &&
                loc1.getBlockY() == loc2.getBlockY() &&
                loc1.getBlockZ() == loc2.getBlockZ();
    }

    public Location getShrineLocationForPlayer(Player player) {
        for (Location shrineLoc : shrines.keySet()) {
            if (shrineLoc.getWorld().equals(player.getWorld()) && shrineLoc.distance(player.getLocation()) <= 5) { // Assuming 5 blocks is the radius
                return shrineLoc;
            }
        }
        return null;
    }

    public int getShrineCount(Player player) {
        int count = 0;
        for (Shrine shrine : allShrinesList) {
            UUID owner = shrine.getOwner();
            if (owner != null && owner.equals(player.getUniqueId())) {
                count++;
            }
        }
        return count;
    }


    public void setStorage(IStorage newStorage) {
        this.storage = newStorage;
        // Clear the existing lists and maps to avoid duplicates
        this.allShrinesList.clear();
        this.shrines.clear();
        // Load all shrines using the newly set storage
        List<Shrine> loadedShrines = storage.loadAllShrines(plugin.getDevotionManager());
        this.allShrinesList.addAll(loadedShrines);
        // Also update the shrines map
        for (Shrine shrine : loadedShrines) {
            this.shrines.put(shrine.getLocation(), shrine);
        }
    }

}
