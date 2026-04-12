package com.driveaway.service;

import com.driveaway.dto.LicenseRequest;
import com.driveaway.entity.License;
import com.driveaway.repository.LicenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class LicenseService {

    @Autowired
    private LicenseRepository licenseRepository;

    public String addOrUpdateLicense(LicenseRequest req) {

        License license = licenseRepository.findByUserId(req.userId)
                .orElse(new License());

        license.setUserId(req.userId);
        license.setLicenseNumber(req.licenseNumber);
        license.setExpiryDate(req.expiryDate);

        if (req.expiryDate.isBefore(LocalDate.now())) {
            license.setStatus("EXPIRED");
        } else {
            license.setStatus("VERIFIED");
        }

        licenseRepository.save(license);

        return "License Saved";
    }

    public boolean isLicenseValid(String userId) {

        return licenseRepository.findByUserId(userId).map(l -> l.getStatus().equals("VERIFIED"))
                .orElse(false);
    }
}