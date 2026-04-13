# Run this script from driveaway-frontend folder
# It writes BookingView.fxml without BOM to BOTH src and target

$content = '<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<VBox spacing="12" alignment="CENTER" xmlns:fx="http://javafx.com/fxml" fx:controller="com.driveaway.controllers.BookingController" style="-fx-padding: 20;">
    <Label text="DriveAway Booking and Inspection" style="-fx-font-size: 18; -fx-font-weight: bold;" />
    <VBox spacing="8" style="-fx-border-color: #cccccc; -fx-padding: 12; -fx-background-color: #f9f9f9;">
        <Label text="Customer Booking" style="-fx-font-weight: bold;" />
        <TextField fx:id="vehicleIdField" promptText="Vehicle ID" />
        <DatePicker fx:id="startDatePicker" />
        <DatePicker fx:id="endDatePicker" />
        <CheckBox fx:id="licenseVerifiedCheckBox" text="License Verified" />
        <Button text="Book Vehicle" onAction="#handleBookVehicle" />
        <Label fx:id="resultLabel" wrapText="true" style="-fx-text-fill: green;" />
    </VBox>
    <VBox spacing="8" style="-fx-border-color: #aaaaff; -fx-padding: 12; -fx-background-color: #f8f8ff;">
        <Label text="Staff Return and Inspection" style="-fx-font-weight: bold;" />
        <TextField fx:id="bookingIdField" promptText="Booking ID" />
        <TextField fx:id="returnVehicleIdField" promptText="Vehicle ID" />
        <CheckBox fx:id="hasDamageCheckBox" text="Damage Found" />
        <TextField fx:id="damageDescriptionField" promptText="Damage description" />
        <ChoiceBox fx:id="damageSeverityChoice" />
        <Button text="Submit Return Inspection" onAction="#handleReturnInspection" />
        <TextArea fx:id="inspectionResultArea" prefHeight="80" editable="false" promptText="Result appears here" />
    </VBox>
    <Button text="Logout" onAction="#handleLogout" />
</VBox>'

# UTF-8 encoding WITHOUT BOM
$utf8NoBom = New-Object System.Text.UTF8Encoding $false

# Write to src
[System.IO.File]::WriteAllText(
    (Resolve-Path "src\com\driveaway\views\BookingView.fxml"),
    $content,
    $utf8NoBom
)
Write-Host "Written to src"

# Write directly to target\classes too (skips Maven copy step)
$targetPath = "target\classes\com\driveaway\views\BookingView.fxml"
if (Test-Path $targetPath) {
    [System.IO.File]::WriteAllText(
        (Resolve-Path $targetPath),
        $content,
        $utf8NoBom
    )
    Write-Host "Written to target\classes"
} else {
    Write-Host "target path not found - run mvn compile first"
}

Write-Host "DONE - Both files written without BOM"

# Verify - first byte should NOT be 239 (BOM)
$bytes = [System.IO.File]::ReadAllBytes((Resolve-Path "src\com\driveaway\views\BookingView.fxml"))
if ($bytes[0] -eq 239) {
    Write-Host "ERROR: BOM still present!" -ForegroundColor Red
} else {
    Write-Host "SUCCESS: No BOM. First byte is: $($bytes[0]) (should be 60 = '<')" -ForegroundColor Green
}
