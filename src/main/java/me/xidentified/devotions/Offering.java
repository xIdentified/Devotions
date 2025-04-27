package me.xidentified.devotions;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

@Getter
public class Offering {

    private final ItemStack itemStack;
    private final int value; // The amount of favor given for an offering
    private final List<String> commands; // Commands to run after the offering is accepted
    @Setter
    private Deity deity;
    private final double chance;

    public Offering(ItemStack itemStack, int value, List<String> commands, double chance) {
        this.itemStack = itemStack;
        this.value = value;
        this.commands = commands;
        this.chance = chance;
    }

    public int getValue() {
        return this.value;
    }

    public List<String> getCommands() {
        return this.commands;
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public double getChance() {
        return this.chance;
    }
}
