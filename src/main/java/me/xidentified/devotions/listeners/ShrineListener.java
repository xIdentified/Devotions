package me.xidentified.devotions.listeners;

import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.Offering;
import me.xidentified.devotions.Shrine;
import me.xidentified.devotions.managers.*;
import me.xidentified.devotions.rituals.Ritual;
import me.xidentified.devotions.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

// Begins rituals or player's offerings
public class ShrineListener implements Listener {
    private final Devotions plugin;
    private final DevotionManager devotionManager;
    private final ShrineManager shrineManager;
    private final CooldownManager cooldownManager;


    public ShrineListener(Devotions plugin, ShrineManager shrineManager, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.shrineManager = shrineManager;
        this.devotionManager = plugin.getDevotionManager();
        this.cooldownManager = cooldownManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onShrineInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Shrine shrine = shrineManager.getShrineAtLocation(clickedBlock.getLocation());
        if (shrine == null) return;

        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // Check for ritual initiation
        Ritual ritual = RitualManager.getInstance(plugin).getRitualByItem(itemInHand);
        if (ritual != null) {
            long remainingCooldown = cooldownManager.isActionAllowed(player, "ritual");
            if (remainingCooldown > 0) {
                player.sendMessage(MessageUtils.parse("<red>You must wait " + cooldownManager.formatCooldownTime(remainingCooldown) + " before performing another ritual."));
                return;
            }
            try {
                Item droppedItem = dropItemOnShrine(clickedBlock, itemInHand);
                handleRitualInteraction(player, itemInHand, droppedItem, event);
            } catch (Exception e) {
                plugin.getLogger().severe("Error while handling ritual interaction: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Check if the player is holding a valid offering
        else if (!itemInHand.getType().equals(Material.AIR)) {
            long remainingCooldown = cooldownManager.isActionAllowed(player, "offering");
            if (remainingCooldown > 0) {
                player.sendMessage(MessageUtils.parse("<red>You must wait " + cooldownManager.formatCooldownTime(remainingCooldown) + " before making another offering!"));
                return;
            }
            try {
                Item droppedItem = dropItemOnShrine(clickedBlock, itemInHand);
                handleOfferingInteraction(player, clickedBlock, itemInHand, droppedItem, shrine);
            } catch (Exception e) {
                plugin.getLogger().severe("Error while handling offering interaction: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private Item dropItemOnShrine(Block clickedBlock, ItemStack itemInHand) {
        plugin.debugLog("Attempting to place item on shrine.");

        if (!itemInHand.getType().equals(Material.AIR)) {
            Location dropLocation = clickedBlock.getLocation().add(0.5, 1, 0.5);

            // Create a new ItemStack with only 1 quantity
            ItemStack singleItemStack = new ItemStack(itemInHand.getType(), 1);

            Item droppedItem = clickedBlock.getWorld().dropItem(dropLocation, singleItemStack);

            // Set item properties
            droppedItem.setInvulnerable(true);
            droppedItem.setPickupDelay(Integer.MAX_VALUE);
            droppedItem.setGravity(false);
            // Set velocity to zero to prevent it from shooting into the air
            droppedItem.setVelocity(new Vector(0, 0, 0));

            plugin.debugLog("Dropped item spawned at: " + dropLocation + " with item " + singleItemStack.getType());
            return droppedItem;
        }
        plugin.debugLog("Dropped item not spawned.");
        return null;
    }


    private void handleRitualInteraction(Player player, ItemStack itemInHand, Item droppedItem, PlayerInteractEvent event) {
        boolean ritualStarted = RitualManager.getInstance(plugin).startRitual(player, itemInHand, droppedItem);
        if (ritualStarted) {
            long ritualCooldown = cooldownManager.getCooldownFromConfig("ritual-cooldown", "5s");
            cooldownManager.setCooldown(player, "ritual", ritualCooldown);
        } else if (droppedItem != null) {
            droppedItem.remove();  // Remove dropped item from shrine if ritual did not start
        }
        event.setCancelled(true);
    }

    private void handleOfferingInteraction(Player player, Block clickedBlock, ItemStack itemInHand, Item droppedItem, Shrine shrine) {
        Offering offering = getOfferingForItem(itemInHand, shrine.getDeity());
        FavorManager favorManager = devotionManager.getPlayerDevotion(player.getUniqueId());

        if (offering != null) {
            long offeringCooldown = cooldownManager.getCooldownFromConfig("offering-cooldown", "5s");
            cooldownManager.setCooldown(player, "offering", offeringCooldown);

            if (favorManager != null) {
                takeItemInHand(player, itemInHand);
                sendMessage(player, "<green>Your offering has been accepted!");

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    favorManager.increaseFavor(offering.getValue());
                    if (droppedItem != null) droppedItem.remove();
                    plugin.playConfiguredSound(player, "offeringAccepted");
                    spawnLocalizedParticles(clickedBlock.getLocation().add(0.5, 1, 0.5), Particle.SPELL_WITCH, 50);
                }, 100L);
            }
        } else {
            sendMessage(player, "<red>Your offering was not accepted by " + favorManager.getDeity().getName() + ".");
            if (droppedItem != null) droppedItem.remove();
        }
    }

    private void spawnLocalizedParticles(Location location, Particle particle, int count) {
        location.getWorld().spawnParticle(particle, location, count, 0.5, 0.5, 0.5, 0.5);
    }

    // Takes item from player's hand to place on the shrine
    public void takeItemInHand(Player player, ItemStack itemInHand) {
        if (itemInHand.getAmount() > 1) {
            itemInHand.setAmount(itemInHand.getAmount() - 1);
        } else {
            player.getInventory().removeItemAnySlot(itemInHand);
        }
    }

    private Offering getOfferingForItem(ItemStack item, Deity deity) {
        // Get the favored offerings for the deity from the config
        List<String> validOfferings = plugin.getDeitiesConfig().getStringList("deities." + deity.getName().toLowerCase() + ".offerings");

        for (String offering : validOfferings) {
            String[] parts = offering.split(":");
            if (parts.length == 2) { // Ensure offering is in the correct format
                String offeringItem = parts[0];
                int favorValue;
                try {
                    favorValue = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid favor value for offering " + offeringItem + " for deity " + deity.getName());
                    continue;
                }

                if (offeringItem.equals(item.getType().toString())) {
                    return new Offering(item, deity, favorValue);
                }
            }
        }
        return null;
    }

    // Don't allow players to place blocks on top of shrines
    @EventHandler
    public void onBlockPlacedOnShrine(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Shrine shrine = shrineManager.getShrineAtLocation(block.getLocation().subtract(0, 1, 0)); // Check the block below
        if (shrine != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MessageUtils.parse("<red>You cannot place blocks on top of a shrine!"));
        }
    }

    // Don't allow players to destroy shrines
    @EventHandler
    public void onShrineBreakAttempt(BlockBreakEvent event) {
        Block block = event.getBlock();
        Shrine shrine = shrineManager.getShrineAtLocation(block.getLocation());
        if (shrine != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MessageUtils.parse("<red>You cannot destroy shrines! Remove with <yellow>/shrine remove<red>."));
        }
    }

    private void sendMessage(Player player, String message) {
        player.sendMessage(MessageUtils.parse(message));
    }

}
