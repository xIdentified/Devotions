package me.xidentified.devotions;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

@Getter
public class Offering {
    @Setter
    private Deity deity;
    private final int value;  // The amount of favor given for an offering

    public Offering(ItemStack item, Deity deity, int value) {
        this.deity = deity;
        this.value = value;
    }

}
