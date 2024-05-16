package me.xidentified.devotions.rituals;

import org.bukkit.inventory.ItemStack;

public class RitualItem {

    public final String type;
    public String id;
    public ItemStack itemStack;

    // Constructor for regular items
    public RitualItem(String type, String id) {
        this.type = type;
        this.id = id;
    }

    // Constructor for saved items
    public RitualItem(String type, ItemStack itemStack) {
        this.type = type;
        this.itemStack = itemStack;
    }

    // Creating a unique ID for usage in maps
    public String getUniqueId() {
        return type + ":" + id;
    }

    // Ensure uniqueness and equivalence checks are accurate
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RitualItem that = (RitualItem) obj;
        return type.equals(that.type) && id.equals(that.id);
    }

    public String id() {
        return this.id;
    }
}
