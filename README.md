# 🚗 DriveAway — Full-Stack Vehicle Rental System

DriveAway is a feature-complete vehicle rental platform built with **Java (Spring Boot)** backend and **JavaFX** desktop frontend. It supports the full rental lifecycle: browse vehicles, book, pay, return, and get refunds — with a comprehensive admin interface for fleet management, booking oversight, payment approval, and reports.

---

## 🗂 Project Structure

```
Rental-vehicles/
├── driveaway-backend/      # Spring Boot REST API (Java 17, MongoDB)
└── driveaway-frontend/     # JavaFX desktop client
```

---

## 🚀 Quick Start

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.9+ |
| MongoDB | Atlas or local (v6+) |

### 1. Configure MongoDB

Set the `MONGO_URI` environment variable (or update `application.properties`):

```bash
export MONGO_URI="mongodb+srv://<user>:<password>@<cluster>.mongodb.net/driveaway"
```

For local MongoDB:
```bash
export MONGO_URI="mongodb://localhost:27017/driveaway"
```

### 2. Start the Backend

```bash
cd driveaway-backend
./mvnw spring-boot:run
```

The API starts on **http://localhost:8080**. Sample vehicles are seeded automatically on first run.

### 3. Start the Frontend

```bash
cd driveaway-frontend
mvn javafx:run
# or using the run script:
run.bat   # Windows
```

---

## 🔐 Admin Credentials

> **Note:** The app uses a header-based admin identity (`X-Admin-ID`). Any logged-in user ID is forwarded as the admin ID during local development.

| Field | Value |
|-------|-------|
| Admin User ID | Any registered user ID |
| Admin Access | Navigate to **Admin Dashboard** from the main dashboard nav bar (⚙️ Admin button) |

**Default seeded user (if using seed data):**
```
Register a new account via the Register screen.
The first user to log in can access all admin features.
```

---

## 🗺 Application Routes / Screens

### Frontend Screens (JavaFX FXML)

| Screen | FXML File | Description |
|--------|-----------|-------------|
| Login | `LoginView.fxml` | User authentication |
| Register | `RegisterView.fxml` | New user registration |
| Dashboard | `DashboardView.fxml` | Home page with stats, bookings, vehicles |
| Vehicle Catalog | `VehicleCatalogView.fxml` | Browse and filter available vehicles |
| Vehicle Details | `VehicleDetailsView.fxml` | Vehicle details + booking form |
| Booking Management | `BookingManagementView.fxml` | View and manage user bookings |
| Payment | `PaymentView.fxml` | Multi-method payment with in-app approval |
| **Admin Dashboard** | `AdminDashboardView.fxml` | ⚙️ **Admin: Fleet CRUD, bookings, payment approval, returns** |
| Reports | `ReportsView.fxml` | Analytics: vehicle, daily, weekly, monthly reports |
| Notifications | `NotificationsView.fxml` | In-app notifications |
| User Profile | `UserProfileView.fxml` | Edit profile |
| License | `LicenseView.fxml` | Driver's license management |

### Backend API Endpoints

#### Vehicles
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/vehicles` | List all vehicles |
| GET | `/api/v1/vehicles/available` | List available vehicles |
| GET | `/api/v1/vehicles/{id}` | Get vehicle by ID |
| POST | `/api/v1/vehicles` | Add vehicle (X-Admin-ID header required) |
| PUT | `/api/v1/vehicles/{id}` | Update vehicle (X-Admin-ID header) |
| DELETE | `/api/v1/vehicles/{id}` | Delete vehicle (X-Admin-ID header) |

#### Admin Fleet (Alias)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/admin/fleet` | List fleet (X-Admin-ID header) |
| POST | `/api/v1/admin/fleet` | Add vehicle to fleet |
| PUT | `/api/v1/admin/fleet/{id}` | Update vehicle in fleet |
| DELETE | `/api/v1/admin/fleet/{id}` | Remove vehicle from fleet |

#### Bookings
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/bookings` | Create booking (X-User-ID header) |
| GET | `/api/v1/bookings` | All bookings |
| GET | `/api/v1/bookings?status=active` | Active bookings (CONFIRMED + ACTIVE) |
| GET | `/api/v1/bookings?status=completed` | Completed bookings |
| GET | `/api/v1/bookings/user/{userId}` | User's bookings |
| POST | `/api/v1/bookings/{id}/cancel` | Cancel booking |
| POST | `/api/v1/bookings/{id}/complete` | Mark booking complete |
| POST | `/api/v1/bookings/{id}/return` | Process vehicle return with damage check |

#### Payments
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/payments/request` | **Step 1:** Initiate payment request (X-User-ID header) |
| POST | `/api/v1/payments/{id}/approve` | **Step 2:** Approve payment (X-Approved-By header) |
| POST | `/api/v1/payments/approve-by-token?token=...` | Approve via token |
| POST | `/api/v1/payments/{id}/complete` | **Step 3:** Complete payment (X-User-ID header) |
| POST | `/api/v1/payments/process` | Legacy direct payment flow |
| GET | `/api/v1/payments` | All payments (admin) |
| GET | `/api/v1/payments/{id}` | Get payment by ID |
| GET | `/api/v1/payments/user/{userId}` | User payments |
| POST | `/api/v1/payments/{id}/refund` | Refund payment (X-Admin-ID header) |
| POST | `/api/v1/payments/refund-deposit` | Refund/forfeit security deposit after return |

#### Admin Reports
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/admin/reports` | List all generated reports |
| GET | `/api/v1/admin/reports/vehicles` | Vehicle inventory report |
| GET | `/api/v1/admin/reports/daily` | Daily report |
| GET | `/api/v1/admin/reports/weekly` | Weekly report |
| GET | `/api/v1/admin/reports/monthly` | Monthly report |
| GET | `/api/v1/admin/dashboard` | Dashboard analytics |

#### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/login` | Login (returns user ID) |
| POST | `/auth/register` | Register new user |

