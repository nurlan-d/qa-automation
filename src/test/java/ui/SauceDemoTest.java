package ui;

import data.SauceDemoUser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * UI-тесты Swag Labs (https://www.saucedemo.com): happy-path под здорового
 * пользователя standard_user плюс негативные проверки формы логина.
 * Все они должны быть ЗЕЛЁНЫМИ — сломанные персоны живут в
 * {@link ProblemUserRegressionTest}.
 *
 * <p>Учётки берутся из {@link SauceDemoUser}, а не пишутся строками: пароль
 * задан в одном месте, а имя персоны в тесте читается как намерение
 * («вошли заблокированным»), а не как магическая строка.
 */
class SauceDemoTest extends PlaywrightBase {

    @Test
    @DisplayName("TC-1.1 Успешный вход standard_user -> каталог из 6 товаров")
    void loginSuccess() {
        login(SauceDemoUser.STANDARD);
        // assertThat из Playwright ждёт, пока условие выполнится (до 5 с).
        // Голый count() считает мгновенно: URL уже сменился, а товары ещё не
        // отрисованы — тест флакал с «expected 6 but was 0».
        assertThat(page).hasURL(Pattern.compile(".*/inventory\\.html"));
        assertThat(page.locator(".inventory_item")).hasCount(6);
    }

    @Test
    @DisplayName("TC-1.2 Заблокированный пользователь -> ошибка блокировки")
    void lockedOut() {
        login(SauceDemoUser.LOCKED_OUT);
        String error = page.locator("[data-test=error]").textContent();
        assertTrue(error.contains("locked out"), "ожидали сообщение о блокировке");
    }

    /**
     * TC-1.4 и соседние негативные кейсы формы логина одним параметризованным
     * тестом. Раньше здесь был один тест на пустой логин; наборы данных живут
     * в {@link SauceDemoUser#invalidLoginCases()}, поэтому добавить кейс —
     * это одна строка в провайдере, а не новый метод здесь.
     */
    @ParameterizedTest(name = "TC-1.4 Негативный вход: ожидаем «{2}»")
    @MethodSource("data.SauceDemoUser#invalidLoginCases")
    @DisplayName("TC-1.4 Негативные сценарии формы логина")
    void loginErrors(String user, String pass, String expectedError) {
        login(user, pass);
        String error = page.locator("[data-test=error]").textContent();
        assertTrue(error.contains(expectedError),
                "ожидали ошибку «" + expectedError + "», получили «" + error + "»");
    }

    @Test
    @DisplayName("TC-1.6 Добавить товар в корзину -> бейдж = 1")
    void addToCart() {
        login(SauceDemoUser.STANDARD);
        page.click("[data-test='add-to-cart-sauce-labs-backpack']");
        assertEquals("1", page.locator(".shopping_cart_badge").textContent());
    }

    @Test
    @DisplayName("TC-1.10 Сортировка по цене (low to high) -> порядок по возрастанию")
    void sortByPrice() {
        login(SauceDemoUser.STANDARD);
        page.selectOption(".product_sort_container", "lohi");
        // Сначала дождаться цен: allTextContents() не ждёт, и на пустом списке
        // проверка сортировки прошла бы ложно (пустой список «отсортирован»).
        assertThat(page.locator(".inventory_item_price")).hasCount(6);

        List<Double> prices = page.locator(".inventory_item_price").allTextContents()
                .stream()
                .map(p -> Double.parseDouble(p.replace("$", "").trim()))
                .collect(Collectors.toList());

        List<Double> expected = new ArrayList<>(prices);
        Collections.sort(expected);
        assertEquals(expected, prices, "цены должны идти по возрастанию");
    }

    @Test
    @DisplayName("TC-1.8 Полный чекаут standard_user -> Thank you for your order")
    void fullCheckout() {
        login(SauceDemoUser.STANDARD);
        page.click("[data-test='add-to-cart-sauce-labs-backpack']");
        page.click(".shopping_cart_link");
        page.click("[data-test=checkout]");
        page.fill("[data-test=firstName]", "Ivan");
        page.fill("[data-test=lastName]", "Petrov");
        page.fill("[data-test=postalCode]", "720000");
        page.click("[data-test=continue]");
        page.click("[data-test=finish]");

        String header = page.locator(".complete-header").textContent();
        assertTrue(header.toLowerCase().contains("thank you"), "заказ должен оформиться");
    }
}
