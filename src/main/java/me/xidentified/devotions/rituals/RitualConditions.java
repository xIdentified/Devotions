package me.xidentified.devotions.rituals;

public record RitualConditions(String expression, String time, String biome, String weather, String moonPhase, Double minAltitude,
                               Integer minExperience, Double minHealth, Integer minHunger) {

    public long getMoonPhaseNumber(String moonPhase) {
        return switch (moonPhase) {
            case "FULL_MOON" -> 0;
            case "WANING_GIBBOUS" -> 1;
            case "LAST_QUARTER" -> 2;
            case "WANING_CRESCENT" -> 3;
            case "NEW_MOON" -> 4;
            case "WAXING_CRESCENT" -> 5;
            case "FIRST_QUARTER" -> 6;
            case "WAXING_GIBBOUS" -> 7;
            default -> throw new IllegalArgumentException("Unknown moon phase: " + moonPhase);
        };
    }

}
