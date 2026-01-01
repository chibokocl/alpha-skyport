package com.alphaskyport.admin.model;

public enum AdjustmentType {
    PERCENTAGE("percentage"),
    FIXED("fixed"),
    SET_PRICE("set_price");

    private final String value;

    AdjustmentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
