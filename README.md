# QA Automation — тестовый фреймворк (JUnit 5 + REST Assured + Playwright)

[![tests](https://github.com/nurlan-d/qa-automation/actions/workflows/tests.yml/badge.svg)](https://github.com/nurlan-d/qa-automation/actions/workflows/tests.yml)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://adoptium.net/)
[![JUnit5](https://img.shields.io/badge/JUnit-5-green)](https://junit.org/junit5/)
[![REST Assured](https://img.shields.io/badge/REST%20Assured-5.4-blue)](https://rest-assured.io/)
[![Playwright](https://img.shields.io/badge/Playwright-1.61-2EAD33)](https://playwright.dev/java/)
[![Datafaker](https://img.shields.io/badge/Datafaker-2.7-lightgrey)](https://www.datafaker.net/)

Учебно-портфельный проект автотестов. Показывает четыре вещи, которые ждут от QA-инженера:
API-тесты, UI-тесты, регрессионную сетку на реальные найденные дефекты и подготовку
тестовых данных по классам эквивалентности и границам.

## Что внутри

```
qa-automation/
├── pom.xml
├── README.md
├── test-data-фабрика-памятка.md              # как устроены фабрики и как их подключать
└── src/test/java/
    ├── api/
    │   ├── RestfulBookerApiTest.java         # 10 API-тестов: CRUD + авторизация
    │   └── RestfulBookerNegativeTest.java    # 17 параметризованных: границы и негатив
    ├── ui/
    │   ├── PlaywrightBase.java               # жизненный цикл браузера
    │   ├── SauceDemoTest.java                # 10 UI-тестов: happy-path + негатив логина
    │   └── ProblemUserRegressionTest.java    # 6 регрессий на баги problem_user (@Disabled)
    ├── data/
    │   ├── BookingFactory.java               # генератор бронирований (Datafaker)
    │   └── SauceDemoUser.java                # фиксированные учётки Swag Labs
    └── model/
        ├── Booking.java                      # POJO бронирования + toJson()
        └── BookingDates.java                 # вложенные даты заезда/выезда
```

## Требования

- **JDK 17+** (проверь: `java -version`)
- **Maven ставить не нужно** — в проект встроен Maven Wrapper (`./mvnw`), он сам скачает нужную версию Maven
- Доступ в интернет (тесты бьют по живым учебным сайтам)

Браузер для Playwright скачается автоматически при первом запуске. Если нет — поставь вручную:

```
./mvnw exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.classpathScope=test -Dexec.args="install chromium"
```

## Как запустить

Maven ставить не нужно — используется встроенный wrapper `./mvnw` (на Windows: `mvnw.cmd`).

Клонировать и прогнать всё:

```
git clone https://github.com/nurlan-d/qa-automation.git
cd qa-automation
./mvnw test
```

Только API (быстро, без браузера):

```
./mvnw test -Dtest=RestfulBookerApiTest
```

Только UI:

```
./mvnw test -Dtest=SauceDemoTest
```

Только граничные и негативные наборы данных:

```
./mvnw test -Dtest=RestfulBookerNegativeTest
```

Ожидаемый результат «из коробки»: **37 тестов зелёные** (27 API + 10 UI), **6 регрессий пропущены** (`@Disabled` — это открытые баги problem_user). Всего 43.

## Мишени

| Слой | Сайт | Что проверяем |
|------|------|----------------|
| API | https://restful-booker.herokuapp.com | CRUD бронирований + авторизация |
| UI | https://www.saucedemo.com | Логин, каталог, корзина, чекаут, сортировка |

## Тестовые данные

Тела запросов и учётки не пишутся строками в тестах, а приходят из фабрик в пакете `data`.
Что это даёт на практике:

- **Захардкоженные «Ivan/Petrov» проверяли ровно один набор значений.** `BookingFactory`
  генерирует новые реалистичные данные каждый прогон, а проверки идут против того же
  объекта, который отправили, — не против константы в ассерте.
- **Наборы отобраны по технике, а не наугад:** классы эквивалентности (`depositpaid`
  true/false), границы (длина 0, строка на 10 000 символов, цена `0` / `-1` /
  `Integer.MAX_VALUE`), негативные данные (спецсимволы, XSS, SQL-инъекция), локаль
  (кириллица, эмодзи, RTL) и невалидные даты.
- **Добавить кейс — одна строка в провайдере**, а не новый тест-метод: 17 прогонов
  `RestfulBookerNegativeTest` разворачиваются из `BookingFactory.boundaryAndNegativeCases()`.
- **Пароль Swag Labs задан в одном месте.** В тестах читается намерение —
  `login(SauceDemoUser.LOCKED_OUT)` вместо пары магических строк.

Подробнее — в [памятке по фабрикам](test-data-фабрика-памятка.md): что за что отвечает,
как менять одно поле через копирующий билдер и как сделать прогон воспроизводимым (`reseed`).

**Честно о силе негативных проверок.** `RestfulBookerNegativeTest` — характеризующий тест:
у публичного тренажёра нет спецификации, которая обязывала бы его определённым образом
реагировать на пустое имя, поэтому фиксируется факт (ответ приходит и он один из
документированных: 200/400/500). Такой тест не поймает «сервис молча принял мусор», но
поймает падение, таймаут или 502 на конкретном наборе данных. Появится спека — `anyOf`
меняется на точный код по каждому кейсу, сами данные при этом трогать не придётся.

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
