package me.xidentified.devotions.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.rituals.Ritual;
import me.xidentified.devotions.rituals.RitualManager;
import me.xidentified.devotions.rituals.RitualObjective;
import me.xidentified.devotions.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

public class WorldGuardListener implements Listener {
    private final Devotions plugin;
    private final RitualManager ritualManager;

    public WorldGuardListener(Devotions plugin) {
        this.plugin = plugin;
        this.ritualManager = RitualManager.getInstance(plugin);
    }

    @EventHandler
    public void onPilgrimage(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Ritual currentRitual = ritualManager.getCurrentRitualForPlayer(player);
        if (currentRitual != null) {
            for (RitualObjective objective : currentRitual.getObjectives()) {
                if (objective.getType() == RitualObjective.Type.PILGRIMAGE && !objective.isComplete() && objective.isRegionTarget()) {
                    if (isPlayerInRegion(player, objective.getTarget())) {
                        handlePilgrimageCompletion(player, objective);
                    }
                }
            }
        }
    }

    private void handlePilgrimageCompletion(Player player, RitualObjective objective) {
        objective.incrementCount();
        if (objective.isComplete()) {
            Devotions.sendMessage(player, Messages.RITUAL_RETURN_TO_RESUME);
        } else {
            Devotions.sendMessage(player, Messages.RITUAL_PROGRESS
                    .insertParsed("current", String.valueOf(objective.getCurrentCount()))
                    .insertParsed("total", String.valueOf(objective.getCount()))
                    .insertParsed("objective", objective.getDescription()));
        }
    }

    private boolean isPlayerInRegion(Player player, String regionName) {
        WorldGuardPlugin wgPlugin = getWorldGuardPlugin();
        if (wgPlugin == null) {
            plugin.getLogger().warning("WorldGuard plugin not found!");
            return false;
        }

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
        if (regions != null) {
            ApplicableRegionSet set = regions.getApplicableRegions(
                    BlockVector3.at(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()));
            for (ProtectedRegion region : set) {
                if (region.getId().equalsIgnoreCase(regionName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private WorldGuardPlugin getWorldGuardPlugin() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        if (!(plugin instanceof WorldGuardPlugin)) {
            return null;
        }
        return (WorldGuardPlugin) plugin;
    }
}
