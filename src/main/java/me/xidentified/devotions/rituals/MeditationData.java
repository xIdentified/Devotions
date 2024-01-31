package me.xidentified.devotions.rituals;

import org.bukkit.Location;

import java.util.concurrent.atomic.AtomicInteger;

public record MeditationData(long startTime, Location initialLocation, AtomicInteger moveCounter) {
    public MeditationData(long startTime, Location initialLocation) {
        this(startTime, initialLocation, new AtomicInteger(0));
    }
}