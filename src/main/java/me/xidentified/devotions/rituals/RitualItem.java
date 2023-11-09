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

    public static String getReadableName(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        StringBuilder readableName = new StringBuilder();
        String[] words = id.split("_");
        for (String word : words) {
            if (word.length() > 0) {
                readableName.append(word.substring(0, 1).toUpperCase())
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return readableName.toString().trim();
    }

}
