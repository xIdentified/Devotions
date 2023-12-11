package me.xidentified.devotions;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.util.UUID;

@Getter
public class Shrine {
    private final Location location;
    private final UUID owner;
    @Setter
    private Deity deity;

    public Shrine(Location location, Deity deity, UUID owner) {
        this.location = location;
        this.deity = deity;
        this.owner = owner;
    }

}