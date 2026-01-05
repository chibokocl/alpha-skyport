package com.alphaskyport.admin.model;

public enum AdjustmentType {
    PERCENTAGE("percentage"),
    FIXED("fixed"),
    SET_PRICE("set_price"),
    BASE_RATE_PER_KG("base_rate_per_kg"),
    MULTIPLIER("multiplier");

    private final String value;

    AdjustmentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
