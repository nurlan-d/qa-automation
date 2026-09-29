package data;

import org.junit.jupiter.params.provider.Arguments;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Фиксированные тестовые учётки тренажёра Swag Labs (https://www.saucedemo.com).
 *
 * <p>Это НЕ синтетика — это заранее заведённые демо-аккаунты сайта с общим
 * паролем {@link #PASSWORD}. Их нельзя генерировать или удалять: только
 * выбирать нужную персону под сценарий. Каждая персона ведёт себя по-своему —
 * поле {@link #expectedBehavior} служит меткой «что проверяем».
 */
public enum SauceDemoUser {

    /** Здоровый пользователь — happy-path работает полностью. */
    STANDARD("standard_user", "логинится и проходит все сценарии магазина"),

    /** Заблокирован — при входе показывает ошибку блокировки, в каталог не попадает. */
    LOCKED_OUT("locked_out_user", "вход даёт ошибку 'locked out', каталог недоступен"),

    /** Сломанный UI — набор дефектов (картинки, сортировка, add/remove, поле Last Name). */
    PROBLEM("problem_user", "логинится, но UI с багами (см. ProblemUserRegressionTest)"),

    /** Медленный — страницы грузятся с искусственной задержкой. */
    PERFORMANCE_GLITCH("performance_glitch_user", "логинится, но с заметными задержками загрузки"),

    /** Ошибки — часть действий падает с ошибками. */
    ERROR("error_user", "логинится, но часть действий завершается ошибкой"),

    /** Визуальные дефекты — сдвиги/несоответствия вёрстки. */
    VISUAL("visual_user", "логинится, но есть визуальные расхождения в вёрстке");

    /** Общий пароль для всех учёток тренажёра. */
    public static final String PASSWORD = "secret_sauce";

    private final String username;
    private final String expectedBehavior;

    SauceDemoUser(String username, String expectedBehavior) {
        this.username = username;
        this.expectedBehavior = expectedBehavior;
    }

    public String username() {
        return username;
    }

    public String password() {
        return PASSWORD;
    }

    public String expectedBehavior() {
        return expectedBehavior;
    }

    // =====================================================================
    // ПРОВАЙДЕРЫ ДЛЯ @MethodSource / @EnumSource
    // =====================================================================

    /** Все персоны как пары (пользователь, что проверяем) для параметризованного логина. */
    public static Stream<Arguments> allUsers() {
        return Arrays.stream(values())
                .map(u -> Arguments.of(u, u.expectedBehavior));
    }

    /**
     * Негативные данные для формы логина: (username, password, ожидаемый текст ошибки).
     * Логины/пароли — как ведёт себя реальный тренажёр Swag Labs.
     */
    public static Stream<Arguments> invalidLoginCases() {
        return Stream.of(
                Arguments.of("", PASSWORD, "Username is required"),
                Arguments.of(STANDARD.username, "", "Password is required"),
                Arguments.of(STANDARD.username, "wrong_pass",
                        "Username and password do not match any user in this service"),
                Arguments.of("no_such_user", PASSWORD,
                        "Username and password do not match any user in this service"),
                Arguments.of(LOCKED_OUT.username, PASSWORD,
                        "Sorry, this user has been locked out")
        );
    }
}
