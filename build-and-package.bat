@echo off
echo ================================
echo = Building project with Maven =
echo ================================
call mvn clean package

IF %ERRORLEVEL% NEQ 0 (
    echo [WARNING] Maven returned non-zero errorlevel, continuing anyway...
)

echo Copying JAR as app.jar...
copy /Y target\equalLibTestApp-1.0-SNAPSHOT.jar target\app\app.jar

echo ================================
echo = Running jpackage (app-image) with console =
echo ================================

jpackage ^
  --name EqualLibTestApp ^
  --type app-image ^
  --input target\app ^
  --main-jar app.jar ^
  --main-class com.romiis.equallibtestapp.MainClass ^
  --runtime-image target\app ^
  --dest target\dist ^
  --java-options "--add-opens=java.base/java.util=com.romiis.equallibtestapp" ^
  --java-options "--add-opens=java.base/java.lang=com.romiis.equallibtestapp" ^
  --java-options "--add-opens=java.base/java.security=com.romiis.equallibtestapp" ^
  --java-options "--add-opens=java.base/java.util.concurrent=com.romiis.equallibtestapp" ^
  --java-options "--add-opens=java.base/java.io=com.romiis.equallibtestapp" ^
  --java-options "--add-opens=java.base/jdk.internal.reflect=com.romiis.equallibtestapp" ^
  --java-options "--add-opens=java.base/java.lang.ref=com.romiis.equallibtestapp" ^
  --java-options "--add-opens=java.base/jdk.internal.misc=com.romiis.equallibtestapp" ^
  --java-options "--add-opens=java.base/java.util=EqualLib" ^
  --java-options "--add-opens=java.base/java.lang=EqualLib" ^
  --java-options "--add-opens=java.base/java.security=EqualLib" ^
  --java-options "--add-opens=java.base/java.util.concurrent=EqualLib" ^
  --java-options "--add-opens=java.base/java.io=EqualLib" ^
  --java-options "--add-opens=java.base/jdk.internal.reflect=EqualLib" ^
  --java-options "--add-opens=java.base/java.lang.ref=EqualLib" ^
  --java-options "--add-opens=java.base/jdk.internal.misc=EqualLib" ^
  --vendor "Romiis" ^
  --app-version 1.0 ^
  --win-console

IF %ERRORLEVEL% EQU 0 (
    echo ================================
    echo Build successful!
    echo Launching app...
    echo ================================
    start target\dist\EqualLibTestApp\EqualLibTestApp.exe
) ELSE (
    echo jpackage failed!
)

pause
