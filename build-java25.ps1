# Обходной путь для сборки с Java 25
# Пробуем использовать переменные окружения для обхода проблемы с парсингом версии

Write-Host "Attempting to build with Java 25 workaround..."

# Пробуем установить переменные окружения для обхода проблемы
$env:JAVA_TOOL_OPTIONS = "--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED"

# Пробуем использовать инструмент для обхода проблемы с версией Java
# Устанавливаем системное свойство для обхода проблемы
$env:GRADLE_OPTS = "-Dorg.gradle.java.home=C:\Program Files\Java\jdk-25 -Djava.version=21.0.0"

Write-Host "Running Gradle build with Java 25 workaround..."
.\gradlew.bat build
