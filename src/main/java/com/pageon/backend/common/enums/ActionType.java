package com.pageon.backend.common.enums;

import lombok.Getter;

@Getter
public enum ActionType {
    VIEW(1),
    COMMENT(3),
    RATING(1),
    INTEREST(5),
    RENTAL(10),
    PURCHASE(20);

    private final int score;

    ActionType(int score) {
        this.score = score;
    }

}
