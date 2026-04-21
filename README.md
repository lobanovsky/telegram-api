# telegram-api

Прокси-сервис для отправки сообщений через Telegram Bot API. Предназначен для деплоя вне России, где прямые запросы к `api.telegram.org` заблокированы.

## Эндпоинт

### `POST /send-message`

**Заголовки:**
```
X-Api-Key: <секрет>
Content-Type: application/json
```

**Тело запроса:**
```json
{
  "bot_token": "123456:ABC-DEF...",
  "chat_id": 987654321,
  "text": "Текст сообщения"
}
```

**Ответ (успех):**
```json
{"ok":true,"result":{...}}
```

**Ответ (ошибка авторизации):**
```json
{"ok":false,"error":"Unauthorized"}
```

**Ответ от Telegram проксируется как есть** — статус и тело без изменений. Например, если пользователь заблокировал бота:
```
HTTP 403
{"ok":false,"error_code":403,"description":"Forbidden: bot was blocked by the user"}
```

## Запуск

### Через Docker Compose

1. Создайте `.env` в корне проекта:
   ```
   API_KEY=ваш_секретный_ключ
   ```

2. Укажите тег образа и запустите:
   ```bash
   TAG=latest docker-compose up -d
   ```

### Локально (требуется Java 21)

```bash
API_KEY=testkey ./gradlew run
```

## Сборка образа

```bash
docker build -t lobanovsky/telegram-api:latest .
docker push lobanovsky/telegram-api:latest
```

## Пример вызова из Kotlin

```kotlin
httpClient.post("https://your-server.com/send-message") {
    header("X-Api-Key", apiKey)
    contentType(ContentType.Application.Json)
    setBody("""{"bot_token":"$botToken","chat_id":$chatId,"text":"$text"}""")
}
```
