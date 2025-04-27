package me.xidentified.devotions;

import de.cubbossa.tinytranslations.Message;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import me.xidentified.devotions.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class Miracle {

    private final String name;
    private final List<Condition> conditions;
    private final MiracleEffect effect;

    public Miracle(String name, List<Condition> conditions, MiracleEffect effect) {
        this.name = name;
        this.conditions = conditions;
        this.effect = effect;
    }

    public boolean canTrigger(Player player) {
        for (Condition condition : conditions) {
            if (!condition.check(player)) {
                return false;
            }
        }
        return true;
    }

    public void apply(Player player) {
        effect.execute(player);
    }

    public String getName() {
        return name;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

}

interface Condition {

    boolean check(Player player);
}

class NearVillagersCondition implements Condition {

    @Override
    public boolean check(Player player) {
        int villagerCount = 0;
        for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if (entity.getType() == EntityType.VILLAGER) {
                villagerCount++;
            }
        }
        Bukkit.getLogger().info("Number of villagers near player: " + villagerCount);
        return villagerCount >= 3;
    }

}

class IsDeadCondition implements Condition {

    @Override
    public boolean check(Player player) {
        return player.isDead();
    }
}

class IsOnFireCondition implements Condition {

    @Override
    public boolean check(Player player) {
        return player.getFireTicks() > 0;
    }
}

class LowHealthCondition implements Condition {

    @Override
    public boolean check(Player player) {
        Bukkit.getLogger().info("Player's health: " + player.getHealth());
        return player.getHealth() <= 10;
    }
}

class NearHostileMobsCondition implements Condition {

    @Override
    public boolean check(Player player) {
        for (Entity entity : player.getNearbyEntities(4, 4, 4)) {
            if (isHostileMob(entity)) {
                return true;
            }
        }
        return false;
    }

    private boolean isHostileMob(Entity entity) {
        return entity instanceof Monster;
    }
}

class HasRepairableItemsCondition implements Condition {

    @Override
    public boolean check(Player player) {
        // Check main inventory
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (isRepairable(item)) {
                return true;
            }
        }
        // Check armor slots
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (isRepairable(armor)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRepairable(ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof Damageable damageable) {
                return damageable.hasDamage();
            }
        }
        return false;
    }
}


interface MiracleEffect {

    void execute(Player player);
}


// Existing miracles listed below
class ReviveOnDeath implements MiracleEffect {

    @Override
    public void execute(Player player) {
        // In Spigot, we can use the player's getMaxHealth method directly
        double maxHealth = player.getMaxHealth();
        player.setHealth(maxHealth);
        Devotions.sendMessage(player, Messages.MIRACLE_SAVED_FROM_DEATH);
    }
}

class HeroEffectInVillage implements MiracleEffect {

    @Override
    public void execute(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 6000,
                1)); // 5 minutes of Hero of the Village effect
        Devotions.sendMessage(player, Messages.MIRACLE_HERO_OF_VILLAGE);
    }
}

class SaveFromBurning implements MiracleEffect {

    @Override
    public void execute(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 300, 1));
        Devotions.sendMessage(player, Messages.MIRACLE_FIRE_RESISTANCE);
    }
}

class RepairAllItems implements MiracleEffect {

    @Override
    public void execute(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                ItemMeta meta = item.getItemMeta();
                if (meta instanceof Damageable damageable) {
                    if (damageable.hasDamage()) {
                        damageable.setDamage(0);  // Repair the item
                        item.setItemMeta(meta);  // Update the item with the repaired meta
                    }
                }
            }
        }
        Devotions.sendMessage(player, Messages.MIRACLE_REPAIR);
    }
}

class SummonAidEffect implements MiracleEffect {

    private final int entityCount;

    public SummonAidEffect(int entityCount) {
        this.entityCount = entityCount;
    }

    @Override
    public void execute(Player player) {
        Random random = new Random();
        EntityType entityType;
        Message message;

        // 50% chance to decide between Iron Golems or Wolves
        if (random.nextBoolean()) {
            entityType = EntityType.IRON_GOLEM;
            message = Messages.MIRACLE_GOLEM;
        } else {
            entityType = EntityType.WOLF;
            message = Messages.MIRACLE_WOLVES;
        }

        for (int i = 0; i < entityCount; i++) {
            Entity entity = player.getWorld().spawnEntity(player.getLocation(), entityType);
            if (entityType == EntityType.WOLF) {
                Wolf wolf = (Wolf) entity;
                wolf.setOwner(player);
                wolf.setTamed(true);
            }
        }
        Devotions.sendMessage(player, message);
    }
}

class ExecuteCommandEffect implements MiracleEffect {

    private final String command;

    public ExecuteCommandEffect(String command) {
        this.command = command;
    }

    @Override
    public void execute(Player player) {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
        Devotions.sendMessage(player, Messages.MIRACLE_BESTOWED);
    }
}

class DoubleCropDropsEffect implements MiracleEffect, Listener {

    private final int duration;  // in ticks
    private static final Set<UUID> playersWithEffect = new HashSet<>();
    private final Plugin plugin;

    public DoubleCropDropsEffect(Plugin plugin, int durationInSeconds) {
        this.plugin = plugin;
        this.duration = durationInSeconds * 20;
    }


    @Override
    public void execute(Player player) {
        playersWithEffect.add(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> playersWithEffect.remove(player.getUniqueId()), duration);
        Devotions.sendMessage(player, Messages.MIRACLE_HARVEST);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Check if the block is a mature crop
        if (block.getBlockData() instanceof Ageable ageable) {
            if (ageable.getAge() == ageable.getMaximumAge()) {  // Check for mature crops
                if (playersWithEffect.contains(player.getUniqueId())) {
                    // Cancel the original event to prevent default drops
                    event.setDropItems(false);

                    // Get the default drops
                    Collection<ItemStack> drops = block.getDrops();

                    // Drop double of each item
                    for (ItemStack item : drops) {
                        ItemStack doubleItem = item.clone();
                        doubleItem.setAmount(item.getAmount() * 2);
                        block.getWorld().dropItemNaturally(block.getLocation(), doubleItem);
                    }
                }
            }
        }
    }

}

// Check if player is near any crops
class NearCropsCondition implements Condition {

    @Override
    public boolean check(Player player) {
        for (Block block : getNearbyBlocks(player.getLocation())) {  // Check in a 5 block radius
            Material type = block.getType();
            if (type == Material.WHEAT || type == Material.CARROTS || type == Material.POTATOES
                    || type == Material.BEETROOTS) {
                return true;
            }
        }
        return false;
    }

    private List<Block> getNearbyBlocks(Location location) {
        List<Block> blocks = new ArrayList<>();
        for (int x = location.getBlockX() - 5; x <= location.getBlockX() + 5; x++) {
            for (int y = location.getBlockY() - 5; y <= location.getBlockY() + 5; y++) {
                for (int z = location.getBlockZ() - 5; z <= location.getBlockZ() + 5; z++) {
                    blocks.add(location.getWorld().getBlockAt(x, y, z));
                }
            }
        }
        return blocks;
    }
}
