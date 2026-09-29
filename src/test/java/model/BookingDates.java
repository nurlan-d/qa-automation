package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Вложенный объект {@code bookingdates} restful-booker.
 *
 * <p>Даты — строки (не {@link java.time.LocalDate}), чтобы можно было подавать
 * не только корректный формат {@code yyyy-MM-dd}, но и заведомо «кривые»
 * значения ({@code 15/07/2026}, {@code not-a-date}) как негативные кейсы.
 * По контракту API {@code checkout} должен быть строго позже {@code checkin}.
 */
public class BookingDates {

    private String checkin;
    private String checkout;

    public BookingDates() {
    }

    public BookingDates(String checkin, String checkout) {
        this.checkin = checkin;
        this.checkout = checkout;
    }

    public String getCheckin() {
        return checkin;
    }

    public void setCheckin(String checkin) {
        this.checkin = checkin;
    }

    public String getCheckout() {
        return checkout;
    }

    public void setCheckout(String checkout) {
        this.checkout = checkout;
    }

    public String toJson() {
        List<String> parts = new ArrayList<>();
        if (checkin != null) {
            parts.add("\"checkin\":" + Booking.quote(checkin));
        }
        if (checkout != null) {
            parts.add("\"checkout\":" + Booking.quote(checkout));
        }
        return "{" + String.join(",", parts) + "}";
    }

    @Override
    public String toString() {
        return toJson();
    }
}
