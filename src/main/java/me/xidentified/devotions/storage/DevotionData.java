package me.xidentified.devotions.storage;

import lombok.Getter;

@Getter
public record DevotionData(String deityName, int favor) {

}
