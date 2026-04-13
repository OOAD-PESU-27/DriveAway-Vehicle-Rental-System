# Person 3: Booking, Handover, Return & Maintenance Implementation

## Overview
You (Person 3) are responsible for the **core booking lifecycle**: creation, vehicle handover, return inspection, damage assessment, and maintenance scheduling.

---

## Project Structure

### Backend Files Created

#### Models & DTOs
- **Entity Files:**
  - `driveaway-backend/src/main/java/com/driveaway/entity/Booking.java` ✅
  - `driveaway-backend/src/main/java/com/driveaway/entity/Damage.java` ✅
  - `driveaway-backend/src/main/java/com/driveaway/entity/MaintenanceRecord.java` ✅

- **DTOs (Data Transfer Objects):**
  - `driveaway-backend/src/main/java/com/driveaway/dto/BookingRequest.java` ✅
  - `driveaway-backend/src/main/java/com/driveaway/dto/InspectionRequest.java` ✅

#### Repositories
- `driveaway-backend/src/main/java/com/driveaway/repository/BookingRepository.java` ✅
- `driveaway-backend/src/main/java/com/driveaway/repository/DamageRepository.java` ✅
- `driveaway-backend/src/main/java/com/driveaway/repository/MaintenanceRepository.java` ✅
- `driveaway-backend/src/main/java/com/driveaway/repository/VehicleRepository.java` ✅

#### Services (Business Logic)
- `driveaway-backend/src/main/java/com/driveaway/service/BookingService.java` ✅
  - `createBooking(BookingRequest, licenseVerified)` - validates license, calculates fare, creates booking
  - `handoverVehicle(String bookingId)` - marks vehicle as handed over (CONFIRMED → HANDED_OVER)
  - `returnVehicle(String bookingId, hasDamage)` - marks booking as returned
  - `cancelBooking(String bookingId)` - cancels if not yet handed over
  - Query methods: `getAllBookings()`, `getByUser()`, `getByStatus()`

- `driveaway-backend/src/main/java/com/driveaway/service/DamageService.java` ✅
  - `logDamage(InspectionRequest)` - logs damage with auto penalty (LOW/MEDIUM/HIGH)
  - `getDamagesByBooking()`, `getDamagesByVehicle()`
  - Penalty amounts: LOW = ₹2000, MEDIUM = ₹6000, HIGH = ₹15000

- `driveaway-backend/src/main/java/com/driveaway/service/MaintenanceService.java` ✅
  - `scheduleMaintenance(vehicleId, reason)` - auto-triggers after damage
  - `completeMaintenance(maintenanceId)` - staff marks done, vehicle returned to AVAILABLE
  - `getScheduled()`, `getAll()`, `getByVehicle()`

#### Controller (REST API)
- `driveaway-backend/src/main/java/com/driveaway/controller/BookingController.java` ✅

**All Endpoints:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/bookings` | Create booking |
| GET | `/api/bookings` | All bookings (staff dashboard) |
| GET | `/api/bookings/{id}` | Single booking |
| GET | `/api/bookings/user/{userId}` | Customer's bookings |
| GET | `/api/bookings/status/{status}` | Filter by status |
| PUT | `/api/bookings/{id}/handover` | Mark handed over |
| PUT | `/api/bookings/{id}/cancel` | Cancel booking |
| **POST** | **`/api/bookings/return`** | **Return + inspect (key endpoint)** |
| GET | `/api/bookings/maintenance/all` | All maintenance records |
| GET | `/api/bookings/maintenance/scheduled` | Scheduled maintenance |
| PUT | `/api/bookings/maintenance/{id}/complete` | Complete maintenance |
| GET | `/api/bookings/damages/{bookingId}` | Damages for a booking |

---

### Frontend Files Created

#### Models
- `driveaway-frontend/src/com/driveaway/models/Booking.java` (provided by Person 2 setup, now integrated)

#### Controllers
- `driveaway-frontend/src/com/driveaway/controllers/BookingController.java` ✅
  - `handleBookVehicle()` - customer creates booking
  - `handleReturnInspection()` - staff submits return inspection form
  - UI fields: vehicleId, startDate, endDate, damage description, severity

#### Services
- `driveaway-frontend/src/com/driveaway/services/BookingService.java` ✅
  - `createBooking(json, licenseVerified)` - POST to backend
  - `returnInspection(json)` - POST return inspection data

#### Views (FXML)
- `driveaway-frontend/src/com/driveaway/views/BookingView.fxml` ✅
  - Two sections: **Customer booking** and **Return & inspection**
  - Dynamically enables/disables damage fields based on `hasDamageCheckBox`

---

## How It Works

### Booking Creation Flow
```
1. Customer logs in (Person 1)
2. Customer adds license (Person 1)
3. Customer navigates to booking page
4. Fills: Vehicle ID, start date, end date
5. BookingController → BookingService.createBooking()
   - Checks license verified flag
   - Calculates: days × pricePerDay
   - Status: CONFIRMED
   - Vehicle marked: BOOKED / unavailable
6. Booking stored in MongoDB
```

### Vehicle Handover Flow
```
1. Staff sees all CONFIRMED bookings (GET /api/bookings/status/CONFIRMED)
2. Staff clicks "Hand over vehicle"
3. PUT /api/bookings/{id}/handover
   - Status: CONFIRMED → HANDED_OVER
```

### Return & Inspection Flow (KEY ENDPOINT)
```
1. Staff fills return inspection form:
   - Booking ID
   - Vehicle ID
   - Has damage? (yes/no)
   - If damaged: description + severity (LOW/MEDIUM/HIGH)

