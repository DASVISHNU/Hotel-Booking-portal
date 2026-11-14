package com.hotelportal.service;

import com.hotelportal.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure unit tests for the date rules and price calculation (no Spring, no database). */
class StayDatesAndPricingTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);

    @Test
    void totalIsPricePerNightTimesNights() {
        StayDates stay = StayDates.validate(TODAY.plusDays(1), TODAY.plusDays(4), TODAY);

        assertThat(stay.nights()).isEqualTo(3);
        assertThat(PriceCalculator.total(new BigDecimal("4500.00"), stay.nights()))
                .isEqualByComparingTo("13500.00");
    }

    @Test
    void totalKeepsPaiseAndTwoDecimalPlaces() {
        BigDecimal total = PriceCalculator.total(new BigDecimal("3333.33"), 3);

        assertThat(total).isEqualByComparingTo("9999.99");
        assertThat(total.scale()).isEqualTo(2);
    }

    @Test
    void nightsExcludeTheCheckOutDay() {
        StayDates stay = StayDates.validate(TODAY.plusDays(2), TODAY.plusDays(4), TODAY);

        assertThat(stay.nightDates()).containsExactly(TODAY.plusDays(2), TODAY.plusDays(3));
    }

    @Test
    void checkInTodayIsAllowed() {
        StayDates stay = StayDates.validate(TODAY, TODAY.plusDays(1), TODAY);

        assertThat(stay.nights()).isEqualTo(1);
    }

    @Test
    void rejectsCheckInInThePast() {
        assertThatThrownBy(() -> StayDates.validate(TODAY.minusDays(1), TODAY.plusDays(1), TODAY))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("CHECK_IN_IN_PAST");
    }

    @Test
    void rejectsCheckOutOnSameDayAsCheckIn() {
        assertThatThrownBy(() -> StayDates.validate(TODAY.plusDays(3), TODAY.plusDays(3), TODAY))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("INVALID_DATE_RANGE");
    }

    @Test
    void rejectsCheckOutBeforeCheckIn() {
        assertThatThrownBy(() -> StayDates.validate(TODAY.plusDays(5), TODAY.plusDays(2), TODAY))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("INVALID_DATE_RANGE");
    }

    @Test
    void rejectsMissingDates() {
        assertThatThrownBy(() -> StayDates.validate(null, TODAY.plusDays(2), TODAY))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("DATES_REQUIRED");
    }

    @Test
    void rejectsStaysLongerThanTheMaximum() {
        assertThatThrownBy(() -> StayDates.validate(TODAY, TODAY.plusDays(StayDates.MAX_NIGHTS + 1), TODAY))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("STAY_TOO_LONG");
    }

    @Test
    void priceCalculatorRejectsZeroNights() {
        assertThatThrownBy(() -> PriceCalculator.total(new BigDecimal("1000"), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
