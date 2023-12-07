package me.xidentified.devotions.effects;

import me.xidentified.devotions.Devotions;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Curse extends Effect {

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