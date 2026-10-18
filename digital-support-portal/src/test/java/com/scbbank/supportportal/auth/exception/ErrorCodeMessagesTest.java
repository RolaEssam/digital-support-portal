package com.scbbank.supportportal.auth.exception;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;


class ErrorCodeMessagesTest {

    @Test
    void everyErrorCodeHasMessage() {
        ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
        ms.setBasename("messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setUseCodeAsDefaultMessage(false);

        for (ErrorCode ec : ErrorCode.values()) {
            String msg = assertDoesNotThrow(
                    () -> ms.getMessage(ec.getMessageKey(), null, Locale.ENGLISH),
                    "Missing message for " + ec.name() + " (" + ec.getMessageKey() + ")");
            assertFalse(msg.isBlank(), "Blank message for " + ec.name());
        }
    }
}