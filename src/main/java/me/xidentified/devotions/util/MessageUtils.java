package me.xidentified.devotions.util;

import me.xidentified.devotions.Devotions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class MessageUtils {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final Devotions plugin = Devotions.getInstance();

    // Sends a MiniMessage formatted message to the player
    public static void sendMessage(Player player, String message) {
        Component component = miniMessage.deserialize(message);
        player.sendMessage(component);
    }

    // Returns a MiniMessage formatted string representing the favor amount in color
    public static String getFavorText(int favor) {
        String colorName = getColorNameForFavor(favor);
        return "<" + colorName + ">" + favor + "</" + colorName + ">";
    }

    // Determines the color name based on the favor amount
    private static String getColorNameForFavor(int favor) {
        int curseThreshold = plugin.getConfig().getInt("curse-threshold", 35);
        int blessingThreshold = plugin.getConfig().getInt("blessing-threshold", 150);
        int miracleThreshold = plugin.getConfig().getInt("miracle-threshold", 210);

        if (favor < curseThreshold) return "red";
        if (favor < blessingThreshold) return "yellow";
        if (favor < miracleThreshold) return "green";
        return "aqua"; // For very high favor
    }
}
