package me.xidentified.devotions.rituals;

import lombok.Getter;
import me.xidentified.devotions.Devotions;

public class RitualObjective {

    // Other ritual objectives should be added later, but I'm going to keep it simple for right now
    public enum Type {
        GATHERING, PURIFICATION, MEDITATION
    }

    private final Devotions plugin;
    @Getter private final Type type;
    @Getter private final String description;
    @Getter private final String target;  // Can be an item or a mob type
    @Getter private final int count;
    private int currentCount;  // To track objective progress

    public RitualObjective(Devotions plugin, Type type, String description, String target, int count) {
        this.plugin = plugin;
        this.type = type;
        this.description = description;
        this.target = target;
        this.count = count;
        this.currentCount = 0;
    }

    public void setCurrentCount(int count) {
        this.currentCount = count;
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
        } else {
            plugin.debugLog("Objective is not yet complete. Current/Required: " + this.currentCount + "/" + this.count);
        }
        return complete;
    }

    public void reset() {
        this.currentCount = 0; // Reset the current count
    }
}