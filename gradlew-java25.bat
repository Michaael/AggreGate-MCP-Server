@echo off
REM Обходной путь для работы с Java 25 в Gradle
REM Этот скрипт пытается обойти проблему с парсингом версии Java 25.0.1

REM Проверяем, есть ли Java 8 или другая совместимая версия
set JAVA8_PATH=C:\Program Files (x86)\Common Files\Oracle\Java\java8path
if exist "%JAVA8_PATH%\java.exe" (
    echo Using Java 8 from %JAVA8_PATH%
    set "JAVA_HOME=%JAVA8_PATH%"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    call gradlew.bat %*
    exit /b %ERRORLEVEL%
)

REM Если Java 8 не найдена, пробуем использовать Java 25 с обходным путем
echo Warning: Java 25 is not fully supported by Gradle
echo Attempting to use Java 25 with workaround...

REM Пробуем запустить с дополнительными опциями JVM
set GRADLE_OPTS=-Dorg.gradle.java.home="C:\Program Files\Java\jdk-25" --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED

call gradlew.bat %*
exit /b %ERRORLEVEL%
