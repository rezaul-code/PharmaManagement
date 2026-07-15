# Backend REST Migration Plan

This document outlines the detailed architectural blueprint and implementation steps to migrate the Pharma Management System from a monolithic **Spring Boot + Thymeleaf** structure to a decoupled **Spring Boot REST API + Next.js** architecture.

---

# 1 Project Overview

The application is a multi-tenant Software-as-a-Service (SaaS) Pharmacy Management System. It allows pharmacy owners, pharmacists, and staff members to manage medicines (inventories, low stock warnings, expiry dates), perform billing (invoice creation, CGST/SGST calculations, credit bill settlements), view reports/analytics (sales charts, monthly trends, profit margins), and manage pharmacy employee roles.

### Core Architectural Features:
* **Multi-Tenancy**: Scoped entirely to individual `Pharmacy` records via a thread-local `TenantContext` loaded on each request through JWT token claims.
* **Security & Authorization**: BCrypt password hashing and role-based permissions (SUPER_ADMIN, OWNER, PHARMACIST, STAFF).
* **Billing System**: Computes taxes and manages credit ledger payment schedules.
* **File Uploads**: Handles store logo images saved directly to the server's local file storage.
* **Reporting**: Generates PDF invoice summaries (via OpenPDF) and sheet exports (via Apache POI).

---

# 2 Architecture Diagram

### Current Monolithic Architecture

```
Browser
   │  (HTTP Requests: HTML/CSS/JS)
   ▼
Spring Security
   │  (OncePerRequestFilter - JwtTokenFilter validation)
   ▼
Spring MVC Controller
   │  (Populates Org.Springframework.Ui.Model, returns template name)
   ▼
Thymeleaf Template Engine
   │  (Compiles pages/*.html + sidebar.html with Model values)
   ▼
Service Layer (Tenant Context resolved)
   │  (TenantPharmacyService, BillingService, MedicineService)
   ▼
JPA Repositories
   │  (Spring Data JPA / Hibernate)
   ▼
MySQL Database
```

### Future Decoupled Architecture

```
Next.js Frontend (Port 3000)
   │  (JSON API request with Bearer JWT token header)
   ▼
Reverse Proxy (Nginx / Next.js Rewrite) [Optional / Recommended]
   │
   ▼
Spring Security (Port 8080)
   │  (Stateless API Security - JWT Token Header Filter - CORS configured)
   ▼
Spring REST Controllers (@RestController)
   │  (Returns JSON DTOs, Page objects, or binary files)
   ▼
Service Layer (Tenant Context resolved)
   │  (Uses same ThreadLocal TenantContext)
   ▼
JPA Repositories
   │  (Identical SQL queries and Hibernate mappings)
   ▼
MySQL Database
```

---

# 3 Module Inventory

| Module | Controller | Service | Repository | Views | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Authentication & Sign Up** | `LoginController`, `SignUpController`, `AuthController` | `AuthService`, `UserService`, `TenantRegistrationService` | `UserRepository`, `PharmacyRepository` | `templates/index.html`, `user_auth/user_login.html`, `user_auth/user_signup.html` | UI views to be retired; REST APIs ready. |
| **Dashboard** | `HelloController`, `SalesChartController` | `MedicineService`, `BillingService`, `TenantPharmacyService` | `BillingRepository`, `MedicineRepository` | `templates/pages/dashboard.html` | Migrate UI dashboard to REST endpoints. |
| **Medicine / Inventory** | `MedicineController` | `MedicineService` | `MedicineRepository` | `templates/pages/show_medicine.html`, `add_medicine.html`, `med_edit.html`, `low_stock.html`, `expiring_medicines.html` | Migrate views to REST CRUD APIs. |
| **Billing & Invoices** | `BillingController` | `BillingService`, `PdfInvoiceService`, `TenantPharmacyService` | `BillingRepository`, `BillingItemRepository`, `CreditPaymentRepository` | `templates/pages/billing_new.html`, `billing_view.html`, `billing_list.html`, `billing_credit.html`, `today_sales.html` | Needs REST refactoring for billing CRUD, credit payments, and PDF fetch. |
| **Analytics & Reports** | `SalesAnalyticsController`, `ExportController` | `SalesAnalyticsService`, `ExportService`, `AuditLogService` | `BillingRepository`, `MedicineRepository`, `AuditLogRepository` | `templates/pages/sales-analytics.html`, `inventory-analytics.html`, `monthly-trends.html` | Expose data aggregates as JSON DTOs. |
| **Staff Management** | `StaffController` | `StaffService` | `UserRepository` | `templates/pages/staff.html` | Migrate lists, role updates, and approvals to REST. |
| **Pharmacy Settings** | `PharmacyController` | `TenantPharmacyService`, `AuditLogService` | `PharmacyRepository` | `templates/pages/pharmacy_settings.html` | Convert updates, logo uploads, and logo removals to REST. |
| **Super Admin** | `SuperAdminController` | None | `PharmacyRepository`, `SubscriptionRepository` | `templates/superadmin/dashboard.html` | UI dashboard to be converted to REST. |

