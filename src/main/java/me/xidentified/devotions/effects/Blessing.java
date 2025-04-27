package me.xidentified.devotions.effects;

import me.xidentified.devotions.Devotions;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Blessing extends Effect {

    private final PotionEffectType potionEffectType;

    public Blessing(String type, int duration, int potency, PotionEffectType potionEffectType) {
        super(type, duration, potency);
        this.potionEffectType = potionEffectType;
    }

    public void applyTo(Player player) {
        player.addPotionEffect(new PotionEffect(potionEffectType, duration * 20, potency - 1, false, false, false));
    }

    public void applyVisualEffect(Player player) {
        player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 10);
    }

    public void applyAudioEffect(Player player) {
        Devotions.getInstance().playConfiguredSound(player, "blessingReceived");
    }
}
