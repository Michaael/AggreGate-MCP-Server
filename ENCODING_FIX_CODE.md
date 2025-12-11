# Исправление проблемы с кодировкой в MCP сервере

## Проблема

Русские тексты отображались в неправильной кодировке: `РЈРїСЂР°РІР»РµРЅРёРµ РѕР±СЉРµРєС‚Р°РјРё РЅРµРґРІРёР¶РёРјРѕСЃС‚Рё`

Это происходило из-за того, что `InputStreamReader` и `PrintWriter` использовали системную кодировку (Windows-1251 на Windows) вместо UTF-8.

## Исправление

### Файл: `mcp-server/src/main/java/com/tibbo/aggregate/mcp/protocol/McpProtocolHandler.java`

#### Изменения:

1. **Добавлены импорты:**
```java
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
```

2. **Исправлен конструктор:**
```java
// БЫЛО:
this.reader = new BufferedReader(new InputStreamReader(System.in));
this.writer = new PrintWriter(System.out, true);

// СТАЛО:
// Use UTF-8 encoding explicitly to avoid encoding issues with Russian text
this.reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
this.writer = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
```

## Объяснение

- `InputStreamReader(System.in)` - использовал системную кодировку (Windows-1251 на Windows)
- `InputStreamReader(System.in, StandardCharsets.UTF_8)` - теперь явно использует UTF-8
- `PrintWriter(System.out, true)` - использовал системную кодировку
- `PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true)` - теперь явно использует UTF-8

## Что нужно сделать

1. Пересобрать MCP сервер:
```bash
./gradlew :mcp-server:build
```

2. Перезапустить Cursor IDE

3. Пересоздать контексты с русскими описаниями - теперь они должны отображаться правильно

## Проверка

После пересборки и перезапуска, при создании контекстов с русскими описаниями они должны отображаться корректно в AggreGate.

---

**Дата исправления**: 2025-01-27

