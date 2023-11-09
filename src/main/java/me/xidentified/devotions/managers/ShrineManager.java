package me.xidentified.devotions.managers;

import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.Shrine;
import me.xidentified.devotions.storage.ShrineStorage;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShrineManager {
    private final Devotions plugin;
    private ShrineStorage shrineStorage;
    private final List<Shrine> allShrinesList;
    private final Map<Location, Shrine> shrines = new HashMap<>();

    public ShrineManager(Devotions plugin) {
        this.plugin = plugin;
        this.allShrinesList = new ArrayList<>();
        this.shrineStorage = new ShrineStorage(plugin.getStorageManager());  // Initialize the ShrineStorage
    }


    public void addShrine(Shrine newShrine) {
        if (newShrine != null) {
            if (getShrineAtLocation(newShrine.getLocation()) != null) {
                // Shrine already exists at the location, don't add a new one
                return;
            }

            this.allShrinesList.add(newShrine);
            this.shrines.put(newShrine.getLocation(), newShrine);
            shrineStorage.saveShrine(newShrine);
            plugin.debugLog("New shrine added for deity: " + newShrine.getDeity().getName() + " at " + newShrine.getLocation());
        }
    }

    public boolean removeShrine(Player player, Location location) {
        Shrine shrine = shrines.get(location);
        if (shrine != null && shrine.getOwner().equals(player)) {
            shrines.remove(location);
            allShrinesList.remove(shrine);
            shrineStorage.removeShrine(location);
            plugin.debugLog("Shrine removed for deity " + shrine.getDeity().getName() + " at " + shrine.getLocation());
            return true;  // Successfully removed
        }
        return false;  // Shrine not found or not owned by the player
    }

    public List<Shrine> getAllShrines() {
        return this.allShrinesList;
    }

    public Shrine getShrineAtLocation(Location location) {
        for (Shrine shrine : allShrinesList) {
            Location shrineLocation = shrine.getLocation();
            if (shrineLocation.equals(location)) {
                plugin.debugLog("Shrine found at location: " + shrineLocation);
                return shrine;
            }
        }
        return null;
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
            Player owner = shrine.getOwner();
            if (owner != null && owner.equals(player)) {
                count++;
            }
        }
        return count;
    }


    public Devotions getPlugin() {
        return plugin;
    }

    public void setShrineStorage(ShrineStorage newStorage) {
        this.shrineStorage = newStorage;
        // Clear the existing list to avoid duplicates
        this.allShrinesList.clear();
        // Load all shrines using the newly set ShrineStorage
        this.allShrinesList.addAll(shrineStorage.loadAllShrines(this, plugin.getDevotionManager()));
    }

}
