package me.xidentified.devotions.util;

import me.xidentified.devotions.Devotions;

public class FavorUtils {

    public static String getColorForFavor(int favor) {
        Devotions plugin = Devotions.getInstance();
        int curseThreshold = plugin.getConfig().getInt("curse-threshold");
        int blessingThreshold = plugin.getConfig().getInt("blessing-threshold");
        int miracleThreshold = plugin.getConfig().getInt("miracle-threshold");

        if (favor <= curseThreshold) {
            return "<red>";
        } else if (favor <= blessingThreshold) {
            return "<yellow>";
        } else if (favor <= miracleThreshold) {
            return "<green>";
        } else {
            return "<aqua>";
        }
    }
}
