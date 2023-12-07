package me.xidentified.devotions.storage;

public record DevotionData(String deityName, int favor) {

  public String getDeityName() {
    return deityName;
  }

  public int getFavor() {
    return favor;
  }
}
