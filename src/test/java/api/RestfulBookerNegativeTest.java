package api;

import data.BookingFactory;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import model.Booking;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

/**
 * Граничные и негативные наборы данных для POST /booking — по одному прогону
 * на каждый класс эквивалентности из {@link BookingFactory#boundaryAndNegativeCases()}:
 * пустые и очень длинные строки, 0 / -1 / Integer.MAX_VALUE в цене, спецсимволы,
 * XSS, SQL-инъекция, юникод и кириллица, перевёрнутые и криво отформатированные даты.
 *
 * <p>Вынесено из {@link RestfulBookerApiTest}: там строгий порядок
 * (create → get → auth → update → delete) и общий bookingId, и параметризованные
 * прогоны эту цепочку бы засоряли.
 *
 * <p><b>Честно о силе проверки.</b> Это характеризующий тест: у публичного
 * тренажёра restful-booker нет спецификации, которая говорила бы, что он
 * ОБЯЗАН отвечать на пустое имя. Поэтому фиксируется факт — ответ приходит и
 * он один из документированных (200/400/500). Такой тест не поймает «сервис
 * молча принял мусор», но поймает то, ради чего он и нужен на этом этапе:
 * падение сервиса, таймаут, 502 или необработанное исключение на конкретном
 * наборе данных. Как только появится спека, `anyOf` заменяется на точный код
 * по каждому кейсу — сами наборы данных при этом менять не придётся.
 */
class RestfulBookerNegativeTest {

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "https://restful-booker.herokuapp.com";
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data.BookingFactory#boundaryAndNegativeCases")
    @DisplayName("TC-3.11 POST /booking на граничных и негативных данных")
    void createBookingWithEdgeCaseData(String whatWeCheck, Booking booking) {
        given()
                .contentType(ContentType.JSON)
                .body(booking.toJson())
        .when()
                .post("/booking")
        .then()
                .statusCode(anyOf(is(200), is(400), is(500)));
    }
}
