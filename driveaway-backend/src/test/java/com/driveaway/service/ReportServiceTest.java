package com.driveaway.service;

import com.driveaway.dto.ReportResponse;
import com.driveaway.entity.Report;
import com.driveaway.entity.Vehicle;
import com.driveaway.repository.PaymentRepository;
import com.driveaway.repository.ReportRepository;
import com.driveaway.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
