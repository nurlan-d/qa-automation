package ui;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI-тесты Swag Labs (https://www.saucedemo.com) под здорового пользователя standard_user.
 * Все они должны быть ЗЕЛЁНЫМИ — это проверка happy-path основных сценариев магазина.
 */
class SauceDemoTest extends PlaywrightBase {

    @Test
    @DisplayName("TC-1.1 Успешный вход standard_user -> каталог из 6 товаров")
    void loginSuccess() {
        login("standard_user", "secret_sauce");
        assertTrue(page.url().contains("/inventory.html"), "должны попасть на /inventory.html");
        assertEquals(6, page.locator(".inventory_item").count(), "в каталоге 6 товаров");
    }

    @Test
    @DisplayName("TC-1.2 Заблокированный пользователь -> ошибка блокировки")
    void lockedOut() {
        login("locked_out_user", "secret_sauce");
        String error = page.locator("[data-test=error]").textContent();
        assertTrue(error.contains("locked out"), "ожидали сообщение о блокировке");
    }

    @Test
    @DisplayName("TC-1.4 Пустой логин -> Username is required")
    void emptyUsername() {
        page.navigate("https://www.saucedemo.com/");
        page.click("#login-button");
        String error = page.locator("[data-test=error]").textContent();
        assertTrue(error.contains("Username is required"));
    }

    @Test
    @DisplayName("TC-1.6 Добавить товар в корзину -> бейдж = 1")
    void addToCart() {
        login("standard_user", "secret_sauce");
        page.click("[data-test='add-to-cart-sauce-labs-backpack']");
        assertEquals("1", page.locator(".shopping_cart_badge").textContent());
    }

    @Test
    @DisplayName("TC-1.10 Сортировка по цене (low to high) -> порядок по возрастанию")
    void sortByPrice() {
        login("standard_user", "secret_sauce");
        page.selectOption(".product_sort_container", "lohi");

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
        login("standard_user", "secret_sauce");
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