---

# 4 Controller Analysis

### 4.1 AuthController (@RestController)
* **Endpoints**:
  * `POST /api/auth/login`: Accepts `LoginDto` payload.
  * `POST /api/auth/register-pharmacy`: Accepts `RegistrationDto` payload.
* **Returned Views**: None (returns JSON payloads).
* **Services Used**: `AuthService`, `TenantRegistrationService`.
* **Authentication / Roles**: Public access (`permitAll()`).
* **Model Attributes**: None.

### 4.2 LoginController (@Controller)
* **Endpoints**:
  * `GET /login`: Renders the login page.
  * `POST /login`: Processes username/password, generates JWT, saves token to cookie `jwt`, sets HTTP Session attributes, and signs into Security Context.
  * `GET /logout`: Invalidate session and clear the `jwt` cookie.
  * `GET /`: Route index landing page.
* **Returned Views**: `user_auth/user_login`, `index`, redirects to `/dashboard` or `/login`.
* **Services Used**: `UserService`, `AuthService`.
* **Authentication / Roles**: Public access.
* **Model Attributes**: `success`, `error`.

### 4.3 SignUpController (@Controller)
* **Endpoints**:
  * `GET /signup`: Show signup view.
  * `POST /signup`: Capture user registration and pharmacy info from form fields, hashes password, saves `Pharmacy` and `User` (role: OWNER, status: PENDING).
* **Returned Views**: `user_auth/user_signup`, redirect to `/login?registered=true`.
* **Services Used**: `UserService` (uses `PharmacyRepository` directly).
* **Authentication / Roles**: Public access.
* **Model Attributes**: `user`, `error`.

### 4.4 HelloController (@Controller)
* **Endpoints**:
  * `GET /dashboard`: Fetches pharmacy context, stats, recent bills list, and charts payload.
* **Returned Views**: `pages/dashboard`.
* **Services Used**: `MedicineService`, `BillingService`, `TenantPharmacyService` (uses `BillingRepository` and `MedicineRepository` directly).
* **Authentication / Roles**: Authenticated users.
* **Model Attributes**: `pharmacy`, `totalMedicines`, `lowStockCount`, `expiringCount`, `todaySales`, `lowStockMeds`, `recentBills`, `salesChartJson`, `stockChartJson`.

### 4.5 SalesChartController (@RestController)
* **Endpoints**:
  * `GET /api/dashboard/sales-chart`: Returns dynamic chart JSON array based on period (`7d`, `30d`, `90d`, `1y`).
* **Returned Views**: None (JSON string directly).
* **Services Used**: `TenantPharmacyService` (uses `BillingRepository` directly).
* **Authentication / Roles**: Authenticated users.
* **Model Attributes**: None.

### 4.6 MedicineController (@Controller)
* **Endpoints**:
  * `GET /medicine/show` (Legacy: `/show_medicine`): Paginated, searchable, sorted list of medicines.
  * `GET /medicine/add` (Legacy: `/add_medicine`): Render add medicine form.
  * `POST /medicine/add` (Legacy: `/add_medicine`): Processes new medicine creation.
  * `GET /medicine/edit/{id}` (Legacy: `/med_edit`): Render edit medicine form.
  * `POST /medicine/edit/{id}` (Legacy: `/med_edit`): Processes medicine update.
  * `GET /medicine/delete/{id}` (Legacy: `/med_delete`): Deletes medicine.
  * `GET /medicine/low-stock`: Lists medicines with stock <= 10.
  * `GET /medicine/expiring`: Lists medicines expiring within 30 days.
