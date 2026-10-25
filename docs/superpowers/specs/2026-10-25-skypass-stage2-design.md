# SkyPass — Stage 2 Design (Database Implementation + GUI)

Date: 2026-10-25
Source of truth: `DB-proj-stage1-report.docx` (ER model, relational mapping, keys, constraints) and `Project Description.pdf` (Stage 2 rubric).

## 1. Goal

Deliver everything Stage 2 needs to run: Oracle SQL scripts (run by the team in SQL Developer on `coeoracle.aus.edu`) and a Java Swing GUI (opened in Apache NetBeans) that connects to that schema. Scope is exactly the rubric, with no extra features beyond what is listed here.

## 2. Changes to the Stage 1 design (applied to the Word report)

| # | Stage 1 | Stage 2 | Reason |
|---|---|---|---|
| C1 | `REVIEW.Comment` | `REVIEW.ReviewComment` | `COMMENT` is a reserved word in Oracle |
| C2 | CHECK DateOfBirth ≤ current date (CUSTOMER) | Enforced by trigger on CUSTOMER and TRAVELER | Oracle CHECK constraints cannot reference `SYSDATE` |
| C3 | 'Self' traveler created automatically (listed with trigger rules) | Created by the application in the same transaction as the customer; the GUI asks for the customer's passport number | TRAVELER.PassportNo is NOT NULL and CUSTOMER has no passport column, so a trigger cannot supply it |
| C4 | Surrogate keys (no generation strategy stated) | Oracle sequences (`CUSTOMER_SEQ`, `AIRLINE_SEQ`, `FLIGHT_SEQ`, `BOOKING_SEQ`, `PAYMENT_SEQ`), starting at 1000; BookingRef = `'SP' ‖ sequence value` | Works on every Oracle version; sample data keeps small fixed IDs |

All other tables, keys, FKs, referential actions and CHECK/DEFAULT/UNIQUE constraints are implemented exactly as Stage 1 §4–§5 state.

## 3. SQL deliverables — `sky-pass code/sql/`

Run in order. Each script can be re-run; `01` drops existing objects first.

| File | Contents |
|---|---|
| `01_schema.sql` | DROP of old objects; CREATE of 13 tables with all constraints, named (`PK_`, `FK_`, `UQ_`, `CK_`); sequences; **ALTER TABLE** demo (composite FK `FLIGHT(AirlineID, TailNo) → AIRCRAFT(AirlineID, TailNo)` and a CHECK added via ALTER); **DROP** demo (temporary `FLIGHT_IMPORT_TMP` staging table created, used, then dropped) |
| `02_triggers.sql` | `TRG_TICKET_TRAVELER_OWNER` (BR5), `TRG_TICKET_CAPACITY` statement-level (BR8, avoids mutating-table error), `TRG_REVIEW_FLOWN` (BR11), `TRG_PAYMENT_AMOUNT` (BR6), `TRG_CUSTOMER_DOB` / `TRG_TRAVELER_DOB` (C2). Errors raised with `RAISE_APPLICATION_ERROR(-200xx, '<plain message>')` |
| `03_data.sql` | 12 customers (+ phones), 20 travelers (12 Self + 8 companions), 10 airlines, 12 aircraft, 12 airports, 30+ serves rows, 15 flights (+ amenities), 12 bookings, 12 payments, ~26 tickets, 10 reviews. All rows satisfy the triggers (reviews only for flown airlines; payment = sum of fares; fares = base fare × cabin multiplier) |
| `04_dml.sql` | Meaningful INSERT (new booking with tickets + payment), UPDATE (delay a flight, cancel a booking, change a rating), DELETE (remove a companion, delete a review, cascade on phones) |
| `05_queries.sql` | Every construct in rubric §7, numbered, each with a `-- Purpose:` line |
| `06_views.sql` | `V_BOOKING_SUMMARY` (booking + customer + derived TotalPrice + ticket count + payment) and `V_AIRLINE_STATS` (derived AvgRating, review count, tickets sold, revenue), plus queries against both |
| `run_all.sql` | `@@` runner calling 01→06 for convenience |

Conventions: times are stored as Oracle `DATE` in UAE time (GST), so `Duration = (ArrTime − DepTime) × 1440` minutes. Cabin multipliers: ECONOMY 1.0, PREMIUM_ECONOMY 1.5, BUSINESS 2.5, FIRST 4.0. Derived attributes are never stored; they live in views and queries.

## 4. Java GUI — `sky-pass code/SkyPass/`

