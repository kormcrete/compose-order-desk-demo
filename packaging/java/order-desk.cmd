@echo off
rem Kormcrete Order Desk Compose on your own Java (17 or newer), with the JVM options of the packaged app. Uses
rem %JAVA_HOME%\bin\java.exe when JAVA_HOME is set, else the java on your PATH. Options a Java does not know are ignored.
setlocal
set "DIR=%~dp0"
set "JAVA=java"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java.exe"
"%JAVA%" -version >nul 2>&1
if errorlevel 1 (
  echo Kormcrete Order Desk Compose needs Java 17 or newer: install it, or set JAVA_HOME.
  pause
  exit /b 1
)
"%JAVA%" -XX:+IgnoreUnrecognizedVMOptions --enable-native-access=ALL-UNNAMED -Dcompose.application.configure.swing.globals=true -XX:+UseSerialGC -Xms24m -Xmx384m -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=30 -XX:+UseCompactObjectHeaders -XX:+UseStringDeduplication -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=48m -Xss512k -XX:-UsePerfData -cp "%DIR%lib\*" io.github.kormcrete.orderdesk.desktop.MainKt
