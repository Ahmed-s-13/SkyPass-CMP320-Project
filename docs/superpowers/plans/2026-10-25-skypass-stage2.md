# SkyPass Stage 2 Implementation Plan

> Execution: Native (the user said "go ahead and build it"). Steps use checkbox syntax.

**Goal:** Runnable Oracle scripts plus a NetBeans/Maven Swing GUI for the SkyPass schema, exactly as the spec describes.

**Architecture:** Six ordered SQL scripts build and demo the schema. A Maven Swing app (FlatLaf) has one `Db` class holding the credential placeholders, DAOs that hold all SQL, and pages built from shared `Ui`/`Theme` helpers, with a small `Animator` driving every animation.

**Tech Stack:** Oracle SQL / PL/SQL (version-agnostic: sequences, no identity columns), Java 17 release target, Swing, FlatLaf 3.5.4 + Inter, ojdbc8 21.13.0.0, Maven (NetBeans).

**Spec:** `docs/superpowers/specs/2026-10-25-skypass-stage2-design.md`

## Global Constraints

- Credentials only in `skypass/db/Db.java` as `DBUSER = "username"` / `DBPASS = "password"`, matching the lab's `OracleJDBC.java`.
- `DBURL = "jdbc:oracle:thin:@coeoracle.aus.edu:1521:orcl"`.
- Column `ReviewComment` (not `Comment`); DOB rule by trigger; Self traveler created by the app; sequences start at 1000.
- Cabin multipliers ECONOMY 1.0, PREMIUM_ECONOMY 1.5, BUSINESS 2.5, FIRST 4.0; fare = ROUND(BaseFare × multiplier, 2).
- Look: flat light theme, colour on data + primary action only (international orange), neutral nav pill, no gradients/shadows on resting surfaces, no icon tiles, sentence-case labels.
- Animations 180–450 ms ease-out-cubic (hero loop excepted), never block input.

## Review Focus

1. Re-running `01_schema.sql` on a schema that already has the tables: it must drop and recreate cleanly, with no failure on the first run when nothing exists yet.
2. Saving a booking whose ticket violates a trigger (seat taken, capacity, wrong traveler): the whole transaction rolls back and the message is plain English.
3. Deleting a customer who has bookings: Oracle blocks it (RESTRICT), and the GUI says why instead of showing a raw ORA-02292.
4. Bad user input (empty name, `abc` as a date, arrival before departure, rating 0): caught before SQL, with the field highlighted.
5. Wrong or placeholder credentials: the splash shows the failure and a Retry button rather than crashing.

## File map

```
sky-pass code/
  sql/01_schema.sql 02_triggers.sql 03_data.sql 04_dml.sql 05_queries.sql 06_views.sql run_all.sql
  SkyPass/pom.xml
  SkyPass/src/main/java/skypass/
    App.java
    db/Db.java  db/OracleErrors.java
    dao/LookupDao.java CustomerDao.java FlightDao.java BookingDao.java ReviewDao.java ReportDao.java
    ui/Theme.java Ui.java Validation.java Toast.java FormCard.java DataTable.java CrudPage.java
    ui/anim/Animator.java Ease.java PlaneGlyph.java SplashWindow.java SidebarNav.java PageHost.java
            FlightPathHero.java CountUpLabel.java FlapBoard.java PlaneSpinner.java BoardingPassDialog.java Reveal.java
    ui/pages/OverviewPage.java CustomersPage.java FlightsPage.java BookingsPage.java ReviewsPage.java ReportsPage.java
    MainFrame.java
  README.md (how to run)
```

## Tasks

### Task 1: Schema script (`01_schema.sql`)
- [ ] PL/SQL block drops each table/sequence/view if present (ignores ORA-00942/02289).
- [ ] CREATE 13 tables in FK order with named constraints per Stage 1 §5.2 and Table 5 referential actions.
- [ ] Sequences CUSTOMER_SEQ, AIRLINE_SEQ, FLIGHT_SEQ, BOOKING_SEQ, PAYMENT_SEQ START WITH 1000.
- [ ] ALTER demo: add `FK_FLIGHT_AIRCRAFT (AirlineID, TailNo)` and `CK_TRAVELER_PASSPORT` via ALTER TABLE.
- [ ] DROP demo: CREATE `FLIGHT_IMPORT_TMP`, INSERT a row, SELECT, DROP TABLE … PURGE.

### Task 2: Triggers (`02_triggers.sql`)
- [ ] `TRG_CUSTOMER_DOB`, `TRG_TRAVELER_DOB` (−20001), `TRG_TICKET_TRAVELER_OWNER` (−20002), `TRG_TICKET_CAPACITY` statement-level AFTER INSERT OR UPDATE (−20003), `TRG_REVIEW_FLOWN` (−20004), `TRG_PAYMENT_AMOUNT` (−20005). End each with `/`.

### Task 3: Sample data (`03_data.sql`)
- [ ] Insert order: airports, airlines, serves, aircraft, flights, amenities, customers, phones, travelers, bookings, tickets, payments, reviews. COMMIT.
- [ ] Hand-check: every ticket's traveler owner = booking customer; payment = Σ fares; each review's customer has a ticket on that airline; seats unique per flight.

### Task 4: DML (`04_dml.sql`), Task 5: Queries (`05_queries.sql`), Task 6: Views (`06_views.sql`, `run_all.sql`)
- [ ] Each statement has a `-- Purpose:` line; queries numbered Q1… and mapped to the rubric bullet they satisfy.
- [ ] 06 creates both views, then queries each.

### Task 7: App core
- [ ] `pom.xml` (ojdbc8, flatlaf, flatlaf-fonts-inter, exec mainClass `skypass.App`, release 17).
- [ ] `Db` (placeholders, `get()`, `tx(Work)` helper with commit/rollback), `OracleErrors.friendly(SQLException)`.
- [ ] `Theme` tokens, `Ui` builders, `Validation`, `Toast`, `Animator`/`Ease`, `PlaneGlyph`.

### Task 8: DAOs
- [ ] Records for rows; every query via PreparedStatement; multi-table writes through `Db.tx`.

### Task 9: Shell + pages
- [ ] `SplashWindow` → `MainFrame` (`SidebarNav` + `PageHost` with fade/rise transition).
- [ ] Overview, Customers, Flights, Bookings (+ `BoardingPassDialog`), Reviews, Reports.

### Task 10: Word report
- [ ] Back up the docx, then apply C1–C4 to the relational schema, constraint tables, §5.3 list and mapping notes.

### Task 11: Verify
- [ ] `mvn -q compile` with NetBeans' bundled Maven.
- [ ] Headless render of splash, shell and each page to PNG; look and fix (2 rounds max).
- [ ] Re-read the SQL against the Review Focus list and the rubric checklist.
