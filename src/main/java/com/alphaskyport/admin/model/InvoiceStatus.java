package com.alphaskyport.admin.model;

public enum InvoiceStatus {
    DRAFT("draft"),
    SENT("sent"),
    PAID("paid"),
    PARTIAL("partial"),
    OVERDUE("overdue"),
    CANCELLED("cancelled"),
    REFUNDED("refunded");

    private final String value;

    InvoiceStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static InvoiceStatus fromValue(String value) {
        for (InvoiceStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown invoice status: " + value);
    }
}
