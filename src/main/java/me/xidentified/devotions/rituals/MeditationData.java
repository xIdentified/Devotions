package me.xidentified.devotions.rituals;

import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;

public record MeditationData(long startTime, Location initialLocation, AtomicInteger moveCounter) {

    public MeditationData(long startTime, Location initialLocation) {
        this(startTime, initialLocation, new AtomicInteger(0));
    }
}