---

## 🔑 Key Features

### A. Admin Vehicle Management
Access via **⚙️ Admin** button in the nav bar → Admin Dashboard:
- **Add Vehicle**: Fill in Brand, Model, Type, Price/Day → click ➕ Add Vehicle
- **Update Vehicle**: Click a row in the fleet table → edit fields → click ✏️ Update Selected
- **Delete Vehicle**: Click a row → click 🗑️ Delete Selected → confirm
- **View All Vehicles**: Fleet table with ID, Brand, Model, Type, Price/Day, Status columns

### B. Booking Management (Admin)
In Admin Dashboard, scroll to **All Bookings** section:
- **All**: Shows all bookings across all users
- **Active**: Shows bookings with status CONFIRMED or ACTIVE
- **Completed**: Shows bookings with status COMPLETED
- **Refresh**: Re-fetches the current filter

### C. In-App Payment Approval Flow
1. User submits payment via **PaymentView** → click "Submit Payment Request"
2. System creates payment in `REQUESTED` state and generates an approval token
3. The **Approval Section** appears in the payment form → click "🔒 Approve Payment (In-App)"
4. Payment moves to `APPROVED` state; admin can also approve via Admin Dashboard
5. Click "Complete Payment" → payment processed and booking status updated to `ACTIVE`

**Admin approval**: In Admin Dashboard → Payment Approval Panel → enter Payment ID → click "🔒 Approve Payment"

### D. Netbanking Flow
When selecting **Net Banking** as payment method:
- **Select Bank**: Choose from dropdown (HDFC, SBI, ICICI, Axis, Kotak, Yes Bank, PNB, BOB)
- **Account Holder Name**: Full name as per bank records
- **Account Number**: 10–18 digit account number
- **IFSC Code**: Format validated (e.g., `HDFC0001234`)

Client and server validation ensures all fields are present before submission.

### E. Payment Lifecycle
```
REQUESTED → PENDING_APPROVAL → APPROVED → COMPLETED/FAILED
                                              ↓
                                          REFUNDED
```
- **Security Deposit**: Captured at payment time, stored as `HELD`
- **After Return**: Deposit status updated to `REFUNDED` (no damage) or `FORFEITED` (damage ≥ deposit)

### F. Return, Damage Check, Refund
In Admin Dashboard → **Process Vehicle Return & Damage Check**:
1. Enter Booking ID
2. Enter Damage Charge (₹0 = no damage, full deposit refunded)
3. Enter Damage Notes (optional)
4. Click "📦 Process Return"

Booking status → `RETURNED`; vehicle → `AVAILABLE`; deposit → `REFUNDED` or `FORFEITED`.

### G. Reports & Analytics
Navigate to **📊 Reports** in the nav bar:
- **Summary Cards**: Total Vehicles, Available, Total Revenue, Reports Generated
- **Vehicle Inventory**: Full fleet status breakdown (total / available / booked / maintenance)
- **Daily Report**: Today's bookings and revenue
- **Weekly Report**: 7-day overview
- **Monthly Report**: 30-day summary
- **Report Table**: Colour-coded by type (🔵 Vehicle, 🟢 Daily, 🟡 Weekly, 🟣 Monthly) with revenue column

---

## 🧪 Running Tests

```bash
cd driveaway-backend
mvn test
```

### Test Coverage (88 unit tests)

| Test Class | Tests | Coverage |
|------------|-------|----------|
| `VehicleServiceTest` | 13 | Add, Update, Delete vehicle, mark booked/available, audit logging |
| `BookingServiceTest` | 19 | Complete, Cancel, Create booking, ProcessReturn with deposit, Status filtering (all/active/completed) |
| `PaymentServiceTest` | 22 | Initiate request, Approve, Complete, Approve-by-token, Netbanking validation, Deposit refund/forfeit |
| `NotificationServiceTest` | 17 | In-app notification delivery |
| `ReportServiceTest` | 4 | Vehicle/Daily/Weekly/Monthly report generation |
| `AuthServiceTest` | 5 | Registration, login |
| `PaymentEmailVerificationServiceTest` | 5 | Token verification |
| `VehicleDataSeederTest` | 2 | Database seeding |
| `DriveawayApplicationTests` | 1 | Context load (requires live MongoDB – expected to fail in CI without DB) |

> **Note:** `DriveawayApplicationTests.contextLoads` requires a live MongoDB connection. This is a pre-existing known failure in environments without database connectivity. All other 87 tests pass without a database (pure unit tests using Mockito).

---

## 🛡 Security Notes

- Net banking account numbers and IFSC codes are **validated server-side** before processing
- Sensitive payment fields (CVV, card number) are **never logged**
- All admin operations require the `X-Admin-ID` header
- Payment approval tokens (`APPR_...`) are single-use and tied to the payment record
- MongoDB URI is read from environment variable `MONGO_URI` (never hardcoded)

---

## 🔄 Real-time UX

- After any vehicle CRUD operation, the fleet table **automatically refreshes**
- After booking filter changes (All/Active/Completed), the table **immediately updates**
- After payment approval, the approval section **shows in-app without page reload**
- Status labels provide **immediate visual feedback** for every action (✅/❌/⚠️)

---

## 🏗 Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3, Spring Data MongoDB, Spring Security |
| Database | MongoDB Atlas (cloud) or local MongoDB |
| Frontend | JavaFX 21, FXML |
| HTTP Client | Java `HttpClient` (no extra libs) |
| Testing | JUnit 5, Mockito |
| Build | Apache Maven |
| Notifications | In-app (JavaFX) + optional Twilio SMS |
