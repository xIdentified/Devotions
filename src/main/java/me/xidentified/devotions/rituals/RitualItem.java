package me.xidentified.devotions.rituals;

/**
 * @param type VANILLA or MMOITEM
 * @param id   Minecraft ID or MMOItem ID
 */
public record RitualItem(String type, String id) {

    // Creating a unique ID for usage in maps
    public String getUniqueId() {
        return type + ":" + id;
    }

    // Ensure uniqueness and equivalence checks are accurate
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RitualItem that = (RitualItem) obj;
        return type.equals(that.type) && id.equals(that.id);
    }

}
