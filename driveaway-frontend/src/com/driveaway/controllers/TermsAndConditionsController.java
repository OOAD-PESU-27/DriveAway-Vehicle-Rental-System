package com.driveaway.controllers;

import com.driveaway.utils.SceneNavigator;
import javafx.fxml.FXML;

/**
 * TermsAndConditionsController - Displays the DriveAway Terms and Conditions page.
 * Supports navigating back to the previous booking view.
 */
public class TermsAndConditionsController {

    /** The FXML view path to return to after reading the T&C (e.g. BookingView or VehicleDetailsView). */
    private static String previousView = "views/VehicleCatalogView.fxml";

    public static void setPreviousView(String view) {
        previousView = view;
    }

    /** Navigate back to the view the user came from. */
    @FXML
    public void goBack() {
        SceneNavigator.load(previousView);
    }

    /**
     * Called when the user clicks "I Accept — Go Back to Booking".
     * Returns to the previous booking view so the user can check the checkbox.
     */
    @FXML
    public void handleAcceptAndGoBack() {
        SceneNavigator.load(previousView);
    }

    @FXML
    public void handleLogout() {
        LoginController.logout();
        SceneNavigator.load("views/LoginView.fxml");
    }
}
