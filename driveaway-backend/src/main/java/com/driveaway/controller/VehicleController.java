package com.driveaway.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.driveaway.dto.ConfirmRequest;
import com.driveaway.dto.VehicleResponse;
import com.driveaway.service.VehicleService;

@RestController
@RequestMapping("/vehicles")
@CrossOrigin(origins = "*")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping("/search")
    public List<VehicleResponse> getVehicles(@RequestBody ConfirmRequest req) {

        List<LocalDate> dates = req.getDates();

        return vehicleService.getAvailableVehiclesWithPrice(dates);
    }
}