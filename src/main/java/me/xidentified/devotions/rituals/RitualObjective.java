package me.xidentified.devotions.rituals;

import lombok.Getter;
import lombok.Setter;
import me.xidentified.devotions.Devotions;

@Getter
public class RitualObjective {

    // Other ritual objectives should be added later, but I'm going to keep it simple for right now
    public enum Type {
        GATHERING, PURIFICATION, MEDITATION
    }

    private final Devotions plugin;
    private final Type type;
    private final String description;
    private final String target;  // Can be an item or a mob type
    private final int count;
    @Setter private int currentCount;  // To track objective progress

    public RitualObjective(Devotions plugin, Type type, String description, String target, int count) {
        this.plugin = plugin;
        this.type = type;
        this.description = description;
        this.target = target;
        this.count = count;
        this.currentCount = 0;
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
}