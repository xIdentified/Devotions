package me.xidentified.devotions.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class MessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder().build();

    // Parse mini-messages
    public static Component parse(String message) {
        return MINI_MESSAGE.deserialize(message);
    }

}


