package me.xidentified.devotions;

import org.bukkit.Location;

import java.util.UUID;

public class Shrine {
    private final Location location;
    private Deity deity;
    private final UUID owner;

    public Shrine(Location location, Deity deity, UUID owner) {
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

    public UUID getOwner() {
        return owner;
    }

}