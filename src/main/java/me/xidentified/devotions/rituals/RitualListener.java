package me.xidentified.devotions.rituals;

import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.managers.MeditationManager;
import me.xidentified.devotions.managers.ShrineManager;
import me.xidentified.devotions.util.Messages;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class RitualListener implements Listener {

    private final Devotions plugin;
    private final RitualManager ritualManager;
    private final ShrineManager shrineManager;
    private static RitualListener instance;

    public RitualListener(Devotions plugin, ShrineManager shrineManager) {
        this.plugin = plugin;
        this.ritualManager = RitualManager.getInstance(plugin);
        this.shrineManager = shrineManager;
    }

    public static void initialize(Devotions plugin, ShrineManager shrineManager) {
        if (instance == null) {
            instance = new RitualListener(plugin, shrineManager);
        }
    }

    public static RitualListener getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RitualListener has not been initialized. Call initialize() first.");
        }
        return instance;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Ritual ritual = ritualManager.getCurrentRitualForPlayer(player);

        if (ritual == null || ritual.isCompleted()) {
            return;
        }

        if (meditationManager().isPlayerInMeditation(player)) {
            MeditationData meditationData = meditationManager().getMeditationData(player);
            if (meditationData != null && hasPlayerMoved(event)) {
                plugin.debugLog("Player " + player.getName() + " moved during meditation.");
                meditationManager().applyMeditationPenalties(player, meditationData);
                return;
            }
        }

        for (RitualObjective objective : ritual.getObjectives()) {
            if (objective.getType() == RitualObjective.Type.GATHERING && !objective.isComplete()) {
                int itemCount = countItemsInInventory(player.getInventory(), Material.valueOf(objective.getTarget()));
                if (itemCount >= objective.getCount()) {
                    objective.setCurrentCount(itemCount);
                }
            }
        }

        if (isPlayerNearShrine(player) && allObjectivesCompleted(ritual)) {
            plugin.debugLog("Player " + player.getName() + " returned to the shrine with all objectives completed.");
            plugin.getRitualManager().completeRitual(player, ritual, meditationManager());
        }
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        Player player = (Player) event.getViewers().get(0);
        ItemStack result = event.getInventory().getResult();
        if (result != null) {
            Ritual currentRitual = ritualManager.getCurrentRitualForPlayer(player);
            handleRitualObjective(currentRitual, player, RitualObjective.Type.CRAFTING, result.getType().toString());
        }
    }

    @EventHandler
    public void onEntityBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player player) {
            Ritual currentRitual = ritualManager.getCurrentRitualForPlayer(player);
            handleRitualObjective(currentRitual, player, RitualObjective.Type.BREEDING, event.getEntity().getType().toString());
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player != null) {
            Entity entity = event.getEntity();
            Ritual currentRitual = ritualManager.getCurrentRitualForPlayer(player);
            handleRitualObjective(currentRitual, player, RitualObjective.Type.SACRIFICE, entity.getType().toString());
            handleRitualObjective(currentRitual, player, RitualObjective.Type.PURIFICATION, entity.getType().toString());
        }
    }

    @EventHandler
    public void onPilgrimage(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        RitualManager ritualManager = RitualManager.getInstance(plugin);
        Ritual currentRitual = ritualManager.getCurrentRitualForPlayer(player);
        if (currentRitual != null) {
            for (RitualObjective objective : currentRitual.getObjectives()) {
                if (objective.getType() == RitualObjective.Type.PILGRIMAGE) {
                    String[] coords = objective.getTarget().split(",");
                    int targetX = Integer.parseInt(coords[0]);
                    int targetY = Integer.parseInt(coords[1]);
                    int targetZ = Integer.parseInt(coords[2]);
                    Location targetLocation = new Location(player.getWorld(), targetX, targetY, targetZ);

                    if (to.getBlockX() == targetLocation.getBlockX() && to.getBlockY() == targetLocation.getBlockY()
                            && to.getBlockZ() == targetLocation.getBlockZ()) {
                        objective.incrementCount();
                        if (objective.isComplete()) {
                            Devotions.sendMessage(player, Messages.RITUAL_RETURN_TO_RESUME);
                            break;
                        } else {
                            Devotions.sendMessage(player, Messages.RITUAL_PROGRESS
                                    .insertParsed("current", String.valueOf(objective.getCurrentCount()))
                                    .insertParsed("total", String.valueOf(objective.getCount()))
                                    .insertParsed("objective", objective.getDescription()));
                        }
                    }
                }
            }
        }
    }

    private void handleRitualObjective(Ritual ritual, Player player, RitualObjective.Type type, String target) {
        if (ritual == null || ritual.isCompleted()) {
            return;
        }

        for (RitualObjective objective : ritual.getObjectives()) {
            if (objective.getType() == type && objective.getTarget().equalsIgnoreCase(target)) {
                objective.incrementCount();
                if (objective.isComplete()) {
                    if (allObjectivesCompleted(ritual)) {
                        Devotions.sendMessage(player, Messages.RITUAL_RETURN_TO_RESUME);
                    } else {
                        Devotions.sendMessage(player, Messages.RITUAL_PROGRESS
                                .insertParsed("current", String.valueOf(objective.getCurrentCount()))
                                .insertParsed("total", String.valueOf(objective.getCount()))
                                .insertParsed("objective", objective.getDescription()));
                    }
                }
                break;
            }
        }
    }

    private int countItemsInInventory(Inventory inventory, Material material) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private boolean hasPlayerMoved(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        return to != null && (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ());
    }

    private boolean isPlayerNearShrine(Player player) {
        Location shrineLocation = shrineManager.getShrineLocationForPlayer(player);
        if (shrineLocation != null && player.getWorld().equals(shrineLocation.getWorld())) {
            double distance = player.getLocation().distance(shrineLocation);
            return distance <= 5;
        }
        return false;
    }

    private boolean allObjectivesCompleted(Ritual ritual) {
        return ritual.getObjectives().stream().allMatch(RitualObjective::isComplete);
    }

    private MeditationManager meditationManager() {
        return plugin.getMeditationManager();
    }
}
