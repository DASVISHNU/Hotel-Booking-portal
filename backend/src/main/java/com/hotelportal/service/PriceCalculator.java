package com.hotelportal.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** All money is calculated here, on the server. The client only ever displays these numbers. */
public final class PriceCalculator {

    private PriceCalculator() {
    }

    public static BigDecimal total(BigDecimal pricePerNight, int nights) {
        if (pricePerNight == null || pricePerNight.signum() <= 0) {
            throw new IllegalArgumentException("Price per night must be positive");
        }
        if (nights <= 0) {
            throw new IllegalArgumentException("Nights must be positive");
        }
        return pricePerNight.multiply(BigDecimal.valueOf(nights)).setScale(2, RoundingMode.HALF_UP);
    }
}
