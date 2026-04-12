package com.driveaway.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.driveaway.dto.VehicleResponse;
import com.driveaway.entity.Vehicle;
import com.driveaway.repository.HolidayRepository;
import com.driveaway.repository.VehicleRepository;
import com.driveaway.service.pricing.PricingFactory;
import com.driveaway.service.pricing.PricingStrategy;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepo;
    private final HolidayRepository holidayRepository;

    public VehicleService(VehicleRepository vehicleRepo,
                          HolidayRepository holidayRepository) {
        this.vehicleRepo = vehicleRepo;
        this.holidayRepository = holidayRepository;
    }

    public List<VehicleResponse> getAvailableVehiclesWithPrice(
            List<LocalDate> dates
    ) {

        List<Vehicle> vehicles = vehicleRepo.findAll();

        List<String> holidays = holidayRepository.findAll()
            .stream()
            .map(h -> h.getDate())
            .toList();

        int holidayCount = DateService.countHolidays(dates, holidays);
        int weekendCount = DateService.countWeekends(dates);
        int weekdayCount = DateService.countWeekdays(dates, holidays);
            
        PricingStrategy strategy =
                PricingFactory.getStrategy(holidayCount, weekendCount);

        List<VehicleResponse> responseList = new ArrayList<>();

        for (Vehicle v : vehicles) {

            double basePrice = v.getPricePerDay();
            double total = strategy.calculate(basePrice, dates, holidays);
            String breakdown =
                "Base: " + weekdayCount + " × ₹" + basePrice + "\n" +
                "Weekend: " + weekendCount + " × ₹" + (basePrice * 1.3) + "\n" +
                "Holiday: " + holidayCount + " × ₹" + (basePrice * 1.5);
                
            VehicleResponse res = new VehicleResponse();
            res.setId(v.getId());
            res.setName(v.getBrand() + " " + v.getModel());
            res.setSeatingCapacity(v.getSeatingCapacity());
            res.setPricePerDay(basePrice);
            res.setWeekendPricePerDay(basePrice * 1.3);
            res.setHolidayPricePerDay(basePrice * 1.5);

            res.setTotalPrice(total);

            responseList.add(res);
            res.setPriceBreakdown(breakdown);
      
    }

        return responseList;
    }
}