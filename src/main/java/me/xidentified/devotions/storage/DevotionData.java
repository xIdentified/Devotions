package me.xidentified.devotions.storage;

public record DevotionData(String deityName, int favor) {
    public int getFavor() {
        return favor;
    }

    public String getDeityName() {
        return deityName;
    }
}
