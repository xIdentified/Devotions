package me.xidentified.devotions.listeners;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import me.clip.placeholderapi.PlaceholderAPI;
import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.Offering;
import me.xidentified.devotions.Shrine;
import me.xidentified.devotions.managers.CooldownManager;
import me.xidentified.devotions.managers.DevotionManager;
import me.xidentified.devotions.managers.FavorManager;
import me.xidentified.devotions.rituals.RitualManager;
import me.xidentified.devotions.managers.ShrineManager;
import me.xidentified.devotions.rituals.Ritual;
import me.xidentified.devotions.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.util.Vector;

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

    @EventHandler
    public void onShrineInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        // Return if player isn't right-clicking the shrine or interacting with their hand.
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || clickedBlock == null) {
            return;
        }

        // Make sure valid shrine is at location
        Shrine shrine = shrineManager.getShrineAtLocation(clickedBlock.getLocation());
        if (shrine == null) {
            return;
        }

        // Check config setting for shrine interaction
        boolean allPlayersCanInteract = plugin.getConfig().getBoolean("all-players-can-interact-with-shrines", true);

        // If the shrine doesn't belong to player's deity inform them if config is configured to
        if (!allPlayersCanInteract) {
            Deity playerDeity = devotionManager.getPlayerDevotion(player.getUniqueId()).getDeity();
            if (!shrine.getDeity().equals(playerDeity)) {
                Devotions.sendMessage(player, Messages.SHRINE_NOT_FOLLOWING_DEITY.insertParsed("deity", shrine.getDeity().getName()));
                return;
            }
        }

        // Check for ritual initiation
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        Ritual ritual = RitualManager.getInstance(plugin).getRitualByItem(itemInHand);
        plugin.debugLog("Checking for ritual associated with item: " + itemInHand);

        if (ritual != null) {
            event.setCancelled(true); // Prevent the default action (block placement)
            try {
                Item droppedItem = null;
                if (ritual.isConsumeItem()) {
                    droppedItem = dropItemOnShrine(clickedBlock, itemInHand);
                }

                handleRitualInteraction(player, itemInHand, droppedItem, event, ritual.isConsumeItem());
            } catch (Exception e) {
                plugin.getLogger().severe("Error while handling ritual: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        // Check if the player is holding a valid offering
        Offering offering = getOfferingForItem(itemInHand, devotionManager.getPlayerDevotion(player.getUniqueId()).getDeity());
        plugin.debugLog("Checking for ritual associated with item: " + itemInHand);
        plugin.debugLog("Checking for offering associated with item: " + itemInHand);
        if (offering != null) {
            event.setCancelled(true); // Prevent the default action (block placement)
            try {
                // Only drop item on shrine if it's a valid offering
                Item droppedItem = dropItemOnShrine(clickedBlock, itemInHand);
                handleOfferingInteraction(player, clickedBlock, itemInHand, droppedItem);
            } catch (Exception e) {
                plugin.getLogger().severe("Error while handling offering: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void handleRitualInteraction(Player player, ItemStack itemInHand, Item droppedItem,
            PlayerInteractEvent event, boolean consumeItem) {

        long remainingCooldown = cooldownManager.isActionAllowed(player, "ritual");
        if (remainingCooldown > 0) {
            Devotions.sendMessage(player, Messages.SHRINE_COOLDOWN
                    .insertTemporal("cooldown",
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(remainingCooldown), ZoneId.systemDefault()))
            );
            event.setCancelled(true);
            if (droppedItem != null) {
                droppedItem.remove();
            }
            return;
        }

        boolean ritualStarted = RitualManager.getInstance(plugin).startRitual(player, itemInHand, droppedItem);
        if (ritualStarted) {
            long ritualCooldown = cooldownManager.getCooldownFromConfig("ritual-cooldown", "5s");
            cooldownManager.setCooldown(player, "ritual", ritualCooldown);

            if (consumeItem) {
                player.getInventory().removeItem(new ItemStack(itemInHand.getType(), 1));
            }
        } else if (droppedItem != null) {
            droppedItem.remove();  // Remove dropped item from shrine if ritual did not start
        }
        // Cancel the event only if the ritual started successfully
        event.setCancelled(!ritualStarted);
    }

    private void handleOfferingInteraction(Player player, Block clickedBlock, ItemStack itemInHand, Item droppedItem) {
        long remainingCooldown = cooldownManager.isActionAllowed(player, "offering");
        if (remainingCooldown > 0) {
            Devotions.sendMessage(player, Messages.SHRINE_COOLDOWN
                    .insertTemporal("cooldown",
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(remainingCooldown), ZoneId.systemDefault()))
            );
            if (droppedItem != null) {
                droppedItem.remove();
            }
            return;
        }

        // Get the player's deity
        FavorManager favorManager = devotionManager.getPlayerDevotion(player.getUniqueId());
        Deity playerDeity = favorManager.getDeity();

        // Check if the offering is valid for the player's deity
        Offering offering = getOfferingForItem(itemInHand, playerDeity);

        if (offering != null) {
            long offeringCooldown = cooldownManager.getCooldownFromConfig("offering-cooldown", "5s");
            cooldownManager.setCooldown(player, "offering", offeringCooldown);

            takeItemInHand(player, itemInHand);
            Devotions.sendMessage(player, Messages.SHRINE_OFFERING_ACCEPTED);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                favorManager.adjustFavor(offering.getValue());
                if (droppedItem != null) {
                    droppedItem.remove();
                }
                plugin.playConfiguredSound(player, "offeringAccepted");
                spawnLocalizedParticles(clickedBlock.getLocation().add(0.5, 1, 0.5), Particle.WITCH, 50);

                // Execute commands
                for (String cmd : offering.getCommands()) {
                    String command = cmd.replace("{player}", player.getName());
                    // Replace placeholders if PlaceholderAPI is present
                    if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                        command = PlaceholderAPI.setPlaceholders(player, command);
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    plugin.debugLog("Command executed: " + command);
                }
            }, 100L);
        } else {
            Devotions.sendMessage(player, Messages.SHRINE_OFFERING_DECLINED
                    .insertParsed("subject", playerDeity.getName())
            );
            if (droppedItem != null) {
                droppedItem.remove();
            }
        }
    }

    private void spawnLocalizedParticles(Location location, Particle particle, int count) {
        location.getWorld().spawnParticle(particle, location, count, 0.5, 0.5, 0.5, 0.5);
    }

    // Takes item from player's hand to place on the shrine
    public void takeItemInHand(Player player, ItemStack itemInHand) {
        ItemStack singleItemStack = new ItemStack(itemInHand.getType(), 1);
        player.getInventory().removeItem(singleItemStack);
        player.updateInventory();
    }

    private Offering getOfferingForItem(ItemStack item, Deity deity) {
        for (Offering offering : deity.getOfferings()) {
            if (matchesOffering(item, offering)) {
                plugin.debugLog(String.format("Offering found for deity %s: %s", deity.getName(),
                        offering.getItemStack().getItemMeta().getDisplayName()));

                // Now consider the chance for offering acceptance
                if (Math.random() <= offering.getChance()) {
                    return offering;
                } else {
                    // Log that the offering was not accepted due to chance
                    plugin.debugLog(
                            String.format("Offering for deity %s was not accepted due to chance: %s", deity.getName(),
                                    offering));
                    return null;
                }
            }
        }
        // No offering found
        return null;
    }

    private boolean matchesOffering(ItemStack item, Offering offering) {
        // Check for item type match
        if (!item.getType().equals(offering.getItemStack().getType())) {
            return false;
        }

        // If the offering is a potion, verify potion type
        if (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION
                || item.getType() == Material.LINGERING_POTION) {
            PotionMeta itemMeta = (PotionMeta) item.getItemMeta();
            PotionMeta offeringMeta = (PotionMeta) offering.getItemStack().getItemMeta();

            if (itemMeta == null || offeringMeta == null) {
                return false; // Either the item or the offering has no metadata, so they do not match
            }

            if (!itemMeta.getBasePotionData().getType().equals(offeringMeta.getBasePotionData().getType())) {
                return false; // Potion types do not match
            }
        }

        // TODO: Check for 'SAVED' items here, match metadata/lore/enchantments and stuff maybe?

        // If no checks failed, the item matches the offering
        return true;
    }


    // Don't allow players to place blocks on top of shrines
    @EventHandler
    public void onBlockPlacedOnShrine(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // Check if the block is being placed on or adjacent to a shrine
        if (isBlockAdjacentToShrine(block) || shrineManager.getShrineAtLocation(block.getLocation()) != null) {
            Ritual ritual = RitualManager.getInstance(plugin).getRitualByItem(itemInHand);
            Offering offering = getOfferingForItem(itemInHand, devotionManager.getPlayerDevotion(player.getUniqueId()).getDeity());

            event.setCancelled(true);
            if (ritual != null || offering != null) {
            } else {
                // Prevent block placement if it's not part of a ritual or offering
                Devotions.sendMessage(player, Messages.SHRINE_PLACE_ON_TOP);
            }
        }
    }

    // See if blocks are placed next to shrines
    private boolean isBlockAdjacentToShrine(Block block) {
        // Check all adjacent blocks to see if any is a shrine
        for (BlockFace face : BlockFace.values()) {
            if (shrineManager.getShrineAtLocation(block.getRelative(face).getLocation()) != null) {
                return true;
            }
        }
        return false;
    }

    // Don't allow players to destroy shrines
    @EventHandler
    public void onShrineBreakAttempt(BlockBreakEvent event) {
        Block block = event.getBlock();
        Shrine shrine = shrineManager.getShrineAtLocation(block.getLocation());
        if (shrine != null) {
            event.setCancelled(true);
            Devotions.sendMessage(event.getPlayer(), Messages.SHRINE_CANNOT_BREAK);
        }
    }

    private Item dropItemOnShrine(Block clickedBlock, ItemStack itemInHand) {

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

            plugin.debugLog("Item on shrine spawned at: " + dropLocation + " with ID " + singleItemStack.getType());
            return droppedItem;
        }
        plugin.debugLog("Item on shrine not spawned.");
        return null;
    }

}
