# Фабрика тестовых данных — памятка

Что добавлено в проект (всё в `src/test/java`, scope `test`):

| Файл | Пакет | Роль |
|------|-------|------|
| `model/Booking.java` | `model` | POJO бронирования restful-booker + `toJson()` |
| `model/BookingDates.java` | `model` | вложенный `bookingdates{checkin,checkout}` |
| `data/BookingFactory.java` | `data` | генератор бронирований (Datafaker) |
| `data/SauceDemoUser.java` | `data` | фиксированные учётки Swag Labs |
| `pom.xml` | — | добавлена зависимость `net.datafaker:datafaker:2.7.0` |

---

## 0. Зависимость

```xml
<dependency>
    <groupId>net.datafaker</groupId>
    <artifactId>datafaker</artifactId>
    <version>2.7.0</version>
    <scope>test</scope>
</dependency>
```

Datafaker 2.x требует **Java 17+** — у проекта `maven.compiler.release=17`, всё ок.
После правки pom обнови зависимости в IDE (Maven → Reload) — Maven в консоли не нужен.

**Почему у модели есть свой `toJson()`, а не сериализация POJO.**
В pom нет Jackson/Gson, поэтому REST Assured `.body(pojoObject)` работать не будет —
ему нужен object mapper. `Booking.toJson()` строит тело сам и корректно экранирует
кавычки/спецсимволы, поэтому даже `' OR 1=1 --` и `<script>` остаются валидным JSON.
Поля `null` в JSON **опускаются** — это и даёт «неполное тело» для теста на 500.
Если позже добавишь Jackson — сможешь передавать объект напрямую, `toJson()` не помешает.

---

## 1. Booking → какой метод фабрики для чего

Все методы возвращают `Booking`; тело запроса — `booking.toJson()`.

| Метод `BookingFactory.*` | Класс / граница | Что проверяет |
|--------------------------|-----------------|---------------|
| `validBooking()` | валидный типовой | happy-path create/update (TC-3.1, TC-3.6) |
| `validUniqueBooking()` | валидный + уникальный хвост | различимость записей в общем тренажёре |
| `depositPaidTrue()` / `depositPaidFalse()` | класс эквивалентности boolean | оба значения depositpaid |
| `emptyFirstname()` / `emptyLastname()` | граница длины = 0 | пустая строка в обяз. поле |
| `blankFirstname()` | граница | пробелы ≠ пустая строка |
| `longFirstname()` / `longAdditionalNeeds()` | граница длины (10k) | очень длинная строка |
| `zeroPrice()` | граница числа = 0 | нулевая цена |
| `negativePrice()` | негатив = -1 | отрицательная цена |
| `maxPrice()` | граница типа | `Integer.MAX_VALUE` |
| `specialCharsName()` | негатив | спецсимволы `<>'"&` |
| `xssName()` | негатив | XSS-строка хранится как текст |
| `sqlInjectionName()` | негатив | SQL-инъекция `' OR 1=1 --` |
| `unicodeNames()` | локаль/юникод | эмодзи + RTL + кир/лат |
| `cyrillicNames()` | локаль | ФИО кириллицей с дефисом |
| `checkoutBeforeCheckin()` | негатив дат | checkout раньше checkin |
| `wrongDateFormat()` | негатив дат | формат `dd/MM/yyyy` |
| `incompleteBody()` | негатив | неполное тело → ожидаем 500 (TC-3.8) |

Меняешь только проверяемое поле — через копирующий билдер:

```java
Booking b = BookingFactory.validBooking().toBuilder()
        .totalprice(0)          // проверяем только цену
        .build();
```

Воспроизводимость / уникальность:
```java
BookingFactory.reseed(42L);              // детерминированный прогон
String tag = BookingFactory.uniqueSuffix(); // хвост из UUID для трассировки
```

---

## 2. Как заменить захардкоженное тело в `RestfulBookerApiTest` (пример «до/после»)

Существующие тесты **не трогаем** — это только образец, как подключить фабрику.

**Было** (TC-3.1, строки 40–61):

```java
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
```

**Стало** (значения из фабрики, проверки — против того же объекта):

```java
import model.Booking;
import data.BookingFactory;

Booking booking = BookingFactory.validBooking();

bookingId = given()
        .contentType(ContentType.JSON)
        .body(booking.toJson())
.when()
        .post("/booking")
.then()
        .statusCode(200)
        .body("bookingid", notNullValue())
        .body("booking.firstname", equalTo(booking.getFirstname()))
        .body("booking.lastname", equalTo(booking.getLastname()))
        .extract().path("bookingid");
```

Плюс: захардкоженное `«Ivan/Petrov»` больше не находит нулевой набор багов —
каждый прогон гоняет новые реалистичные данные, а негативные наборы (`xssName()`,
`checkoutBeforeCheckin()`, `incompleteBody()`) можно вынести в параметризованный тест:

```java
@ParameterizedTest(name = "{0}")
@MethodSource("data.BookingFactory#boundaryAndNegativeCases")
void createNegative(String whatWeCheck, Booking booking) {
    given().contentType(ContentType.JSON).body(booking.toJson())
    .when().post("/booking")
    .then().statusCode(anyOf(is(200), is(400), is(500))); // фиксируем факт. поведение
}
```

---

## 3. SauceDemo → `SauceDemoUser`

Фиксированные учётки тренажёра (не удалять, не генерировать). Пароль у всех
`SauceDemoUser.PASSWORD` = `secret_sauce`.

Точечно вместо строк:
```java
login(SauceDemoUser.STANDARD.username(), SauceDemoUser.STANDARD.password());
login(SauceDemoUser.PROBLEM.username(),  SauceDemoUser.PROBLEM.password());
```

Параметризованный логин по всем персонам:
```java
@ParameterizedTest(name = "{1}")
@MethodSource("data.SauceDemoUser#allUsers")
void loginAllUsers(SauceDemoUser user, String whatWeCheck) {
    login(user.username(), user.password());
    // ...ассерты под ожидаемое поведение персоны...
}
```

Негативные данные формы логина:
```java
@ParameterizedTest
@MethodSource("data.SauceDemoUser#invalidLoginCases")
void loginErrors(String user, String pass, String expectedError) {
    page.navigate("https://www.saucedemo.com/");
    page.fill("#user-name", user);
    page.fill("#password", pass);
    page.click("#login-button");
    assertTrue(page.locator("[data-test=error]").textContent().contains(expectedError));
}
```

Где применимо:
- `SauceDemoTest` — `STANDARD` (happy-path), `LOCKED_OUT` (блокировка), `invalidLoginCases()` (пустой логин и пр.).
- `ProblemUserRegressionTest` — `PROBLEM` в `@BeforeEach` вместо строки `"problem_user"`.

---

## 4. Про телефоны/адреса (на будущее)

У `Booking` полей телефона/адреса нет. Когда появится сущность с телефоном,
местный формат через Datafaker, например КГ (+996):

```java
Faker f = new Faker();
String phoneKg = "+996 " + f.numerify("### ## ## ##");
```

Для email — `f.internet().emailAddress()`; негатив — `"user-without-at.example.com"`,
`"a@"`, очень длинный local-part.