* **Returned Views**: `pages/show_medicine`, `pages/add_medicine`, `pages/med_edit`, `pages/low_stock`, `pages/expiring_medicines`, redirect to `/medicine/show` or `/login`.
* **Services Used**: `MedicineService` (uses `MedicineRepository` directly).
* **Authentication / Roles**: Authenticated users for listing. Writes (add, edit, delete) require `canManageMedicines()` (OWNER or PHARMACIST).
* **Model Attributes**: `medicinePage`, `medicines`, `types`, `searchId`, `searchName`, `searchDescription`, `searchType`, `searchMedicineCode`, `currentPage`, `pageSize`, `sortField`, `sortDir`, `reverseSortDir`, `currentUser`, `activePage`, `medicine`, `outOfStockCount`, `criticalCount`, `lowCount`, `today`, `expiredOrTodayCount`, `withinWeekCount`.

### 4.7 BillingController (@Controller)
* **Endpoints**:
  * `GET /billing/new`: Render bill creation canvas.
  * `POST /billing/create`: Submit bill form with validation.
  * `GET /billing/view/{id}`: View invoice summary, payment transactions, and records credit capture.
  * `GET /billing/list`: Historical invoices filtered by year/month.
  * `GET /billing/credit`: View outstanding balance ledger.
  * `POST /billing/credit/{id}/pay`: Submit partial/full credit pay transactions.
  * `GET /billing/medicine/search`: AJAX list lookup for bill items creation.
  * `GET /billing/medicine/{id}`: AJAX metadata fetch for auto-filling item rows.
  * `GET /billing/today`: Today's business metrics.
  * `GET /billing/{id}/pdf`: Generate invoice PDF byte stream.
* **Returned Views**: `pages/billing_new`, `pages/billing_view`, `pages/billing_list`, `pages/billing_credit`, `pages/today_sales`, redirects, or JSON response / raw PDF stream.
* **Services Used**: `BillingService`, `MedicineService`, `TenantPharmacyService`, `PdfInvoiceService` (uses `BillingRepository` directly).
* **Authentication / Roles**: Authenticated users.
* **Model Attributes**: `billingForm`, `paymentTypes`, `currentUser`, `error`, `billing`, `pharmacy`, `creditPaymentForm`, `billPage`, `bills`, `currentPage`, `pageSize`, `selectedYear`, `selectedMonth`, `years`, `creditSummary`, `today`, `todayTotal`, `todayGst`, `pendingBalance`, `creditCount`.

### 4.8 SalesAnalyticsController (@Controller)
* **Endpoints**:
  * `GET /sales-analytics`: Quantity, revenue, and profit charts.
  * `GET /inventory-analytics`: Total stock purchase value, selling value, and margin alerts.
  * `GET /monthly-trends`: Month-by-month financial statements.
* **Returned Views**: `pages/sales-analytics`, `pages/inventory-analytics`, `pages/monthly-trends`.
* **Services Used**: `SalesAnalyticsService`, `MedicineService`.
* **Authentication / Roles**: Authenticated users.
* **Model Attributes**: `topMedicines`, `summary`, `metric`, `period`, `topN`, `chartLabels`, `chartValues`, `chartQty`, `chartRevenue`, `chartProfit`, `trendLabels`, `trendValues`, `activePage`, `totalCostValue`, `totalSellingValue`, `potentialProfit`, `negativeMarginCount`, `monthlyTrend`, `monthlyLabels`, `monthlyRevenue`, `monthlyProfit`, `monthlyUnits`, `viewMode`, `activePeriod`, `periodLabel`, `selectedYear`, `selectedMonth`, `yearOptions`.

### 4.9 ExportController (@Controller)
* **Endpoints**:
  * `GET /export/inventory`: Excel/CSV export of stock.
  * `GET /export/sales`: Excel/CSV export of transactions.
* **Returned Views**: None (binary attachment files).
* **Services Used**: `ExportService`, `AuditLogService`, `TenantPharmacyService`.
* **Authentication / Roles**: Authenticated users with roles OWNER or PHARMACIST.
* **Model Attributes**: None.

### 4.10 StaffController (@Controller)
* **Endpoints**:
  * `GET /staff`: View employee list and counts.
  * `POST /staff/create`: Add pharmacist/staff and enforce constraints (Max: 1 Pharmacist, 2 Staff).
  * `POST /staff/{id}/role`: Adjust permissions.
  * `POST /staff/{id}/delete`: Remove employee records.
  * `POST /staff/{id}/approve`: Approve pending invitations.
* **Returned Views**: `pages/staff`, redirects.
* **Services Used**: `StaffService` (uses `UserRepository` directly).
* **Authentication / Roles**: OWNER only.
* **Model Attributes**: `staffList`, `roles`, `currentUser`, `pharmacyName`, `canAddPharmacist`, `canAddStaff`.

