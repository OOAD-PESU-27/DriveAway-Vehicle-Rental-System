package com.driveaway.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "holidays")
public class Holiday {
    @Id
    private String id;
    private String date; // "YYYY-MM-DD"
    private String name;

    // getters + setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
}
