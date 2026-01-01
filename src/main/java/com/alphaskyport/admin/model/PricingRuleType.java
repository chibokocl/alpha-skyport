package com.alphaskyport.admin.model;

public enum PricingRuleType {
    ROUTE("route"),
    WEIGHT("weight"),
    VOLUME("volume"),
    CUSTOMER("customer"),
    TIME("time"),
    PROMOTIONAL("promotional");

    private final String value;

    PricingRuleType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
