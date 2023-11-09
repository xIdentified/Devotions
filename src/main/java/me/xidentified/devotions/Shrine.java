package me.xidentified.devotions;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Shrine {
    private final Location location;
    private Deity deity;
    private final Player owner;

    public Shrine(Location location, Deity deity, Player owner) {
        this.location = location;
        this.deity = deity;
        this.owner = owner;
    }

    public Location getLocation() {
        return location;
    }

    public Deity getDeity() {
        return deity;
    }

    public void setDeity(Deity deity) {
        this.deity = deity;
    }

    public Player getOwner() {
        return owner;
    }

}