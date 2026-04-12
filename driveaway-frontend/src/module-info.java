module RentalVehicles {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.rentalvehicles to javafx.fxml;
    exports com.rentalvehicles;
}