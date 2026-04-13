# Person 3 - Quick Start Guide

## Step 1: Verify All Files Are Created

Run this command in the root directory to confirm all backend files are present:

```powershell
Set-Location "DriveAway-Vehicle-Rental-System"

# Check backend files
Get-ChildItem -Recurse -Path "driveaway-backend/src/main/java/com/driveaway" -Include "Booking*.java", "Damage*.java", "Maintenance*.java", "InspectionRequest.java" | Select-Object Name, @{Label="Size(bytes)"; Expression={$_.Length}} | Format-Table -AutoSize
```

Expected output (all files should exist with > 0 bytes):
- BookingController.java
- BookingRequest.java
- BookingRepository.java
- BookingService.java
- DamageRepository.java
- DamageService.java
- InspectionRequest.java
- Damage.java
- MaintenanceRepository.java
- MaintenanceService.java
- MaintenanceRecord.java

---

## Step 2: Build Backend with Maven

```bash
cd driveaway-backend

# Clean and compile
mvn clean install

# If compilation succeeds, start the backend
mvn spring-boot:run
```

**Expected Output:**
```
Tomcat started on port(s): 8080
Started DriveawayApplication
```

**Leave this terminal open** - backend must keep running.

---

## Step 3: Verify Backend is Running

In a **new terminal** window, test a backend endpoint:

```powershell
# Test if backend is running
Invoke-WebRequest -Uri "http://localhost:8080/api/bookings" -Method GET
```

You should see a JSON response (currently empty `[]` if no bookings exist).

---

## Step 4: Compile Frontend

Open a **new terminal** window and navigate to the frontend directory:

```bash
cd driveaway-frontend

# Compile all Java source files with JavaFX modules
javac --module-path javafx-sdk-25.0.2/lib --add-modules javafx.controls,javafx.fxml -d . src/com/driveaway/*.java src/com/driveaway/controllers/*.java src/com/driveaway/services/*.java src/com/driveaway/utils/*.java src/com/driveaway/models/*.java

# Copy FXML view files to the compiled directory
xcopy src\com\driveaway\views com\driveaway\views /E /I
```

**Verify compilation succeeded** - no errors should appear.

---

## Step 5: Run Frontend

```bash
java --module-path javafx-sdk-25.0.2/lib --add-modules javafx.controls,javafx.fxml com.driveaway.MainApp
```

**Expected:** JavaFX window opens with "DriveAway Vehicle Rental System" login screen.

---
.\mvnw clean compile
.\mvnw javafx:run

## Step 6: Test the Complete Flow

