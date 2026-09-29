package data;

import model.Booking;
import model.BookingDates;
import net.datafaker.Faker;
import org.junit.jupiter.params.provider.Arguments;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Фабрика тестовых данных для сущности {@link Booking} (restful-booker).
 *
 * <p>Принципы:
 * <ul>
 *   <li><b>Валидное по умолчанию</b> — {@link #validBooking()} даёт реалистичное
 *       случайное бронирование через Datafaker; тест меняет только то, что
 *       проверяет, через {@code validBooking().toBuilder().<поле>(...).build()}.</li>
 *   <li><b>Воспроизводимость</b> — {@link #reseed(long)} фиксирует seed генератора,
 *       чтобы прогон был детерминированным.</li>
 *   <li><b>Уникальность</b> — {@link #uniqueSuffix()} даёт короткий хвост из UUID,
 *       чтобы данные разных прогонов/потоков не путались в логах и отчётах.</li>
 *   <li><b>Метки покрытия</b> — провайдеры {@link #boundaryAndNegativeCases()}
 *       возвращают пары «что проверяем + объект» для параметризованных тестов.</li>
 * </ul>
 *
 * <p>restful-booker создаёт новую запись на каждый POST и не требует уникальных
 * значений, поэтому «убирать за собой» здесь = удалять созданный bookingid
 * (это уже делает TC-3.10). Одноразовость обеспечивает сам сервер.
 */
public final class BookingFactory {

    private static final DateTimeFormatter API_DATE = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd

    /** Общий генератор. По умолчанию случайный; {@link #reseed(long)} делает его детерминированным. */
    private static Faker faker = new Faker();

    private BookingFactory() {
    }

    /** Зафиксировать seed для воспроизводимого прогона. */
    public static synchronized void reseed(long seed) {
        faker = new Faker(new Random(seed));
    }

    // =====================================================================
    // ВАЛИДНЫЕ ТИПОВЫЕ  (класс эквивалентности: корректное бронирование)
    // =====================================================================

    /** Валидное случайное бронирование: реалистичные имена, цена 50..5000, корректные даты. */
    public static Booking validBooking() {
        LocalDate checkin = LocalDate.now().plusDays(faker.number().numberBetween(1, 30));
        LocalDate checkout = checkin.plusDays(faker.number().numberBetween(1, 14));
        return Booking.builder()
                .firstname(faker.name().firstName())
                .lastname(faker.name().lastName())
                .totalprice(faker.number().numberBetween(50, 5000))
                .depositpaid(faker.bool().bool())
                .bookingdates(new BookingDates(checkin.format(API_DATE), checkout.format(API_DATE)))
                .additionalneeds(faker.options().option("Breakfast", "Lunch", "Dinner", "None"))
                .build();
    }

    /**
     * Валидное бронирование с уникальным хвостом в фамилии — удобно, когда
     * несколько прогонов пишут в один общий тренажёр и надо различать записи.
     */
    public static Booking validUniqueBooking() {
        return validBooking().toBuilder()
                .lastname(faker.name().lastName() + "-" + uniqueSuffix())
                .build();
    }

    /** depositpaid = true (класс эквивалентности: депозит внесён). */
    public static Booking depositPaidTrue() {
        return validBooking().toBuilder().depositpaid(true).build();
    }

    /** depositpaid = false (класс эквивалентности: депозит не внесён). */
    public static Booking depositPaidFalse() {
        return validBooking().toBuilder().depositpaid(false).build();
    }

    // =====================================================================
    // ГРАНИЧНЫЕ  (min/max длины, 0, -1, максимум типа)
    // =====================================================================

    /** Граница длины: firstname = "" (пустая строка, длина 0). */
    public static Booking emptyFirstname() {
        return validBooking().toBuilder().firstname("").build();
    }

    /** Граница длины: lastname = "" (пустая строка, длина 0). */
    public static Booking emptyLastname() {
        return validBooking().toBuilder().lastname("").build();
    }

    /** Отличить пустую строку от строки из пробелов: firstname = "   ". */
    public static Booking blankFirstname() {
        return validBooking().toBuilder().firstname("   ").build();
    }

    /** Граница длины сверху: очень длинный firstname (10 000 символов). */
    public static Booking longFirstname() {
        return validBooking().toBuilder().firstname(longString(10_000)).build();
    }

    /** Граница длины сверху: очень длинный additionalneeds (10 000 символов). */
    public static Booking longAdditionalNeeds() {
        return validBooking().toBuilder().additionalneeds(longString(10_000)).build();
    }

    /** Граница числа: totalprice = 0. */
    public static Booking zeroPrice() {
        return validBooking().toBuilder().totalprice(0).build();
    }

    /** Негатив/граница: totalprice = -1 (отрицательная цена). */
    public static Booking negativePrice() {
        return validBooking().toBuilder().totalprice(-1).build();
    }

    /** Граница типа: totalprice = Integer.MAX_VALUE. */
    public static Booking maxPrice() {
        return validBooking().toBuilder().totalprice(Integer.MAX_VALUE).build();
    }

    // =====================================================================
    // НЕГАТИВНЫЕ  (спецсимволы, инъекции, юникод, кривые форматы, null)
    // =====================================================================

    /** Негатив: спецсимволы {@code <>'"&} в имени. */
    public static Booking specialCharsName() {
        return validBooking().toBuilder().firstname("<>'\"&").build();
    }

    /** Негатив: XSS-строка в имени (должна храниться как текст, не исполняться). */
    public static Booking xssName() {
        return validBooking().toBuilder().firstname("<script>alert('xss')</script>").build();
    }

    /** Негатив: SQL-инъекция в имени (должна храниться как текст). */
    public static Booking sqlInjectionName() {
        return validBooking().toBuilder().lastname("' OR 1=1 --").build();
    }

    /** Локаль/юникод: эмодзи, RTL-текст и кириллица/латиница вперемешку. */
    public static Booking unicodeNames() {
        return validBooking().toBuilder()
                .firstname("Иван‏مرحبا😀")   // кириллица + арабский (RTL) + эмодзи
                .lastname("O'Ivan-Иваноff")                  // апостроф, дефис, лат/кир вперемешку
                .build();
    }

    /** Локаль: реалистичное ФИО кириллицей с дефисом и апострофом (синтетика). */
    public static Booking cyrillicNames() {
        return validBooking().toBuilder()
                .firstname("Айгуль")
                .lastname("Осмон-кызы")
                .build();
    }

    /** Негатив дат: checkout РАНЬШЕ checkin (нарушение контракта). */
    public static Booking checkoutBeforeCheckin() {
        LocalDate checkin = LocalDate.now().plusDays(10);
        LocalDate checkout = checkin.minusDays(5);
        return validBooking().toBuilder()
                .bookingdates(new BookingDates(checkin.format(API_DATE), checkout.format(API_DATE)))
                .build();
    }

    /** Негатив дат: неверный формат даты (dd/MM/yyyy вместо yyyy-MM-dd). */
    public static Booking wrongDateFormat() {
        return validBooking().toBuilder()
                .bookingdates(new BookingDates("15/07/2026", "20/07/2026"))
                .build();
    }

    /**
     * Негатив: неполное тело — задан только firstname, остальные поля null.
     * В JSON null-поля опускаются, т.е. тело = {@code {"firstname":"..."}}.
     * Ожидаемый ответ restful-booker — 500 (см. TC-3.8).
     */
    public static Booking incompleteBody() {
        return Booking.builder()
                .firstname(faker.name().firstName())
                .build();
    }

    // =====================================================================
    // ПРОВАЙДЕРЫ ДЛЯ @MethodSource  (метка «что проверяет» + объект)
    // =====================================================================

    /**
     * Набор кейсов для параметризованного теста: классы эквивалентности,
     * границы и негативные данные. Первый аргумент — человекочитаемая метка
     * (она же имя прогона в отчёте), второй — сам {@link Booking}.
     * Подключён в {@code api.RestfulBookerNegativeTest}; добавить кейс — это
     * одна строка здесь, править тест не нужно.
     */
    public static Stream<Arguments> boundaryAndNegativeCases() {
        return Stream.of(
                Arguments.of("Класс эквивалентности: depositpaid = true", depositPaidTrue()),
                Arguments.of("Класс эквивалентности: depositpaid = false", depositPaidFalse()),
                Arguments.of("Граница: пустой firstname (len=0)", emptyFirstname()),
                Arguments.of("Граница: пустой lastname (len=0)", emptyLastname()),
                Arguments.of("Граница: firstname из пробелов", blankFirstname()),
                Arguments.of("Граница: firstname 10k символов", longFirstname()),
                Arguments.of("Граница: additionalneeds 10k символов", longAdditionalNeeds()),
                Arguments.of("Граница: totalprice = 0", zeroPrice()),
                Arguments.of("Негатив: totalprice = -1", negativePrice()),
                Arguments.of("Граница: totalprice = Integer.MAX_VALUE", maxPrice()),
                Arguments.of("Негатив: спецсимволы <>'\"& в имени", specialCharsName()),
                Arguments.of("Негатив: XSS в имени", xssName()),
                Arguments.of("Негатив: SQL-инъекция в фамилии", sqlInjectionName()),
                Arguments.of("Локаль: эмодзи + RTL + кир/лат", unicodeNames()),
                Arguments.of("Локаль: кириллица с дефисом", cyrillicNames()),
                Arguments.of("Негатив дат: checkout < checkin", checkoutBeforeCheckin()),
                Arguments.of("Негатив дат: формат dd/MM/yyyy", wrongDateFormat())
        );
    }

    // =====================================================================
    // ХЕЛПЕРЫ
    // =====================================================================

    /** Короткий уникальный суффикс из UUID (8 символов). */
    public static String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /** Строка из {@code n} символов 'a' для проверки границ длины. */
    public static String longString(int n) {
        return "a".repeat(n);
    }
}
