package me.xidentified.devotions.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;

public class MessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder().build();

    // Parse mini-messages
    public static Component parse(String message) {
        return MINI_MESSAGE.deserialize(message);
    }

    // Convert location to string to make it more readable in storage
    public static String locationToString(Location location) {
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

}