### 6a. Login (Person 1)
- Email: `test@email.com` (or any test email from Person 1's database)
- Password: `password123`

### 6b. Add License (Person 1)
- License Number: `ABC123XYZ`
- Expiry Date: `2030-12-31`

### 6c. Create a Booking (Your Part!)

In the **Booking View**, fill the **Customer Booking** section:

1. **Vehicle ID**: `V001` (or any vehicle ID from the system)
2. **Start Date**: Click datepicker, choose today's date
3. **End Date**: Click datepicker, choose 5 days from today
4. **License Verified**: ✓ Check this box
5. Click **"Book Vehicle"** button

**Expected Result:**
- Popup or status shows: "Booking created successfully" or confirmation message
- Backend logs show the booking was saved to MongoDB

---

### 6d. Test Return & Inspection (Your Part!)

In the **Return & inspection** section:

1. **Booking ID**: Copy the booking ID from the booking creation response
2. **Vehicle ID**: Same as the vehicle you booked (V001)
3. **Damage Found**: Check the box
4. **Damage Description**: Type "Scratched bumper"
5. **Damage Severity**: Select "MEDIUM"
6. Click **"Submit Return Inspection"** button

**Expected Result:**
- Status shows: "Vehicle returned. Damage logged (MEDIUM). Penalty: Rs.6000. Maintenance scheduled."
- In backend logs, you'll see maintenance scheduled for the next day

---

### 6e. Verify in Backend

While frontend is running, in **another terminal**, test the backend endpoints:

```powershell
# Get all bookings
curl http://localhost:8080/api/bookings

# Get damages for booking (replace BOOKING_ID)
curl http://localhost:8080/api/bookings/damages/BOOKING_ID

# Get scheduled maintenance
curl http://localhost:8080/api/bookings/maintenance/scheduled
```

---

## Step 7: Check MongoDB Data

If you have MongoDB installed locally, you can verify data directly:

```bash
# Connect to MongoDB
mongo

# Use driveaway database
use driveaway

# View bookings collection
db.bookings.find().pretty()

# View damages collection
db.damages.find().pretty()

# View maintenance records
db.maintenance_records.find().pretty()
```

---

## Common Issues & Solutions

### Issue: Backend won't start
**Solution:**
- Ensure MongoDB is running: `mongod` in command prompt
- Check port 8080 is not in use: `netstat -ano | findstr :8080`
- If in use, kill process or start Spring Boot on different port

### Issue: Frontend compilation fails
**Solution:**
- Ensure JavaFX SDK path is correct in the command
- Check all source files are in `src/com/driveaway/` structure
- Recompile with: `javac --module-path javafx-sdk-25.0.2/lib --add-modules javafx.controls,javafx.fxml -d . src/com/driveaway/**/*.java`

### Issue: POST endpoints return error
**Solution:**
- Backend must be running (`mvn spring-boot:run`)
- Check license is verified before booking
- Ensure vehicle ID exists in system

### Issue: FXML files not found
**Solution:**
- Run: `xcopy src\com\driveaway\views com\driveaway\views /E /I`
- Verify files exist in `com/driveaway/views/`

---

## Debugging Tips

### Enable verbose output on frontend:
```bash
java -verbose:class --module-path javafx-sdk-25.0.2/lib --add-modules javafx.controls,javafx.fxml com.driveaway.MainApp
```

### Check backend logs for errors:
- Look for stack traces in the terminal running `mvn spring-boot:run`
- Common issues: MongoRepository not found, service not autowired

### Monitor HTTP requests (Frontend to Backend):
- Open browser DevTools while running
- Check Network tab for failed POST/PUT requests
- Look for 400/500 errors and read response body

---

## Architecture Summary

Your Person 3 implementation follows this flow:

```
JavaFX Frontend
    ↓
BookingController.java (UI events)
    ↓
BookingService.java (HTTP POST to backend)
    ↓
Spring Boot Backend
    ↓
BookingController.java (REST API)
    ↓
BookingService.java (Business logic)
    ↓
BookingRepository.java (MongoDB CRUD)
    ↓
MongoDB (persistence)

---

Return & Inspection Flow (Complex):
    ↓
BookingService.returnVehicle()
    ↓
DamageService.logDamage()
    ↓
MaintenanceService.scheduleMaintenance()
    ↓
VehicleRepository (mark unavailable)
```

---

## Next Steps After Verification

1. **Integrate with Person 1's Auth Module**: Ensure token/session is passed correctly
2. **Integrate with Person 2's Vehicle Module**: Verify vehicle availability checking works
3. **Integrate with Person 4's Payment Module**: Link penalty amounts to security deposit deduction
4. **Add UI Refinements**: Error dialogs, loading spinners, booking history view
5. **Add Admin Dashboard**: Staff view of all bookings, maintenance status, inspection history

---

## Files Location Reference

```
DriveAway-Vehicle-Rental-System/
├── driveaway-backend/
│   └── src/main/java/com/driveaway/
│       ├── controller/
│       │   └── BookingController.java ✅
│       ├── service/
│       │   ├── BookingService.java ✅
│       │   ├── DamageService.java ✅
│       │   └── MaintenanceService.java ✅
│       ├── repository/
│       │   ├── BookingRepository.java ✅
│       │   ├── DamageRepository.java ✅
│       │   ├── MaintenanceRepository.java ✅
│       │   └── VehicleRepository.java ✅
│       ├── entity/
│       │   ├── Booking.java ✅
│       │   ├── Damage.java ✅
│       │   └── MaintenanceRecord.java ✅
│       └── dto/
│           ├── BookingRequest.java ✅
│           └── InspectionRequest.java ✅
│
├── driveaway-frontend/
│   └── src/com/driveaway/
│       ├── controllers/
│       │   └── BookingController.java ✅
│       ├── services/
│       │   └── BookingService.java ✅
│       ├── views/
│       │   └── BookingView.fxml ✅
│       └── models/
│           └── Booking.java
│
└── PERSON3_IMPLEMENTATION.md ✅
```

---

**You're all set! Run through Steps 1-7 and your Person 3 booking system will be fully operational.** 🎉
