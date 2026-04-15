@echo off

set JAVA_HOME=C:\Users\Sushma Venkatesh\.jdk\jdk-23
set PATH=%JAVA_HOME%\bin;%PATH%

if exist ".env" (
    for /f "usebackq tokens=1* delims==" %%A in (".env") do (
        if not "%%A"=="" if not "%%A"=="#" set "%%A=%%B"
    )
)

set FX_LIB=javafx-sdk-25.0.2/lib

if not exist "%FX_LIB%\javafx.controls.jar" (
    echo JavaFX SDK jars not found in %FX_LIB%
    echo Please extract the JavaFX SDK into the driveaway-frontend\javafx-sdk-25.0.2\lib folder.
    echo Expected files include javafx.controls.jar and javafx.fxml.jar.
    pause
    exit /b 1
)

echo ===============================
echo Cleaning previous build...
echo ===============================
if exist out rmdir /s /q out

echo ===============================
echo Compiling JavaFX Project...
echo ===============================

javac --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.fxml -d out ^
    src/com/driveaway/MainApp.java ^
    src/com/driveaway/controllers/*.java ^
    src/com/driveaway/models/*.java ^
    src/com/driveaway/services/*.java ^
    src/com/driveaway/utils/*.java

if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)

echo ===============================
echo Copying FXML files...
echo ===============================

xcopy src\com\driveaway\views out\com\driveaway\views /E /I /Y

echo ===============================
echo Running Application...
echo ===============================

java --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.fxml -cp out com.driveaway.MainApp

pause