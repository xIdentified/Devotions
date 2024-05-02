package me.xidentified.devotions;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Getter
public class Offering {
    private final ItemStack itemStack;
    private final int value; // The amount of favor given for an offering
    private final List<String> commands; // Commands to run after the offering is accepted
    @Setter private Deity deity;
    private final double chance;

    public Offering(ItemStack itemStack, int value, List<String> commands, double chance) {
        this.itemStack = itemStack;
        this.value = value;
        this.commands = commands;
        this.chance = chance;
    }

}

