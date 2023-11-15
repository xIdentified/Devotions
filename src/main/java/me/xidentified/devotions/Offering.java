package me.xidentified.devotions;

import org.bukkit.inventory.ItemStack;

public class Offering {
    private Deity deity;
    private final int value;  // The amount of favor given for an offering

    public Offering(ItemStack item, Deity deity, int value) {
        this.deity = deity;
        this.value = value;
    }

    public Deity getDeity() {
        return deity;
    }

    public void setDeity(Deity deity) {
        this.deity = deity;
    }

    public int getValue() {
        return value;
    }

}
