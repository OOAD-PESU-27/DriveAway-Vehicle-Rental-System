package com.driveaway.service;

import com.driveaway.dto.ReportResponse;
import com.driveaway.entity.Booking;
import com.driveaway.entity.MaintenanceRecord;
import com.driveaway.entity.Report;
import com.driveaway.entity.Vehicle;
import com.driveaway.repository.BookingRepository;
import com.driveaway.repository.MaintenanceRepository;
import com.driveaway.repository.PaymentRepository;
import com.driveaway.repository.ReportRepository;
import com.driveaway.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportService.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private MaintenanceRepository maintenanceRepository;

    @InjectMocks
    private ReportService reportService;

    private Vehicle buildVehicle(boolean available, String status) {
        Vehicle v = new Vehicle();
        v.setAvailable(available);
        v.setStatus(status);
        v.setBrand("Toyota");
        v.setModel("Camry");
        return v;
    }

    @Test
    void generateVehicleInventoryReport_countsVehiclesCorrectly() {
        Vehicle avail1 = buildVehicle(true, "AVAILABLE");
        Vehicle avail2 = buildVehicle(true, "AVAILABLE");
        Vehicle booked = buildVehicle(false, "BOOKED");
        Vehicle maint = buildVehicle(false, "MAINTENANCE");

        when(vehicleRepository.findAll()).thenReturn(List.of(avail1, avail2, booked, maint));
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            r.setId("report-001");
            return r;
        });

        ReportResponse response = reportService.generateVehicleInventoryReport("admin-1");

        assertTrue(response.isSuccess());
        assertEquals(4, response.getTotalVehicles());
        assertEquals(2, response.getAvailableVehicles());
        assertEquals(1, response.getBookedVehicles());
        assertEquals(1, response.getMaintenanceVehicles());
        assertEquals("VEHICLE_INVENTORY", response.getReportType());

        verify(auditLogService).logReportAction(eq("REPORT_GENERATED"), eq("report-001"), eq("admin-1"), any());
    }

    @Test
    void generateVehicleInventoryReport_withEmptyFleet_returnsZeroCounts() {
        when(vehicleRepository.findAll()).thenReturn(List.of());
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            r.setId("report-empty");
            return r;
        });

        ReportResponse response = reportService.generateVehicleInventoryReport("admin-1");

        assertTrue(response.isSuccess());
        assertEquals(0, response.getTotalVehicles());
        assertEquals(0, response.getAvailableVehicles());
    }

    @Test
    void generateCustomReport_withNoPayments_returnsZeroMetrics() {
        when(paymentRepository.findByPaymentDateBetween(any(), any())).thenReturn(List.of());
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            r.setId("report-custom");
            return r;
        });

        ReportResponse response = reportService.generateDailyReport("admin-1");

        assertTrue(response.isSuccess());
        assertEquals(0, response.getTotalTransactions());
        assertEquals(0.0, response.getTotalRevenue());
    }

    @Test
    void getAllReports_returnsRepositoryResults() {
        Report r1 = new Report("DAILY", null, null);
        r1.setId("r1");
        Report r2 = new Report("WEEKLY", null, null);
        r2.setId("r2");

        when(reportRepository.findAll()).thenReturn(List.of(r1, r2));

        List<Report> reports = reportService.getAllReports();

        assertEquals(2, reports.size());
        assertEquals("r1", reports.get(0).getId());
        assertEquals("r2", reports.get(1).getId());
    }

    // ── New report types ─────────────────────────────────────────────────────

    @Test
    void generateRevenueReport_withNoPayments_returnsZeroMetrics() {
        when(paymentRepository.findAll()).thenReturn(List.of());
        when(bookingRepository.count()).thenReturn(0L);
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            r.setId("rev-001");
            return r;
        });

        ReportResponse response = reportService.generateRevenueReport("admin-1");

        assertTrue(response.isSuccess());
        assertEquals("REVENUE", response.getReportType());
        assertEquals(0.0, response.getTotalRevenue());
        assertEquals(0, response.getTotalBookings());
    }

    @Test
    void generateVehicleUsageReport_identifiesMostUsedVehicle() {
        Booking b1 = buildBooking("bk-u01", "v-car1");
        Booking b2 = buildBooking("bk-u02", "v-car1");
        Booking b3 = buildBooking("bk-u03", "v-car2");
        b1.setStatus("COMPLETED");
        b2.setStatus("COMPLETED");
        b3.setStatus("CANCELLED");

        when(bookingRepository.findAll()).thenReturn(List.of(b1, b2, b3));
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            r.setId("usage-001");
            return r;
        });

        ReportResponse response = reportService.generateVehicleUsageReport("admin-1");

        assertTrue(response.isSuccess());
        assertEquals("VEHICLE_USAGE", response.getReportType());
        assertEquals(3, response.getTotalBookings());
        assertEquals("v-car1", response.getMostUsedVehicleId());
    }

    @Test
    void generateDamageReport_aggregatesDamageCharges() {
        Booking b1 = buildBooking("bk-d01", "v-1");
        b1.setDamageCharge(500.0);
        Booking b2 = buildBooking("bk-d02", "v-2");
        b2.setDamageCharge(0.0);
        Booking b3 = buildBooking("bk-d03", "v-3");
        b3.setDamageCharge(1500.0);

        when(bookingRepository.findAll()).thenReturn(List.of(b1, b2, b3));
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            r.setId("dmg-001");
            return r;
        });

        ReportResponse response = reportService.generateDamageReport("admin-1");

        assertTrue(response.isSuccess());
        assertEquals("DAMAGE", response.getReportType());
        assertEquals(2, response.getDamageIncidents());
        assertEquals(2000.0, response.getTotalDamageCharges(), 0.001);
    }

    @Test
    void generateMaintenanceReport_aggregatesCosts() {
        MaintenanceRecord mr1 = new MaintenanceRecord("v-1", "admin-1", "OIL_CHANGE", "Oil change", 800.0, LocalDate.now());
        mr1.setStatus("COMPLETED");
        MaintenanceRecord mr2 = new MaintenanceRecord("v-2", "admin-1", "BRAKE_CHECK", "Brake check", 1200.0, LocalDate.now().plusDays(3));
        mr2.setStatus("SCHEDULED");

        when(maintenanceRepository.findAll()).thenReturn(List.of(mr1, mr2));
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            r.setId("mnt-001");
            return r;
        });

        ReportResponse response = reportService.generateMaintenanceReport("admin-1");

        assertTrue(response.isSuccess());
        assertEquals("MAINTENANCE", response.getReportType());
        assertEquals(2000.0, response.getMaintenanceCost(), 0.001);
        assertEquals(1, (long) response.getSuccessfulTransactions());  // completed
        assertEquals(1, (long) response.getFailedTransactions());      // scheduled
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private Booking buildBooking(String id, String vehicleId) {
        Booking b = new Booking();
        b.setId(id);
        b.setUserId("user-1");
        b.setVehicleId(vehicleId);
        b.setStartDate(LocalDate.now());
        b.setEndDate(LocalDate.now().plusDays(2));
        b.setStatus("CONFIRMED");
        return b;
    }
}