2. POST /api/bookings/return
   - Step 1: Mark booking as RETURNED
   - Step 2: Log damage (if any) + calculate penalty
   - Step 3: If damage found → auto-schedule maintenance next day

3. Response:
   - "Vehicle returned. Damage logged (MEDIUM). Penalty: Rs.6000. Maintenance scheduled."
   OR
   - "Vehicle returned successfully. No damage found."

4. Vehicle status:
   - If no damage: AVAILABLE
   - If damaged: MAINTENANCE (until maintenance completed)
```

### Maintenance Completion Flow
```
1. Staff completes maintenance for vehicle
2. PUT /api/bookings/maintenance/{id}/complete
   - Mark as COMPLETED
   - Set completedDate to today
   - Vehicle restored to AVAILABLE
```

---

## Booking Status Lifecycle
```
PENDING → CONFIRMED → HANDED_OVER → RETURNED
                   ↓
               CANCELLED
```

- **PENDING**: Created but not confirmed
- **CONFIRMED**: Ready for handover
- **HANDED_OVER**: Vehicle given to customer
- **RETURNED**: Vehicle returned by customer after inspection
- **CANCELLED**: Booking cancelled before handover

---

## Database Collections

MongoDB Collections automatically created:

1. **bookings**
   - Fields: id, userId, vehicleId, startDate, endDate, totalPrice, status, licenseVerified, createdAt

2. **damages**
   - Fields: id, bookingId, vehicleId, description, severity, penaltyAmount, reportedDate

3. **maintenance_records**
   - Fields: id, vehicleId, reason, scheduledDate, completedDate, status

---

## How to Run

### Backend (Spring Boot)
```bash
cd driveaway-backend
mvn clean install
mvn spring-boot:run
# Runs on http://localhost:8080
```

### Frontend (JavaFX)
```bash
cd driveaway-frontend

# Compile
javac --module-path javafx-sdk-25.0.2/lib --add-modules javafx.controls,javafx.fxml -d . src/com/driveaway/*.java src/com/driveaway/controllers/*.java src/com/driveaway/services/*.java src/com/driveaway/utils/*.java src/com/driveaway/models/*.java

# Copy FXML files
xcopy src\com\driveaway\views com\driveaway\views /E /I

# Run
java --module-path javafx-sdk-25.0.2/lib --add-modules javafx.controls,javafx.fxml com.driveaway.MainApp
```

---

## Key Features You Implemented

✅ **Booking Creation with validation**
- License verification required
- Vehicle availability check
- Automatic fare calculation (days × rate)
- Status tracking (PENDING → CONFIRMED)

✅ **Vehicle Handover**
- Transition CONFIRMED → HANDED_OVER
- Vehicle marked BOOKED/unavailable

✅ **Return & Inspection**
- Complex workflow: mark returned + log damage + schedule maintenance
- Damage severity affects penalty
- Maintenance auto-scheduled if damage found

✅ **Damage Logging**
- Severity-based penalty calculation
- Records damage details & penalties
- Deducted from security deposit

✅ **Maintenance Scheduling**
- Auto-triggered when damage found
- Vehicle marked MAINTENANCE / unavailable
- Staff can mark completed → vehicle AVAILABLE again

✅ **Clean MVC + SOLID Principles**
- Separation of concerns (Controller → Service → Repository)
- Builder pattern for object creation
- Dependency injection for testability

---

## Integration with Other Persons

**Person 1 (Authentication & User Management):**
- Provides userId from login
- Provides license verification status

**Person 2 (Vehicle Browsing & Pricing):**
- Provides vehicle availability
- Provides pricePerDay for fare calculation
- Provides holiday pricing (can be integrated into fare calc)

**Person 4 (Payments & Admin):**
- Will handle payment collection after booking
- Will use booking data for reports
- Will receive penalty amounts from damage logs

---

## Testing the System

### 1. Create a Booking
```bash
curl -X POST http://localhost:8080/api/bookings?licenseVerified=true \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "vehicleId": "vehicle456",
    "startDate": "2025-06-01",
    "endDate": "2025-06-05"
  }'
```

### 2. View All Bookings
```bash
curl http://localhost:8080/api/bookings
```

### 3. Handover Vehicle
```bash
curl -X PUT http://localhost:8080/api/bookings/BOOKING_ID/handover
```

### 4. Return & Inspect
```bash
curl -X POST http://localhost:8080/api/bookings/return \
  -H "Content-Type: application/json" \
  -d '{
    "bookingId": "BOOKING_ID",
    "vehicleId": "vehicle456",
    "hasDamage": true,
    "damageDescription": "Scratched bumper",
    "damageSeverity": "MEDIUM"
  }'
```

### 5. Complete Maintenance
```bash
curl -X PUT http://localhost:8080/api/bookings/maintenance/MAINTENANCE_ID/complete
```

---

## Notes

1. **Database**: Ensure MongoDB is running
2. **Ports**: Backend on 8080, frontend on localhost JavaFX
3. **CORS**: Enabled on all controllers for frontend access
4. **Error Handling**: All endpoints return meaningful error messages
5. **Validation**: License, dates, availability all checked server-side

---

## What You've Completed

You've implemented the complete **Booking, Handover, Return & Inspection, and Maintenance** workflow with:

- ✅ 3 Service classes (Booking, Damage, Maintenance)
- ✅ 3Entity classes (Booking, Damage, MaintenanceRecord)
- ✅ 4 Repository interfaces
- ✅ 1 comprehensive Controller with 11 REST endpoints
- ✅ 2 Frontend services (Booking service, API communication)
- ✅ 1 enhanced FXML view with customer booking + staff inspection
- ✅ Full SOLID + Design Pattern compliance
- ✅ Integration-ready with Person 1, 2, and 4 modules

**Ready to compile and run!** 🚀
