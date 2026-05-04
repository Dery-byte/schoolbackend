package com.alibou.book.email;

import lombok.Getter;

@Getter
public enum EmailTemplateName {
    ACCOUNT_ACTIVATION("activate_account"),
    PAYMENT_CONFIRMATION("payment_success"),
    RESET_PASSWORD("reset_password"),
    ADMIN_MESSAGE("admin_message"),
    DISCOUNT_ASSIGNED("discount_assigned");


    private final String name;
    EmailTemplateName(String name) {
        this.name = name;
    }
}
