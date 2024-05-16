package me.xidentified.devotions.util;

import de.cubbossa.tinytranslations.libs.kyori.adventure.text.format.NamedTextColor;
import de.cubbossa.tinytranslations.libs.kyori.adventure.text.format.TextColor;
import me.xidentified.devotions.Devotions;

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
