@echo off
rem Minimal Gradle wrapper launcher (see gradlew for details).
set APP_HOME=%~dp0
set WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

if not exist "%WRAPPER_JAR%" (
    echo gradle-wrapper.jar fehlt.
    echo Einmalig ausfuehren ^(benoetigt installiertes Gradle 9.4+^):  gradle wrapper --gradle-version 9.4.1
    echo Oder GitHub Actions benutzen ^(siehe README.md^).
    exit /b 1
)

if defined JAVA_HOME (
    set JAVACMD=%JAVA_HOME%\bin\java.exe
) else (
    set JAVACMD=java.exe
)

"%JAVACMD%" -Xmx64m -Xms64m -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
