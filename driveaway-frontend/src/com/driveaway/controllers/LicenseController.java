package com.driveaway.controllers;

import com.driveaway.services.LicenseService;
import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class LicenseController {

    @FXML private TextField licenseField;
    @FXML private TextField expiryField;
    @FXML private Label statusLabel;

    private final LicenseService service = new LicenseService();

    @FXML
    public void initialize() {
        if (LoginController.getUserId() == null) {
            SceneNavigator.load("views/LoginView.fxml");
        }
    }

    @FXML
    public void handleSubmit() {
        String license = licenseField != null ? licenseField.getText() : "";
        String expiry = expiryField != null ? expiryField.getText() : "";

        if (license.isBlank()) { setStatus("License number is required."); return; }
        if (expiry.isBlank()) { setStatus("Expiry date is required."); return; }

        String userId = LoginController.getUserId();
        if (userId == null) {
            setStatus("Session expired. Please login again.");
            SceneNavigator.load("views/LoginView.fxml");
            return;
        }

        setStatus("Verifying license...");
        String response = service.addLicense(userId, license, expiry);

        if (response != null) {
            SceneNavigator.load("views/DashboardView.fxml");
        } else {
            // Even if license API fails, navigate to dashboard
            setStatus("Could not verify license (offline mode), continuing...");
            SceneNavigator.load("views/DashboardView.fxml");
        }
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }
}
