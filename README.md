# QA Automation — тестовый фреймворк (JUnit 5 + REST Assured + Playwright)

<!-- После создания репозитория замени OWNER/REPO на свой логин и название репозитория -->
[![tests](https://github.com/OWNER/REPO/actions/workflows/tests.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/tests.yml)

Учебно-портфельный проект автотестов. Показывает три вещи, которые ждут от QA-инженера:
API-тесты, UI-тесты и регрессионную сетку на реальные найденные дефекты.

## Что внутри

```
qa-automation/
├── pom.xml
├── README.md
└── src/test/java/
    ├── api/
    │   └── RestfulBookerApiTest.java      # 10 API-тестов (REST Assured) — restful-booker
    └── ui/
        ├── PlaywrightBase.java            # жизненный цикл браузера
        ├── SauceDemoTest.java             # 6 UI-тестов happy-path (standard_user) — зелёные
        └── ProblemUserRegressionTest.java # 6 регрессий на баги problem_user (@Disabled)
```

## Требования

- **JDK 11+** (проверь: `java -version`)
- **Maven 3.8+** (проверь: `mvn -v`)
- Доступ в интернет (тесты бьют по живым учебным сайтам)

Браузер для Playwright скачается автоматически при первом запуске. Если нет — поставь вручную:

```
mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

## Как запустить

Все тесты:

```
mvn test
```

Только API (быстро, без браузера):

```
mvn test -Dtest=RestfulBookerApiTest
```

Только UI:

```
mvn test -Dtest=SauceDemoTest
```

Ожидаемый результат «из коробки»: **16 тестов зелёные** (10 API + 6 UI), **6 регрессий пропущены** (`@Disabled` — это открытые баги problem_user).

## Мишени

| Слой | Сайт | Что проверяем |
|------|------|----------------|
| API | https://restful-booker.herokuapp.com | CRUD бронирований + авторизация |
| UI | https://www.saucedemo.com | Логин, каталог, корзина, чекаут, сортировка |

---

## 🐞 Баг-репорты (persona `problem_user`, saucedemo)

Найдены слепым исследовательским тестированием. Каждому соответствует регрессионный тест
в `ProblemUserRegressionTest` — сними `@Disabled`, чтобы увидеть падение вживую.

### BUG-1 — Одинаковые картинки товаров · Severity: Medium
- **Шаги:** войти `problem_user`/`secret_sauce` → открыть каталог → сравнить `src` картинок.
- **Ожидается:** у каждого из 6 товаров своя картинка.
- **Фактически:** все 6 `<img>` ссылаются на заглушку `sl-404` (уникальных src = 1 из 6).
- **Тест:** `productImagesShouldBeUnique()`

### BUG-2 — Не работает сортировка · Severity: Medium
- **Шаги:** каталог → выбрать «Price (low to high)» → сверить порядок.
- **Ожидается:** порядок меняется по возрастанию цены.
- **Фактически:** порядок не меняется (у `standard_user` тем же кодом сортировка работает — контроль).
- **Тест:** `sortingShouldWork()`

### BUG-3 — «Add to cart» не срабатывает у части товаров · Severity: High
- **Шаги:** каталог → «Add to cart» на Bolt T-Shirt / Fleece Jacket / Test.allTheThings T-Shirt.
- **Ожидается:** товар добавляется, счётчик растёт.
- **Фактически:** у 3 из 6 товаров кнопка не реагирует — половину каталога нельзя купить.
- **Тест:** `addToCartShouldWork()`

### BUG-4 — «Remove» не убирает товар · Severity: High
- **Шаги:** добавить товар → нажать «Remove» → смотреть счётчик.
- **Ожидается:** товар удаляется, счётчик уменьшается.
- **Фактически:** счётчик не меняется — состав корзины исправить нельзя.
- **Тест:** `removeShouldWork()`

### BUG-5 — Ссылка ведёт на чужой товар · Severity: High
- **Шаги:** каталог → клик по названию товара → сверить название на карточке.
- **Ожидается:** открывается тот же товар.
- **Фактически:** совпадений 0 из 6; одна ссылка ведёт на «ITEM NOT FOUND» (битый id).
- **Тест:** `productLinkShouldMatch()`

### BUG-6 — Поле Last Name в чекауте не принимает ввод · Severity: Critical
- **Шаги:** корзина → Checkout → заполнить First/Last/Zip по отдельности → Continue.
- **Ожидается:** поля сохраняют ввод, заказ идёт дальше.
- **Фактически:** Last Name остаётся пустым → «Error: Last Name is required» → купить невозможно.
  На `standard_user` тем же скриптом заказ проходит (контроль).
- **Тест:** `lastNameShouldAcceptInput()`

---

## Методическая заметка

Баги BUG-1…BUG-6 найдены не по готовым кейсам, а **исследовательским тестированием вслепую**:
без подсказок, где искать. Каждый дефект перед занесением проверялся на «не ошибка ли это
теста» — контрольным прогоном под `standard_user`. Это ключевой навык QA: отличать дефект
продукта от кривого локатора/ассерта, чтобы не плодить ложные баги.