### 4.11 PharmacyController (@Controller)
* **Endpoints**:
  * `GET /pharmacy/settings`: Load store settings form.
  * `POST /pharmacy/settings`: Processes text configuration settings updates.
  * `POST /pharmacy/upload-logo`: Processes logo file upload (max 5MB, validates PNG/JPG content type).
  * `POST /pharmacy/remove-logo`: Deletes logo files and removes database reference.
* **Returned Views**: `pages/pharmacy_settings`, redirects.
* **Services Used**: `TenantPharmacyService`, `AuditLogService` (uses `PharmacyRepository` directly).
* **Authentication / Roles**: OWNER only.
* **Model Attributes**: `pharmacy`.

### 4.12 SuperAdminController (@Controller)
* **Endpoints**:
  * `GET /admin/pharmacies`: Renders superadmin control board.
  * `GET /admin/subscriptions`: Renders subscription configs (placeholder).
* **Returned Views**: `superadmin/dashboard`.
* **Services Used**: None.
* **Authentication / Roles**: SUPER_ADMIN role.
* **Model Attributes**: None.

---

# 5 Thymeleaf Dependency Analysis

The application uses Thymeleaf (22 files, including 1 shared fragment) to compile dynamic serverside HTML views.

### 5.1 Reusable Fragments
* `templates/fragments/sidebar.html` (defines fragment `sidebar`): Nav bar references `session.loggedInUser` for checking permissions, rendering credentials, and displaying role badges. Highlight state relies on `activePage` controller model parameter.

### 5.2 Form Binding
Forms are mapped to Spring backing forms and models using `th:object` and `th:field`:
* **Medicine Forms** (`add_medicine.html`, `med_edit.html`): Binds to `Medicine` object with fields: `name`, `description`, `price`, `purchasePrice`, `stockQuantity`, `expiryDate`, `type`.
* **Billing Form** (`billing_new.html`): Binds to `BillingForm` DTO. Uses dynamic table lines inputs mapping to `items[__${rowStat.index}__].medicineId` and `items[__${rowStat.index}__].quantity`.
* **Credit Payment Form** (`billing_view.html`): Binds to `CreditPaymentForm` DTO matching `amountPaid`.
* **Pharmacy Settings Form** (`pharmacy_settings.html`): Binds to `Pharmacy` entity matching `name`, `gstNumber`, `phone`, `address`, and `invoiceFooter`.
* **Signup Form** (`user_signup.html`): Binds to `User` backing object with input inputs for custom HTTP parameters representing store fields (`pharmacyName`, `pharmacyAddress`, etc.).

### 5.3 Redirects
MVC controllers issue `redirect:` commands to control user workflow:
* `redirect:/dashboard`: Landing zone after log in, or unauthorized redirects.
* `redirect:/login`: Redirect when credentials fail or sessions time out.
* `redirect:/login?registered=true`: Redirected from SignUpController.
* `redirect:/medicine/show`: Success redirect after additions/updates/deletions.
* `redirect:/billing/view/{id}`: Redirected after creating a bill or recording credit payments.
* `redirect:/staff`: Redirected after CRUDing or approving staff.
* `redirect:/pharmacy/settings`: Redirected after setting saves, logo uploads, and removals.

### 5.4 Session Attributes
The application relies heavily on servlet containers to cache logged-in states:
* `session.loggedInUser`: The current authenticated user. It holds permissions, username, and role.
* `session.pharmacyId`: Used as a fallback tenant filter.

---

# 6 REST Readiness

The modules must be migrated from server-side rendering to stateless REST APIs.

### 6.1 Authentication (NEEDS SMALL CHANGE)
* **Reason**: Exists partially in `AuthController` (`/api/auth/login` and `/api/auth/register-pharmacy`). We need to add `/api/auth/me` to retrieve authorization payloads, retire HTTP sessions in web security configuration, and set up cookie/header JWT validations inside `SecurityConfig.java` to make the REST client completely stateless.

### 6.2 Dashboard (NEEDS SMALL CHANGE)
* **Reason**: The dashboard charts (`SalesChartController`) are already written as REST resources. The primary task is rewriting `HelloController` to expose a `@RestController` returning statistic cards summaries as clean JSON DTOs.

### 6.3 Medicine (NEEDS SMALL CHANGE)
* **Reason**: The business logic is already isolated. The controller simply needs a class annotation rewrite (from `@Controller` to `@RestController`) and replacing view names with `ResponseEntity` returning medicine models, lists, and paginated wrappers.

