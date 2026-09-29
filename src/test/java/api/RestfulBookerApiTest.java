package api;

import data.BookingFactory;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import model.Booking;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * API-тесты Restful-Booker (https://restful-booker.herokuapp.com).
 * Полный CRUD с авторизацией. Порядок важен: create -> get -> auth -> update -> delete,
 * поэтому используем @TestMethodOrder.
 *
 * <p>Тела запросов берутся из {@link BookingFactory}, а не пишутся строкой.
 * Захардкоженные «Ivan/Petrov» каждый прогон проверяли ровно один набор данных;
 * фабрика даёт новые реалистичные значения, а проверки идут против того же
 * объекта, который отправили. Граничные и негативные наборы вынесены в
 * {@link RestfulBookerNegativeTest}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RestfulBookerApiTest {

    private static int bookingId;
    private static String token;

    /** Что именно создали в TC-3.1 — против него проверяем в TC-3.3. */
    private static Booking created;

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "https://restful-booker.herokuapp.com";
    }

    @Test
    @Order(1)
    @DisplayName("TC-3.5 Healthcheck GET /ping -> 201")
    void ping() {
        given()
        .when()
                .get("/ping")
        .then()
                .statusCode(201);
    }

    @Test
    @Order(2)
    @DisplayName("TC-3.1 Создать бронирование -> 200 + bookingid")
    void createBooking() {
        created = BookingFactory.validUniqueBooking();

        bookingId = given()
                .contentType(ContentType.JSON)
                .body(created.toJson())
        .when()
                .post("/booking")
        .then()
                .statusCode(200)
                .body("bookingid", notNullValue())
                .body("booking.firstname", equalTo(created.getFirstname()))
                .body("booking.lastname", equalTo(created.getLastname()))
                .body("booking.totalprice", equalTo(created.getTotalprice()))
                .extract().path("bookingid");
    }

    @Test
    @Order(3)
    @DisplayName("TC-3.2 Список бронирований -> 200")
    void listBookings() {
        given()
        .when()
                .get("/booking")
        .then()
                .statusCode(200)
                .body("bookingid", not(empty()));
    }

    @Test
    @Order(4)
    @DisplayName("TC-3.3 Получить бронирование по id -> 200")
    void getBookingById() {
        given()
        .when()
                .get("/booking/" + bookingId)
        .then()
                .statusCode(200)
                .body("firstname", equalTo(created.getFirstname()))
                .body("lastname", equalTo(created.getLastname()));
    }

    @Test
    @Order(5)
    @DisplayName("TC-3.4 Получить токен POST /auth -> 200 + token")
    void auth() {
        token = given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"admin\",\"password\":\"password123\"}")
        .when()
                .post("/auth")
        .then()
                .statusCode(200)
                .body("token", notNullValue())
                .extract().path("token");
    }

    @Test
    @Order(6)
    @DisplayName("TC-3.6 Обновить бронирование с токеном -> 200")
    void updateWithToken() {
        Booking updated = BookingFactory.validUniqueBooking();

        given()
                .contentType(ContentType.JSON)
                .header("Cookie", "token=" + token)
                .body(updated.toJson())
        .when()
                .put("/booking/" + bookingId)
        .then()
                .statusCode(200)
                .body("firstname", equalTo(updated.getFirstname()))
                .body("lastname", equalTo(updated.getLastname()));
    }

    @Test
    @Order(7)
    @DisplayName("TC-3.7 Обновить БЕЗ токена -> 403 Forbidden")
    void updateWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"firstname\":\"X\",\"lastname\":\"Y\",\"totalprice\":1,\"depositpaid\":true,"
                        + "\"bookingdates\":{\"checkin\":\"2026-01-01\",\"checkout\":\"2026-01-02\"}}")
        .when()
                .put("/booking/" + bookingId)
        .then()
                .statusCode(403);
    }

    @Test
    @Order(8)
    @DisplayName("TC-3.8 Создать с неполным телом -> 500")
    void createIncomplete() {
        given()
                .contentType(ContentType.JSON)
                .body(BookingFactory.incompleteBody().toJson())
        .when()
                .post("/booking")
        .then()
                .statusCode(500);
    }

    @Test
    @Order(9)
    @DisplayName("TC-3.9 Запросить несуществующий id -> 404")
    void notFound() {
        given()
        .when()
                .get("/booking/99999999")
        .then()
                .statusCode(404);
    }

    @Test
    @Order(10)
    @DisplayName("TC-3.10 Удалить с токеном -> 201, повторный GET -> 404")
    void deleteBooking() {
        given()
                .header("Cookie", "token=" + token)
        .when()
                .delete("/booking/" + bookingId)
        .then()
                .statusCode(201);

        given()
        .when()
                .get("/booking/" + bookingId)
        .then()
                .statusCode(404);
    }
}
