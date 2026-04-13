package com.driveaway.controllers;

import com.driveaway.services.LicenseService;
import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class LicenseController {

    @FXML private TextField licenseField;
    @FXML private TextField expiryField;

    private LicenseService service = new LicenseService();

    @FXML
    public void handleSubmit() {
        String userId  = LoginController.getUserId();
        String license = licenseField.getText().trim();
        String expiry  = expiryField.getText().trim();

        if (license.isEmpty() || expiry.isEmpty()) {
            System.out.println("[LICENSE] Please fill all fields.");
            return;
        }

        String response = service.addLicense(userId, license, expiry);
        System.out.println("[LICENSE] Response: " + response);

        // Navigate to booking page after license added
        SceneNavigator.load("views/BookingView.fxml");
    }
}