### 6.4 Billing (NEEDS MAJOR CHANGE)
* **Reason**: While standard list lookups are basic REST actions, the invoice PDF rendering (/billing/{id}/pdf) outputs binary output streams. Because frontend REST clients (like Next.js on port 3000) cannot trigger browser downloads directly via standard links with authorization headers, a stateless approach must be devised. Options include using an authorization query param (e.g. `/api/billing/{id}/pdf?token=...`) or creating short-lived temporary download links.

### 6.5 Analytics & Reports (NEEDS SMALL CHANGE)
* **Reason**: Requires exposing calculated charts statistics (previously loaded as Spring UI models) as JSON lists. The Excel/CSV exports in `ExportController` must be converted to REST resources returning download streams, requiring the same frontend authorization headers logic as PDF generation.

### 6.6 Staff (NEEDS SMALL CHANGE)
* **Reason**: Straightforward transition from HTML table form submittals to JSON API requests. Validation responses must return JSON error strings on constraints failures (like employee size limits).

### 6.7 Pharmacy Settings (NEEDS MAJOR CHANGE)
* **Reason**: The settings form handles multi-part logo file uploads (`/pharmacy/upload-logo`). The controller logic must be converted to a REST endpoint returning success metadata. In addition, serving static directories (`uploads/logos/**`) requires CORS enablement so the Next.js client can load images across different host ports.

---

# 7 Migration Order

To minimize disruption, the safest order for modular migration is:

```
1. Authentication ──► 2. Pharmacy Settings ──► 3. Medicine Catalog
       │
       ▼
4. Staff Management ──► 5. Billing Engine ──► 6. Dashboard Stats
                                                  │
                                                  ▼
                                            7. Analytics & Charts ──► 8. Exports & Downloads
```

### Rationale:
1. **Authentication**: Sets up stateless security configuration, CORS mappings, and user contexts, which are needed for all subsequent endpoints.
2. **Pharmacy Settings**: Permits verifying basic profile lookups and file upload mechanisms early.
3. **Medicine Catalog**: Provides a simple, standalone catalog module to verify paginated list tables, searches, and edit models.
4. **Staff Management**: Validates role-based write guards and invites workflows under OWNER credentials.
5. **Billing Engine**: The core business engine; relies on medicines and users already functioning statelessly.
6. **Dashboard Stats**: Aggregates medicine quantities and transaction sales into KPI statistics cards.
7. **Analytics**: Builds chart aggregates once actual billing transactions are recorded in database tables.
8. **Exports & Downloads**: Handles POI spreadsheet streams and OpenPDF files as final refinements.

---

# 8 Files That Will Change

| File | Reason | Estimated Change |
| :--- | :--- | :--- |
| [pom.xml](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/pom.xml) | Retire template engines. | Remove `spring-boot-starter-thymeleaf`. |
| [SecurityConfig.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/config/SecurityConfig.java) | Stateless API configuration. | Set `SessionCreationPolicy.STATELESS`, register CORS filter bean permitting Next.js host origins, and configure JWT path permit mappings. |
| [JwtTokenFilter.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/security/JwtTokenFilter.java) | Stateless request processing. | Remove HTTP Session synchronization. Rely entirely on extracting JWT from headers/cookies to populate `TenantContext` and Security Context. |
| [LoginController.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/controller/LoginController.java) | [DELETE] Legacy UI route controller. | Completely retire. Routing moves to Next.js pages. |
| [SignUpController.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/controller/SignUpController.java) | [DELETE] Legacy UI route controller. | Completely retire. Signup endpoint moves to AuthController. |
| [HelloController.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/controller/HelloController.java) | [DELETE] Bypassed by Next.js views. | Retire UI logic. Extract dashboard statistic methods to a new `DashboardRestController`. |
| [MedicineController.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/controller/MedicineController.java) | Convert UI controllers to REST resources. | Change to `@RestController`, return JSON models / list response wrappers, and replace form binds with `@RequestBody`. |
| [BillingController.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/controller/BillingController.java) | Convert UI controllers to REST resources. | Change to `@RestController`, adjust PDF generation paths for token downloads, and return JSON responses. |
| [SalesAnalyticsController.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/controller/SalesAnalyticsController.java) | Convert UI controllers to REST resources. | Change to `@RestController` returning raw chart datasets, trends, and summary metrics. |
| [ExportController.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/controller/ExportController.java) | Convert to REST download resources. | Ensure endpoints are accessible via API authentication headers or download tokens. |
| [StaffController.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/controller/StaffController.java) | Convert UI controllers to REST resources. | Change to `@RestController` and return success/error JSON response payloads. |
| [PharmacyController.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/controller/PharmacyController.java) | Convert UI controllers to REST resources. | Change to `@RestController`, refactor logo upload/remove API methods, and allow CORS access for uploads folder. |
| [SuperAdminController.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/controller/SuperAdminController.java) | Convert UI controllers to REST resources. | Change to `@RestController` returning placeholder JSON objects. |

