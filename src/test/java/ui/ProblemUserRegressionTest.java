package ui;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * РЕГРЕССИЯ на 6 дефектов persona problem_user, найденных при слепом исследовании (qa-explore).
 *
 * Каждый тест проверяет КОРРЕКТНОЕ поведение и помечен @Disabled как открытый баг,
 * чтобы основной прогон оставался зелёным. Когда дефект починят — снять @Disabled,
 * тест должен позеленеть. Это и есть регрессионная сетка: она поймает, если баг вернётся.
 *
 * Хочешь увидеть, как тесты ловят баги «вживую» — временно закомментируй @Disabled
 * и запусти: они упадут ровно на тех дефектах, что описаны в багрепортах.
 */
class ProblemUserRegressionTest extends PlaywrightBase {

    @BeforeEach
    void loginAsProblemUser() {
        login("problem_user", "secret_sauce");
    }

    @Test
    @Disabled("BUG-1 (Medium): у problem_user все 6 картинок товаров — одна заглушка sl-404")
    @DisplayName("BUG-1 Картинки товаров должны различаться")
    void productImagesShouldBeUnique() {
        Locator imgs = page.locator("img.inventory_item_img");
        int n = imgs.count();
        Set<String> unique = new HashSet<>();
        for (int i = 0; i < n; i++) {
            unique.add(imgs.nth(i).getAttribute("src"));
        }
        assertEquals(n, unique.size(), "у каждого товара должна быть своя картинка");
    }

    @Test
    @Disabled("BUG-2 (Medium): сортировка не меняет порядок товаров")
    @DisplayName("BUG-2 Сортировка по цене должна работать")
    void sortingShouldWork() {
        page.selectOption(".product_sort_container", "lohi");
        List<Double> prices = page.locator(".inventory_item_price").allTextContents()
                .stream()
                .map(p -> Double.parseDouble(p.replace("$", "").trim()))
                .collect(Collectors.toList());
        List<Double> expected = new ArrayList<>(prices);
        Collections.sort(expected);
        assertEquals(expected, prices, "после сортировки цены должны идти по возрастанию");
    }

    @Test
    @Disabled("BUG-3 (High): 'Add to cart' не срабатывает у части товаров (напр. Bolt T-Shirt)")
    @DisplayName("BUG-3 Add to cart должен работать для всех товаров")
    void addToCartShouldWork() {
        page.click("[data-test='add-to-cart-sauce-labs-bolt-t-shirt']");
        assertEquals("1", page.locator(".shopping_cart_badge").textContent(),
                "товар должен добавиться в корзину");
    }

    @Test
    @Disabled("BUG-4 (High): 'Remove' не убирает товар из корзины")
    @DisplayName("BUG-4 Remove должен убирать товар")
    void removeShouldWork() {
        page.click("[data-test='add-to-cart-sauce-labs-backpack']");
        page.click("[data-test='remove-sauce-labs-backpack']");
        assertEquals(0, page.locator(".shopping_cart_badge").count(),
                "после удаления счётчик корзины должен исчезнуть");
    }

    @Test
    @Disabled("BUG-5 (High): ссылка на товар открывает чужую карточку / ITEM NOT FOUND")
    @DisplayName("BUG-5 Ссылка на товар должна вести на этот же товар")
    void productLinkShouldMatch() {
        String nameInList = page.locator(".inventory_item_name").first().textContent();
        page.locator(".inventory_item_name").first().click();
        String nameOnDetail = page.locator(".inventory_details_name").textContent();
        assertEquals(nameInList, nameOnDetail, "открыться должен тот же товар, по которому кликнули");
    }

    @Test
    @Disabled("BUG-6 (Critical): поле Last Name в чекауте не принимает ввод -> заказ невозможен")
    @DisplayName("BUG-6 Поле Last Name должно принимать ввод")
    void lastNameShouldAcceptInput() {
        page.click("[data-test='add-to-cart-sauce-labs-backpack']");
        page.click(".shopping_cart_link");
        page.click("[data-test=checkout]");
        page.fill("[data-test=firstName]", "Ivan");
        page.fill("[data-test=lastName]", "Petrov");
        page.fill("[data-test=postalCode]", "720000");
        assertEquals("Petrov", page.locator("[data-test=lastName]").inputValue(),
                "поле Last Name должно сохранять введённое значение");
    }
}
