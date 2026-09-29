package model;

import java.util.ArrayList;
import java.util.List;

/**
 * POJO-модель бронирования под JSON restful-booker
 * (https://restful-booker.herokuapp.com).
 *
 * <p>Имена полей — СТРОГО как в API, в нижнем регистре:
 * firstname, lastname, totalprice, depositpaid, bookingdates, additionalneeds.
 *
 * <p>Осознанные отступления от ТЗ (int/boolean) ради тестируемости:
 * <ul>
 *   <li>{@code totalprice} — {@link Integer}, {@code depositpaid} — {@link Boolean}
 *       (боксированные), чтобы поле можно было выставить в {@code null}
 *       для негативного кейса «неполное тело -&gt; 500»;</li>
 *   <li>даты внутри {@link BookingDates} — строки, чтобы можно было подавать
 *       и заведомо «кривые форматы» дат как негативный кейс.</li>
 * </ul>
 *
 * <p>Сериализация: в pom нет Jackson/Gson, поэтому REST Assured не умеет сам
 * превратить объект в JSON. Метод {@link #toJson()} строит тело запроса сам и
 * корректно экранирует кавычки/спецсимволы/управляющие символы — благодаря
 * этому даже строки вида {@code ' OR 1=1 --} и {@code <script>} остаются
 * синтаксически валидным JSON. Поля со значением {@code null} в JSON НЕ
 * попадают (это и даёт «неполное тело»).
 */
public class Booking {

    private String firstname;
    private String lastname;
    private Integer totalprice;
    private Boolean depositpaid;
    private BookingDates bookingdates;
    private String additionalneeds;

    public Booking() {
    }

    // --- getters / setters (bean-стиль: пригодится, если позже добавят Jackson) ---

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public Integer getTotalprice() {
        return totalprice;
    }

    public void setTotalprice(Integer totalprice) {
        this.totalprice = totalprice;
    }

    public Boolean getDepositpaid() {
        return depositpaid;
    }

    public void setDepositpaid(Boolean depositpaid) {
        this.depositpaid = depositpaid;
    }

    public BookingDates getBookingdates() {
        return bookingdates;
    }

    public void setBookingdates(BookingDates bookingdates) {
        this.bookingdates = bookingdates;
    }

    public String getAdditionalneeds() {
        return additionalneeds;
    }

    public void setAdditionalneeds(String additionalneeds) {
        this.additionalneeds = additionalneeds;
    }

    // --- сериализация в JSON restful-booker ---

    /** Тело запроса для restful-booker. Поля {@code null} опускаются. */
    public String toJson() {
        List<String> parts = new ArrayList<>();
        if (firstname != null) {
            parts.add("\"firstname\":" + quote(firstname));
        }
        if (lastname != null) {
            parts.add("\"lastname\":" + quote(lastname));
        }
        if (totalprice != null) {
            parts.add("\"totalprice\":" + totalprice);
        }
        if (depositpaid != null) {
            parts.add("\"depositpaid\":" + depositpaid);
        }
        if (bookingdates != null) {
            parts.add("\"bookingdates\":" + bookingdates.toJson());
        }
        if (additionalneeds != null) {
            parts.add("\"additionalneeds\":" + quote(additionalneeds));
        }
        return "{" + String.join(",", parts) + "}";
    }

    /** JSON-экранирование строки. Package-private — переиспользуется в {@link BookingDates}. */
    static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        // кириллица, эмодзи, RTL и пр. — валидный UTF-8, отдаём как есть
                        sb.append(c);
                    }
            }
        }
        return sb.append("\"").toString();
    }

    @Override
    public String toString() {
        return toJson();
    }

    // --- builder ---

    public static Builder builder() {
        return new Builder();
    }

    /** Копирующий билдер: взять валидный объект и поменять ТОЛЬКО проверяемое поле. */
    public Builder toBuilder() {
        return new Builder()
                .firstname(firstname)
                .lastname(lastname)
                .totalprice(totalprice)
                .depositpaid(depositpaid)
                .bookingdates(bookingdates)
                .additionalneeds(additionalneeds);
    }

    public static final class Builder {
        private final Booking b = new Booking();

        public Builder firstname(String v) {
            b.firstname = v;
            return this;
        }

        public Builder lastname(String v) {
            b.lastname = v;
            return this;
        }

        public Builder totalprice(Integer v) {
            b.totalprice = v;
            return this;
        }

        public Builder depositpaid(Boolean v) {
            b.depositpaid = v;
            return this;
        }

        public Builder bookingdates(BookingDates v) {
            b.bookingdates = v;
            return this;
        }

        public Builder dates(String checkin, String checkout) {
            b.bookingdates = new BookingDates(checkin, checkout);
            return this;
        }

        public Builder additionalneeds(String v) {
            b.additionalneeds = v;
            return this;
        }

        public Booking build() {
            return b;
        }
    }
}
