package com.hotelportal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Sent as a POST body (not query params) so guest emails don't end up in URLs and access logs. */
public record BookingLookupRequest(
        @NotBlank(message = "Booking reference is required.")
        String reference,

        @NotBlank(message = "Email is required.")
        @Email(message = "Please enter a valid email address.")
        String email
) {
}
