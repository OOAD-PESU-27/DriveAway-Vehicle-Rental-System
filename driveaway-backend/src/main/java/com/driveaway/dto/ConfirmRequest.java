package com.driveaway.dto;

import java.time.LocalDate;
import java.util.List;

public class ConfirmRequest {

    private List<LocalDate> dates;

    public List<LocalDate> getDates() {
        return dates;
    }

    public void setDates(List<LocalDate> dates) {
        this.dates = dates;
    }
}