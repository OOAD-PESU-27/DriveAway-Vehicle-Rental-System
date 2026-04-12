package com.driveaway.controllers;

import com.driveaway.services.LicenseService;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class LicenseController {

    @FXML
    private TextField licenseField;

    @FXML
    private TextField expiryField;

    private LicenseService service = new LicenseService();

    @FXML
    public void handleSubmit() {

        String userId = LoginController.getUserId();

        String response = service.addLicense(
                userId,
                licenseField.getText(),
                expiryField.getText()
        );

        System.out.println(response);
    }
}