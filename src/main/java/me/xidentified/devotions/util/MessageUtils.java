package me.xidentified.devotions.util;

import me.xidentified.devotions.Devotions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessageUtils {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final boolean IS_PAPER = checkForPaper();
    private static final Devotions plugin = Devotions.getInstance();

    // Check if the server is running on Paper
    private static boolean checkForPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void sendMessage(Player player, String message) {
        if (IS_PAPER) {
            // Use Adventure's Component-based method
            Component component = miniMessage.deserialize(message);
            player.sendMessage(component);
        } else {
            // Use the traditional String-based method
            String legacyMessage = ChatColor.translateAlternateColorCodes('&', message);
            player.sendMessage(legacyMessage);
        }
    }

    // Method to get color-coded favor text
    public static String getFavorText(int favor) {
        int curseThreshold = plugin.getConfig().getInt("curse-threshold", 35);
        int blessingThreshold = plugin.getConfig().getInt("blessing-threshold", 150);
        int miracleThreshold = plugin.getConfig().getInt("miracle-threshold", 210);

        TextColor color = getColorForFavor(favor, curseThreshold, blessingThreshold, miracleThreshold);

        if (IS_PAPER) {
            Component component = Component.text(favor, color);
            return LegacyComponentSerializer.legacySection().serialize(component);
        } else {
            ChatColor chatColor = convertToChatColor(color);
            return chatColor + String.valueOf(favor);
        }
    }

    // Helper method to determine the color based on favor
    private static TextColor getColorForFavor(int favor, int curseThreshold, int blessingThreshold, int miracleThreshold) {
        if (favor < curseThreshold) return NamedTextColor.RED;
        if (favor < blessingThreshold) return NamedTextColor.YELLOW;
        if (favor < miracleThreshold) return NamedTextColor.GREEN;
        return NamedTextColor.AQUA; // For very high favor
    }

    // Convert TextColor to ChatColor for legacy servers
    private static ChatColor convertToChatColor(TextColor color) {
        if (color == NamedTextColor.RED) return ChatColor.RED;
        if (color == NamedTextColor.YELLOW) return ChatColor.YELLOW;
        if (color == NamedTextColor.GREEN) return ChatColor.GREEN;
        if (color == NamedTextColor.AQUA) return ChatColor.AQUA;
        return ChatColor.WHITE; // Default color
    }
}


