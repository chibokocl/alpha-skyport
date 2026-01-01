package com.alphaskyport.admin.model;

public enum PaymentMethod {
    BANK_TRANSFER("bank_transfer"),
    CREDIT_CARD("credit_card"),
    DEBIT_CARD("debit_card"),
    CASH("cash"),
    CHECK("check"),
    MOBILE_MONEY("mobile_money"),
    OTHER("other");

    private final String value;

    PaymentMethod(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
