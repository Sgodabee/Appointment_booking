package com.appointmentbooking.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


public enum ServiceType {

    ACCOUNT_OPENING("Account Opening"),
    LOAN_APPLICATION("Loan Application"),
    CARD_SERVICES("Card Services"),
    GENERAL_ENQUIRY("General Enquiry"),
    DOCUMENT_SUBMISSION("Document Submission"),
    INVESTMENT_ADVICE("Investment Advice");

    private final String displayName;

    ServiceType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }


    @JsonCreator
    public static ServiceType fromString(String value) {
        for (ServiceType st : values()) {
            if (st.name().equalsIgnoreCase(value) || st.displayName.equalsIgnoreCase(value)) {
                return st;
            }
        }
        throw new IllegalArgumentException("Unknown service type: " + value);
    }
}
