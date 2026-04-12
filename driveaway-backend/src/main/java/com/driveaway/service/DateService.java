package com.driveaway.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public class DateService {

    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY ||
               date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    public static int countHolidays(List<LocalDate> dates, List<String> holidays) {
        int count = 0;
        for (LocalDate date : dates) {
            if (holidays.contains(date.toString())) {
                count++;
            }
        }
        return count;
    }

    public static int countWeekends(List<LocalDate> dates) {
        int count = 0;
        for (LocalDate date : dates) {
            if (isWeekend(date)) count++;
        }
        return count;
    }
}