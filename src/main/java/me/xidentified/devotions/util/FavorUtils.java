package me.xidentified.devotions.util;

import me.xidentified.devotions.Devotions;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class FavorUtils {

    public static TextColor getColorForFavor(int favor) {
        Devotions plugin = Devotions.getInstance();
        int curseThreshold = plugin.getConfig().getInt("curse-threshold");
        int blessingThreshold = plugin.getConfig().getInt("blessing-threshold");
        int miracleThreshold = plugin.getConfig().getInt("miracle-threshold");

        if (favor <= curseThreshold) {
            return NamedTextColor.RED;
        } else if (favor <= blessingThreshold) {
            return NamedTextColor.YELLOW;
        } else if (favor <= miracleThreshold) {
            return NamedTextColor.GREEN;
        } else {
            return NamedTextColor.AQUA;
        }
    }
}
