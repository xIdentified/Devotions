package me.xidentified.devotions;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public abstract class Effect {
    private final String type;
    protected int duration;
    protected int potency;

    public Effect(String type, int duration, int potency) {
        this.type = type;
        this.duration = duration;
        this.potency = potency;
    }

    public String getName() {
        // Convert to lowercase and replace underscores with spaces
        String name = type.toLowerCase().replace("_", " ");

        // Capitalize first letter of each word
        String[] words = name.split(" ");
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].substring(0, 1).toUpperCase() + words[i].substring(1);
        }
        name = String.join(" ", words);

        return name;
    }

}

class Blessing extends Effect {

    private final PotionEffectType potionEffectType;

    public Blessing(String type, int duration, int potency, PotionEffectType potionEffectType) {
        super(type, duration, potency);
        this.potionEffectType = potionEffectType;
    }

    public void applyTo(Player player) {
        player.addPotionEffect(new PotionEffect(potionEffectType, duration * 20, potency - 1, false, false, false));
    }

    public void applyVisualEffect(Player player) {
        player.spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation(), 10);
    }

    public void applyAudioEffect(Player player) {
        Devotions.getInstance().playConfiguredSound(player, "blessingReceived");
    }
}

class Curse extends Effect {

    private final PotionEffectType potionEffectType;

    public Curse(String type, int duration, int potency, PotionEffectType potionEffectType) {
        super(type, duration, potency);
        this.potionEffectType = potionEffectType;
    }

    public void applyTo(Player player) {
        player.addPotionEffect(new PotionEffect(potionEffectType, duration * 20, potency - 1, false, false, true));
    }

    public void applyVisualEffect(Player player) {
        player.spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 10);
    }

    public void applyAudioEffect(Player player) {
        Devotions.getInstance().playConfiguredSound(player, "curseReceived");
    }

}