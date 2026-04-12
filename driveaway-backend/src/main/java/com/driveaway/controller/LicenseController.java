package com.driveaway.controller;

import com.driveaway.dto.LicenseRequest;
import com.driveaway.service.LicenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/license")
@CrossOrigin
public class LicenseController {

    @Autowired
    private LicenseService licenseService;

    @PostMapping("/add")
    public String addLicense(@RequestBody LicenseRequest req) {
        return licenseService.addOrUpdateLicense(req);
    }
}