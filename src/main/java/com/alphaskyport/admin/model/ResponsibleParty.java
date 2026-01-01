package com.alphaskyport.admin.model;

public enum ResponsibleParty {
    CARRIER("carrier"),
    WAREHOUSE("warehouse"),
    CUSTOMER("customer"),
    INTERNAL("internal"),
    OTHER("other");

    private final String value;

    ResponsibleParty(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
