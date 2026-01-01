package com.alphaskyport.admin.model;

public enum IssueType {
    DELAY("delay"),
    DAMAGE("damage"),
    LOSS("loss"),
    WRONG_DELIVERY("wrong_delivery"),
    COMPLAINT("complaint"),
    DOCUMENTATION("documentation"),
    CUSTOMS("customs"),
    OTHER("other");

    private final String value;

    IssueType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
