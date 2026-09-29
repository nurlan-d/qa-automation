package ui;

import com.microsoft.playwright.*;
import data.SauceDemoUser;
import org.junit.jupiter.api.*;

/**
 * Базовый класс для UI-тестов: жизненный цикл браузера Playwright.
 * Браузер поднимается один раз на класс, свежий контекст — на каждый тест
 * (чистые cookies/сессия между тестами).
 */
public abstract class PlaywrightBase {

    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void createContext() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        if (context != null) context.close();
    }

    /** Логин в Swag Labs произвольными строками — для негативных проверок формы. */
    protected void login(String user, String pass) {
        page.navigate("https://www.saucedemo.com/");
        page.fill("#user-name", user);
        page.fill("#password", pass);
        page.click("#login-button");
    }

    /**
     * Логин фиксированной учёткой тренажёра. Предпочитай эту перегрузку:
     * персона названа явно, а пароль берётся из одного места
     * ({@link SauceDemoUser#PASSWORD}), а не дублируется строкой по тестам.
     */
    protected void login(SauceDemoUser user) {
        login(user.username(), user.password());
    }
}
