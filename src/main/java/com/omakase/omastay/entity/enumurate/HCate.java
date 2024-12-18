package com.omakase.omastay.entity.enumurate;
import lombok.Getter;

@Getter
public enum HCate {
    /*모텔
    * 호텔/리조트
    * 풀빌라
    * 게하/한옥
    * */
    MOTEL("모텔"),
    HOTEL_RESORT("호텔/리조트"),
    POOL_VILLA("펜션/풀빌라"),
    GUESTHOUSE_HANOK("게하/한옥");

    private final String description;

    HCate(String description) {
        this.description = description;
    }

}