Maven project (same shape as the course's `OracleJDBC` lab). Dependencies: `ojdbc8 21.13.0.0`, `flatlaf`, `flatlaf-fonts-inter` / `flatlaf-fonts-jetbrains-mono` (bundled fonts, so it looks the same on every machine). `maven.compiler.release` 17.

### 4.1 Connection

`skypass/db/Db.java` holds, like the lab file:

```java
public static final String DBURL  = "jdbc:oracle:thin:@coeoracle.aus.edu:1521:orcl";
public static final String DBUSER = "username";   // <-- put your Oracle username here
public static final String DBPASS = "password";   // <-- put your Oracle password here
```

One shared connection; every statement is a `PreparedStatement`; multi-row writes (customer + Self traveler + phones; booking + tickets + payment) run in a single transaction with commit/rollback.

### 4.2 Packages

- `skypass.App`: entry point; sets FlatLaf, shows splash, opens `MainFrame`.
- `skypass.db`: `Db`, `OracleErrors` (maps ORA-00001/02290/02291/02292/200xx to plain English).
- `skypass.dao`: `CustomerDao`, `FlightDao`, `BookingDao`, `ReviewDao`, `ReportDao`, `LookupDao` (dropdown data). SQL lives only here.
- `skypass.ui`: `Theme` (colors, fonts, spacing tokens), `Ui` (form/field/button builders), `Validation`, `Toast`.
- `skypass.ui.anim`: `Animator` (Swing-Timer tween with easing), `SplashWindow`, `FlightPathHero`, `PageTransition`, `SidebarNav`, `CountUpLabel`, `FlapBoard`, `PlaneSpinner`, `BoardingPassDialog`.
- `skypass.ui.pages`: `OverviewPage`, `CustomersPage`, `FlightsPage`, `BookingsPage`, `ReviewsPage`, `ReportsPage`.

### 4.3 Screens

Layout: fixed left sidebar (logo + 6 nav items), content area. CRUD pages use one pattern: header with title + search box, a table on the left, a form card on the right, and **New / Save / Delete** buttons. Selecting a row loads the form (update mode); New clears it (insert mode).

- **Overview**: flight-path hero, 4 count-up stat cards (customers, flights, bookings, revenue), and a "Next departures" split-flap board (next 6 flights from the DB).
- **Customers**: CRUD on CUSTOMER + CUSTOMER_PHONE (comma-separated phones). Insert also asks for a passport number and creates the Self traveler. Below the form, a companions list for the selected customer with Add / Remove (weak entity TRAVELER). Search by name/email/city. Shows derived Age.
- **Flights**: CRUD on FLIGHT + FLIGHT_AMENITY. The aircraft dropdown is filtered by the selected airline; origin/destination dropdowns; amenities as checkboxes; status dropdown. Table shows derived duration. Search by flight no / airport code.
- **Bookings**: list from `V_BOOKING_SUMMARY`; selecting one shows its tickets. "New booking": choose customer → add ticket lines (traveler of that customer, flight, cabin, seat; fare auto-computed) → payment method → Save (one transaction) → boarding-pass confirmation. Update = change status. Delete removes payment + booking (tickets cascade).
- **Reviews**: CRUD on REVIEW (customer, airline, 1–5 rating, comment, date). Trigger message shown if the customer never flew that airline.
- **Reports**: 3 multi-table reports picked from a segmented control: Booking summary (view 1), Airline performance (view 2), Flight load factor (FLIGHT ⋈ AIRCRAFT ⋈ AIRLINE ⋈ TICKET: sold / capacity %).

### 4.4 Validation and errors

Before any SQL runs: required fields, email format, date formats (`yyyy-MM-dd`, `yyyy-MM-dd HH:mm`), DOB not in future, numbers > 0, arrival after departure, origin ≠ destination, rating 1–5, seat format (e.g. `12A`), at least one ticket per booking. Invalid fields get a red outline plus a message under the field. DB errors go through `OracleErrors` and appear as an error toast.

### 4.5 Animations (plane theme, subtle and fast)

All animations use one `Animator` (60 fps `javax.swing.Timer`, ease-out-cubic), last 180–450 ms (except the slow hero loop), and never block input.

1. **Splash**: a plane traces a dashed great-circle arc across a dusk-blue sky, the SkyPass wordmark fades in, and the status reads "Connecting to Oracle…". On failure it shows the error and a Retry button, with a hint to check `Db.java`.
2. **Sidebar**: the active-item pill slides to the clicked item; a small plane glyph rides along with it.
3. **Page transitions**: the new page fades in and rises 12 px (snapshot-based, then the live panel is swapped in).
4. **Overview hero**: a plane loops along a curved route between two airport dots with a fading contrail. Stat numbers count up when the page appears. Departure board rows flip in split-flap style, staggered.
5. **Scroll reveal**: on Overview and Reports, cards fade and slide in as they scroll into view.
6. **Toasts**: they slide in from the top-right with a small plane icon and auto-dismiss.
7. **Loading**: a plane circles a dot while a DB call runs (SwingWorker keeps the UI responsive).
8. **Booking confirmation**: a boarding-pass card rises in, showing the booking ref, travelers, flights and total.

## 5. Out of scope

Login screen, editing tickets on an existing booking, CRUD for Airline/Aircraft/Airport/Serves (seeded by SQL only), real payments, reports export.

## 6. Verification

- I compile the Maven project with NetBeans' bundled Maven to confirm it builds, and render pages headlessly to check the look where feasible.
- The team runs `01`→`06` in SQL Developer on `coeoracle`, sets `DBUSER`/`DBPASS` in `Db.java`, and runs the app from NetBeans.
- No local Oracle container (per user).
