# Staff User Setup & Testing Guide

## Step 1: Insert New Staff User into MongoDB

**Copy and paste this into MongoDB Atlas Data Explorer** (Collections → driveaway_db → users → ADD DATA):

```javascript
// Delete old staff if exists
db.users.deleteOne({ email: "john.staff@driveaway.com" });

// Insert NEW staff user
db.users.insertOne({
  name: "John Staff",
  email: "john.staff@driveaway.com",
  password: "$2a$10$9Z0kVl2l7V3QqKzPqM8nRO1XYz5QZ5XYz5QZ5XYz5QZ5XYz5QZ5XY",
  role: "STAFF",
  phone: "9876543210"
});

// Verify it was inserted
db.users.findOne({ email: "john.staff@driveaway.com" });
```

**Password for this staff user:** `password123`

---

## Step 2: Customer Creates a Booking (Setup Handover)

**Login as Customer FIRST:**
- Email: `sushmav418@gmail.com` (or any existing customer)
- Password: `password123`

**Create a booking:**
1. Vehicle: Select any available vehicle
2. Start Date: Today
3. End Date: 5 days from today
4. ✓ Check "License Verified"
5. Click "Book Vehicle"

**Note the Booking ID** — you'll need it for staff inspection

**Request Handover:**
1. Click "My Bookings"
2. Select the booking you just created
3. Click "Request Handover"
4. Status should show: "✅ Handover done! Staff can now see this in their dashboard."

---

## Step 3: Staff Logs In & Sees Handover

**Logout** (click Logout button)

**Login as NEW staff:**
- Email: `john.staff@driveaway.com`
- Password: `password123`

**Expected:**
- ✅ Sees "Staff Dashboard" (NOT BookingView)
- ✅ Header shows "👤 John Staff | Role: STAFF"
- ✅ "Pending Handover Requests" section shows the booking you just handed over

---

## Step 4: Staff Submits Return Inspection (Damage Logic)

**In Staff Dashboard:**

1. **In "Pending Handover Requests" section:**
   - Click on the booking from the list
   - Fields auto-populate: Booking ID and Vehicle ID

2. **In "Return and Inspection" form:**
   - **Booking ID:** Auto-filled
   - **Vehicle ID:** Auto-filled
   - **Damage Found on Vehicle:** ☑️ **CHECK THIS**
   - **Damage Description:** Type "Dented door panel"
   - **Severity:** Select "MEDIUM"
   - Click "Submit Return Inspection"

**Expected Result:**
```
✅ Vehicle returned. Damage: MEDIUM. Penalty: Rs.6000. Maintenance scheduled.
```

---

## Step 5: Verify Backend Logic

**Open another terminal and run:**

```powershell
# Get all bookings (should show status as RETURNED)
curl http://localhost:8080/api/bookings

# Get damages for the booking (replace BOOKING_ID)
curl http://localhost:8080/api/bookings/damages/BOOKING_ID

# Get scheduled maintenance
curl http://localhost:8080/api/bookings/maintenance/scheduled
```

**Expected MongoDB changes:**
```javascript
// Booking status should be RETURNED
db.bookings.findOne({ _id: ObjectId("BOOKING_ID") });

// Damage should be logged
db.damages.find({ bookingId: "BOOKING_ID" });

// Maintenance should be scheduled
db.maintenance_records.find({ vehicleId: "VEHICLE_ID" });
```

---

## Complete Staff Flow Checklist

- [ ] Staff user inserted in MongoDB with role: "STAFF"
- [ ] Staff login works (doesn't see customer screens)
- [ ] Staff Dashboard loads with correct header
- [ ] Staff sees pending handover requests
- [ ] Clicking handover auto-fills inspection form
- [ ] Submitting damage creates maintenance record
- [ ] Backend penalty calculation is correct (Rs.6000 for MEDIUM)
- [ ] Vehicle status changes to MAINTENANCE (unavailable)

---

**If any step fails, check the error message and let me know!**
