package com.driveaway.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.driveaway.entity.DateSelectionEntity;
import com.driveaway.repository.DateSelectionRepository;

@RestController
@RequestMapping("/dates")   // 🔥 base path
@CrossOrigin(origins = "*")
public class DateSelectionController {

    private final DateSelectionRepository repo;

    // ✅ Constructor Injection (SOLID)
    public DateSelectionController(DateSelectionRepository repo) {
        this.repo = repo;
    }

    // ✅ Create record when user clicks BOOK
    @PostMapping("/create")
    public DateSelectionEntity create(@RequestBody DateSelectionEntity record) {

        System.out.println("Received: " + record);
        record.setStatus("PENDING");

        return repo.save(record);
    }

    // ✅ Optional: Get all records (for debugging/testing)
    @GetMapping
    public Iterable<DateSelectionEntity> getAll() {
        return repo.findAll();
    }
    
}