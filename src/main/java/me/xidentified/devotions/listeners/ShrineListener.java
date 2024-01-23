package me.xidentified.devotions.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.Offering;
import me.xidentified.devotions.Shrine;
import me.xidentified.devotions.managers.*;
import me.xidentified.devotions.rituals.Ritual;
import me.xidentified.devotions.util.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
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
import org.bukkit.util.Vector;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
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

    @EventHandler
    public void onShrineInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        // Return if player isn't right-clicking the shrine or interacting with their hand.
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || clickedBlock == null) return;

        // Make sure valid shrine is at location
        Shrine shrine = shrineManager.getShrineAtLocation(clickedBlock.getLocation());
        if (shrine == null) return;

        // Check config setting for shrine interaction
        boolean allPlayersCanInteract = plugin.getConfig().getBoolean("all-players-can-interact-with-shrines", true);

        // If the shrine doesn't belong to player's deity inform them if config is configured to
        if (!allPlayersCanInteract) {
            Deity playerDeity = devotionManager.getPlayerDevotion(player.getUniqueId()).getDeity();
            if (!shrine.getDeity().equals(playerDeity)) {
                plugin.sendMessage(player, Messages.SHRINE_NOT_FOLLOWING_DEITY.formatted(
                        Placeholder.unparsed("deity", shrine.getDeity().getName())
                ));
                return;
            }
        }

        // Check for ritual initiation
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        Ritual ritual = RitualManager.getInstance(plugin).getRitualByItem(itemInHand);
        if (ritual != null) {
            long remainingCooldown = cooldownManager.isActionAllowed(player, "ritual");
            if (remainingCooldown > 0) {
                plugin.sendMessage(player, Messages.SHRINE_COOLDOWN.formatted(
                    Formatter.date("cooldown", LocalDateTime.ofInstant(Instant.ofEpochMilli(remainingCooldown), ZoneId.systemDefault()))
                ));
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
                plugin.sendMessage(player, Messages.SHRINE_COOLDOWN.formatted(
                    Formatter.date("cooldown", LocalDateTime.ofInstant(Instant.ofEpochMilli(remainingCooldown), ZoneId.systemDefault()))
                ));
                return;
            }
            try {
                Item droppedItem = dropItemOnShrine(clickedBlock, itemInHand);
                handleOfferingInteraction(player, clickedBlock, itemInHand, droppedItem);
            } catch (Exception e) {
                plugin.getLogger().severe("Error while handling offering interaction: " + e.getMessage());
                e.printStackTrace();
            }
        }
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

    private void handleOfferingInteraction(Player player, Block clickedBlock, ItemStack itemInHand, Item droppedItem) {
        // Get the player's deity
        FavorManager favorManager = devotionManager.getPlayerDevotion(player.getUniqueId());
        Deity playerDeity = favorManager.getDeity();

        // Check if the offering is valid for the player's deity
        Offering offering = getOfferingForItem(itemInHand, playerDeity);

        if (offering != null) {
            long offeringCooldown = cooldownManager.getCooldownFromConfig("offering-cooldown", "5s");
            cooldownManager.setCooldown(player, "offering", offeringCooldown);

            takeItemInHand(player, itemInHand);
            plugin.sendMessage(player, Messages.SHRINE_OFFERING_ACCEPTED);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                favorManager.increaseFavor(offering.getValue());
                if (droppedItem != null) droppedItem.remove();
                plugin.playConfiguredSound(player, "offeringAccepted");
                spawnLocalizedParticles(clickedBlock.getLocation().add(0.5, 1, 0.5), Particle.SPELL_WITCH, 50);

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
            plugin.sendMessage(player, Messages.SHRINE_OFFERING_DECLINED.formatted(
                    Placeholder.unparsed("subject", playerDeity.getName())
            ));
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
            // Use removeItem instead of removeItemAnySlot
            player.getInventory().removeItem(itemInHand);
        }
    }

    private Offering getOfferingForItem(ItemStack item, Deity deity) {
        // Load saved items from configuration
        FileConfiguration config = plugin.getSavedItemsConfig();
        ConfigurationSection savedItemsSection = config.getConfigurationSection("items");

        // Get the favored offerings for the deity from the config
        List<String> validOfferings = plugin.getDeitiesConfig().getStringList("deities." + deity.getName().toLowerCase() + ".offerings");

        for (String offering : validOfferings) {
            String[] parts = offering.split(":");
            if (parts.length < 3) { // Ensure offering is in the correct format
                continue;
            }

            String offeringType = parts[0];
            String offeringItemId = parts[1];
            int favorValue;
            try {
                favorValue = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid favor value for offering " + offering + " for deity " + deity.getName());
                continue;
            }

            List<String> commands = new ArrayList<>();
            if (parts.length > 3) {
                // Parts[3] contains the commands separated by a semicolon
                commands = Arrays.asList(parts[3].split(";"));
            }

            if ("SAVED".equalsIgnoreCase(offeringType)) {
                // Handle saved items
                if (savedItemsSection != null) {
                    ItemStack savedItem = savedItemsSection.getItemStack(offeringItemId);
                    if (savedItem != null && savedItem.isSimilar(item)) {
                        return new Offering(item, favorValue, commands);
                    }
                }
            } else if ("VANILLA".equalsIgnoreCase(offeringType)) {
                // Handle vanilla items
                Material material = Material.matchMaterial(offeringItemId);
                if (material != null && item.getType() == material) {
                    return new Offering(item, favorValue, commands);
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
            plugin.sendMessage(event.getPlayer(), Messages.SHRINE_PLACE_ON_TOP);
        }
    }

    // Don't allow players to destroy shrines
    @EventHandler
    public void onShrineBreakAttempt(BlockBreakEvent event) {
        Block block = event.getBlock();
        Shrine shrine = shrineManager.getShrineAtLocation(block.getLocation());
        if (shrine != null) {
            event.setCancelled(true);
            plugin.sendMessage(event.getPlayer(), Messages.SHRINE_CANNOT_BREAK);
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

}
