package com.briefix.user.dto;

public record UpdateBillingRequest(
        String billingName,
        String billingStreet,
        String billingStreetNo,
        String billingZip,
        String billingCity,
        String billingCountry
) {}
