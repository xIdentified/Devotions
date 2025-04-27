package me.xidentified.devotions.rituals;

import lombok.Getter;
import lombok.Setter;
import me.xidentified.devotions.Devotions;

@Getter
public class RitualObjective {

    public enum Type {
        GATHERING,
        PURIFICATION,
        MEDITATION,
        CRAFTING,
        BREEDING,
        SACRIFICE,
        PILGRIMAGE
    }

    private final Devotions plugin;
    private final Type type;
    private final String description;
    private final String target;  // Can be an item, a mob type, or a location
    private final int count;
    @Setter
    private int currentCount;  // To track objective progress
    private final boolean isRegionTarget; // Added boolean to differentiate between coordinates and region

    public RitualObjective(Devotions plugin, Type type, String description, String target, int count, boolean isRegionTarget) {
        this.plugin = plugin;
        this.type = type;
        this.description = description;
        this.target = target;
        this.count = count;
        this.currentCount = 0;
        this.isRegionTarget = isRegionTarget;
    }

    public void incrementCount() {
        if (this.currentCount < this.count) {
            this.currentCount++;
            plugin.debugLog("Incremented objective count to: " + this.currentCount + "/" + this.count);
        }
    }

    public boolean isComplete() {
        boolean complete = this.currentCount >= this.count;
        if (complete) {
            plugin.debugLog("Objective is marked as complete.");
        }
        return complete;
    }

    public void reset() {
        this.currentCount = 0; // Reset the current count
    }

    public int getCount() {
        return this.count;
    }

    public String getTarget() {
        return this.target;
    }

    public String getDescription() {
        return this.description;
    }

    public Type getType() {
        return this.type;
    }
}
