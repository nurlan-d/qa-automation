package api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * API-тесты Restful-Booker (https://restful-booker.herokuapp.com).
 * Полный CRUD с авторизацией. Порядок важен: create -> get -> auth -> update -> delete,
 * поэтому используем @TestMethodOrder.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RestfulBookerApiTest {

    private static int bookingId;
    private static String token;

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
        String body = "{"
                + "\"firstname\":\"Ivan\","
                + "\"lastname\":\"Petrov\","
                + "\"totalprice\":150,"
                + "\"depositpaid\":true,"
                + "\"bookingdates\":{\"checkin\":\"2026-07-10\",\"checkout\":\"2026-07-15\"},"
                + "\"additionalneeds\":\"Breakfast\""
                + "}";

        bookingId = given()
                .contentType(ContentType.JSON)
                .body(body)
        .when()
                .post("/booking")
        .then()
                .statusCode(200)
                .body("bookingid", notNullValue())
                .body("booking.firstname", equalTo("Ivan"))
                .body("booking.lastname", equalTo("Petrov"))
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
                .body("firstname", equalTo("Ivan"));
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
        String body = "{"
                + "\"firstname\":\"Ivan\","
                + "\"lastname\":\"Ivanov\","
                + "\"totalprice\":200,"
                + "\"depositpaid\":false,"
                + "\"bookingdates\":{\"checkin\":\"2026-08-01\",\"checkout\":\"2026-08-05\"},"
                + "\"additionalneeds\":\"Lunch\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .header("Cookie", "token=" + token)
                .body(body)
        .when()
                .put("/booking/" + bookingId)
        .then()
                .statusCode(200)
                .body("lastname", equalTo("Ivanov"));
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
                .body("{\"firstname\":\"OnlyName\"}")
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
