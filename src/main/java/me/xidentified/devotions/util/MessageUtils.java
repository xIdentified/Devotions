package me.xidentified.devotions.util;

import me.xidentified.devotions.Devotions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class MessageUtils {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder().build();
    private static final Devotions plugin = Devotions.getInstance();

    // Method to get color-coded favor text
    public static Component getFavorText(int favor) {
        int curseThreshold = plugin.getConfig().getInt("curse-threshold", 35);
        int blessingThreshold = plugin.getConfig().getInt("blessing-threshold", 150);
        int miracleThreshold = plugin.getConfig().getInt("miracle-threshold", 210);

        TextColor color = getColorForFavor(favor, curseThreshold, blessingThreshold, miracleThreshold);
        return Component.text(favor, color);
    }

    // Helper method to determine the color based on favor
    private static TextColor getColorForFavor(int favor, int curseThreshold, int blessingThreshold, int miracleThreshold) {
        if (favor < curseThreshold) return NamedTextColor.RED;
        if (favor < blessingThreshold) return NamedTextColor.YELLOW;
        if (favor < miracleThreshold) return NamedTextColor.GREEN;
        return NamedTextColor.AQUA; // For very high favor
    }

}


