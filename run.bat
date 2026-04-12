@echo off

echo ===============================
echo Compiling JavaFX Project...
echo ===============================

javac --module-path "javafx-sdk-25.0.2/lib" --add-modules javafx.controls,javafx.fxml -d out ^
src/com/driveaway/MainApp.java ^
src/com/driveaway/controllers/*.java ^
src/com/driveaway/services/*.java ^
src/com/driveaway/utils/*.java
echo ===============================
echo Copying FXML files...
echo ===============================

xcopy src\com\driveaway\views out\com\driveaway\views /E /I /Y
echo ===============================
echo Running Application...
echo ===============================

java --module-path "javafx-sdk-25.0.2/lib;out" --add-modules RentalVehicles,javafx.controls,javafx.fxml -m RentalVehicles/com.driveaway.MainApp

pause