---

# 9 Risk Analysis

### Low Risk:
* **Catalog Data Structures**: Fetching, adding, editing, and deleting medicines maps 1:1 with existing database repository schemas.
* **Staff Controls**: Modifying employee statuses and authorization scopes involves basic text fields.
* **Super Admin Features**: The super admin actions are simple list queries.

### Medium Risk:
* **Security & Tokens Lifecycle**: Moving auth states to JWT tokens stored on frontend clients (e.g. Next.js local storage or cookies) introduces risks of token capture, cross-site scripting (XSS), token expirations, and missing security route guards.
* **Spreadsheet & Data Exports**: Browsers block file downloads when authorization headers are sent inside generic tags (e.g. `<a href="...">`). Requires implementing blob-fetch download managers in React.
* **Multi-Tenant Scoping**: The backend relies on `ThreadLocal` `TenantContext`. We must ensure the filter interceptor initializes this context correctly on *every* REST API call before hibernate entities are fetched.

### High Risk:
* **Binary File Streams (PDF Generation)**: Invoices generate raw PDF byte streams. Downloading or rendering these PDFs inline inside Next.js requires blob processing or temporary URL generator endpoints with short-lived tokens to bypass authorization header constraints.
* **Store Logo Image Uploads**: Handles multi-part file uploads across different server hosts. We must configure WebMvc static directories (`uploads/logos/**`) to enable CORS headers; otherwise, the Next.js client will experience origin blocks.
* **Concurrency in Billing transactions**: Submitting invoices deducts medicine inventories. Database locking (currently Pessimistic Write locks inside `MedicineRepository`) must be carefully verified to prevent race conditions during concurrent REST transactions.

---

# 10 Final TODO Checklist

### Phase 1: Authentication REST Migration (COMPLETED)
- [x] Add REST authentication endpoints alongside MVC in [AuthController.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/controller/AuthController.java)
- [x] Configure Spring Security in [SecurityConfig.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/config/SecurityConfig.java) to permit public access to REST auth endpoints
- [x] Implement `/api/auth/me` to retrieve stateless authentication context profile info
- [x] Add `/api/auth/logout` to clear cookie and security context
- [x] Map `/api/auth/register` alias to register new pharmacy tenant

### Phase 2: Core REST API Conversion
- [x] Expose stateless REST endpoints for settings in [PharmacyRestController.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/controller/api/PharmacyRestController.java) (COMPLETED)
- [x] Expose stateless REST endpoints for inventory in [MedicineRestController.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/controller/api/MedicineRestController.java) (COMPLETED)
- [x] Expose stateless REST endpoints for staff management in [StaffRestController.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/controller/api/StaffRestController.java) (COMPLETED)
- [x] Expose stateless REST endpoints for billing in [BillingRestController.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/controller/api/BillingRestController.java) (COMPLETED)
- [x] Implement PDF invoice download stream endpoint `/api/billing/{id}/pdf` (COMPLETED)

### Phase 3: Analytics & Exports
- [x] Create [DashboardRestController.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/controller/api/DashboardRestController.java) to serve statistics summaries (COMPLETED)
- [x] Expose stateless REST endpoints for analytics in [AnalyticsRestController.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/controller/api/AnalyticsRestController.java) (COMPLETED)
- [x] Expose stateless REST endpoints for exports and reports in [ReportsRestController.java](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/src/main/java/com/myspringboot/SpringBootApp/controller/api/ReportsRestController.java) (COMPLETED)
- [x] Delete legacy MVC controllers and SuperAdminController (COMPLETED)

### Phase 4: Clean up MVC and Thymeleaf (COMPLETED)
- [x] Remove `spring-boot-starter-thymeleaf` from [pom.xml](file:///C:/Users/Rezaul/.gemini/antigravity/worktrees/PharmaManagement/migrate-thymeleaf-to-nextjs/SpringBootApp/pom.xml) (COMPLETED)
- [x] Remove all legacy HTML templates from `src/main/resources/templates` (COMPLETED)
- [x] Remove legacy static CSS/JS files (COMPLETED)
