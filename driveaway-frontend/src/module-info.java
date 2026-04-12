module com.driveaway {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    opens com.driveaway to javafx.fxml;
    opens com.driveaway.controllers to javafx.fxml;
}