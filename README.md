# 💱 Currency Exchange REST API

REST API для конвертации валют и управления обменными курсами.  
Проект написан на **Java Servlets + JDBC + SQLite**.  
Веб-интерфейс подключён для удобного тестирования.

## 📋 Возможности

- Получение списка всех валют
- Получение валюты по коду
- Добавление новой валюты
- Получение списка всех обменных курсов
- Получение курса по паре валют
- Добавление нового курса
- Обновление существующего курса
- Конвертация суммы из одной валюты в другую (3 сценария)

## 🛠️ Технологии

- **Java 24** (OpenJDK)
- **Java Servlets** (javax.servlet-api 4.0.1)
- **Maven** — управление зависимостями
- **Tomcat 7** (maven-plugin)
- **SQLite** — встраиваемая БД
- **JDBC** — работа с БД
- **Gson** — JSON сериализация

## 🚀 Запуск проекта

### 1. Сборка и запуск через Maven

```bash
# Собрать проект
mvn clean package

# Запустить Tomcat
mvn tomcat7:run
```

### 2. Открыть в браузере

```
http://localhost:8080/
```

### 3. Тестовый фронтенд

После запуска доступен по адресу:
```
http://localhost:8080/index.html
```

## 📡 REST API Эндпоинты

### Валюты

| Метод | URL | Описание |
|-------|-----|----------|
| GET | `/currencies` | Список всех валют |
| POST | `/currencies` | Добавить новую валюту |
| GET | `/currency/{code}` | Получить валюту по коду |

### Обменные курсы

| Метод | URL | Описание |
|-------|-----|----------|
| GET | `/exchangeRates` | Список всех курсов |
| POST | `/exchangeRates` | Добавить новый курс |
| GET | `/exchangeRate/{pair}` | Получить курс по паре |
| PATCH | `/exchangeRate/{pair}` | Обновить курс |

### Конвертация

| Метод | URL | Описание |
|-------|-----|----------|
| GET | `/exchange?from=USD&to=RUB&amount=100` | Конвертировать сумму |

## 📝 Примеры запросов

### Добавление валюты

```http
POST /currencies
Content-Type: application/x-www-form-urlencoded

code=GBP&fullName=British Pound&sign=£
```

### Добавление курса

```http
POST /exchangeRates
Content-Type: application/x-www-form-urlencoded

baseCurrencyCode=USD&targetCurrencyCode=GBP&rate=0.79
```

### Конвертация USD → RUB

```http
GET /exchange?from=USD&to=RUB&amount=100
```

Ответ:

```json
{
  "baseCurrency": {
    "id": 1,
    "code": "USD",
    "fullName": "United States dollar",
    "sign": "$"
  },
  "targetCurrency": {
    "id": 2,
    "code": "RUB",
    "fullName": "Russian Ruble",
    "sign": "₽"
  },
  "rate": 71.91,
  "amount": 100.0,
  "convertedAmount": 7191.0
}
```

## 🗄️ База данных

### Таблица `Currencies`

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | INTEGER | Первичный ключ, автоинкремент |
| `code` | VARCHAR | Код валюты (уникальный) |
| `full_name` | VARCHAR | Полное название |
| `sign` | VARCHAR | Символ валюты |

### Таблица `ExchangeRates`

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | INTEGER | Первичный ключ, автоинкремент |
| `base_currency_id` | INTEGER | Внешний ключ → `Currencies.id` |
| `target_currency_id` | INTEGER | Внешний ключ → `Currencies.id` |
| `rate` | REAL | Курс обмена |

## 📦 Структура проекта

```
CurrencyExchange/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/shadowd3m/
│       │       ├── datatransferobject/   # DTO модели
│       │       └── servlet/              # REST контроллеры
│       └── webapp/
│           ├── WEB-INF/
│           ├── index.html                # Тестовый фронтенд
│           ├── styles.css
│           └── js/
│               └── app.js
├── pom.xml                               # Maven конфигурация
├── currency.db                           # База данных SQLite
└── README.md
```

## 🔧 Сборка WAR

```bash
mvn clean package
```

WAR-файл появится в папке `target/`

## 🧪 Тестирование

- **Postman** — ручное тестирование API
- **Тестовый фронтенд** — `http://localhost:8080/index.html`

## 📄 Лицензия

MIT

---

**Автор:** shadowd3m  
**GitHub:** [https://github.com/shadowd3m](https://github.com/shadowd3m)

---
