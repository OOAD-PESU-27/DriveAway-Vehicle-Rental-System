package com.driveaway.controller;

import org.springframework.web.bind.annotation.*;
import com.driveaway.entity.DateSelectionEntity;
import com.driveaway.repository.DateSelectionRepository;

@RestController
@RequestMapping("/dates")
@CrossOrigin(origins = "*")
public class DateSelectionController {

    private final DateSelectionRepository repo;
    public DateSelectionController(DateSelectionRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/create")
    public DateSelectionEntity create(@RequestBody DateSelectionEntity record) {
        record.setStatus("PENDING");
        return repo.save(record);
    }

    @GetMapping
    public Iterable<DateSelectionEntity> getAll() {
        return repo.findAll();
    }
}