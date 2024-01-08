package me.xidentified.devotions.commandexecutors;

import de.cubbossa.tinytranslations.GlobalMessages;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.util.Messages;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DevotionsCommandExecutor implements CommandExecutor, TabCompleter {

    private final Devotions plugin;

    public DevotionsCommandExecutor(Devotions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            return false;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("devotions.reload")) {
                plugin.sendMessage(sender, GlobalMessages.NO_PERM_CMD);
                return true;
            }

            plugin.reloadConfigurations();
            plugin.sendMessage(sender,Messages.DEVOTION_RELOAD_SUCCESS);
            return true;
        }
        if ("saveitem".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("devotions.admin")) {
                plugin.sendMessage(sender, GlobalMessages.NO_PERM_CMD);
                return true;
            }

            if (!(sender instanceof Player player)) {
                plugin.sendMessage(sender, GlobalMessages.CMD_PLAYER_ONLY);
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Please provide a name for the item.");
                return true;
            }

            String itemName = args[1];
            ItemStack itemInHand = player.getInventory().getItemInMainHand();

            if (itemInHand.getType() == Material.AIR) {
                player.sendMessage(ChatColor.RED + "You must hold an item in your hand.");
                return true;
            }

            saveItem(itemName, itemInHand);
            player.sendMessage("Item saved as " + itemName);
            return true;
        }

        return false;
    }

    private void saveItem(String name, ItemStack item) {
        File storageFolder = new File(plugin.getDataFolder(), "storage");
        if (!storageFolder.exists()) {
            storageFolder.mkdirs();
        }

        File itemsFile = new File(storageFolder, "savedItems.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
        config.set("items." + name, item.serialize());
        try {
            config.save(itemsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("reload");
            completions.add("saveitem");
        }

        return completions;
    }
}
