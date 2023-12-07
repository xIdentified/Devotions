package me.xidentified.devotions.effects;

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