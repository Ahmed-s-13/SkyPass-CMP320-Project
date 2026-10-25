# SkyPass: Stage 2 code

Two parts: the Oracle scripts in `sql/` and the Java GUI in `SkyPass/`.

## 1. Build the database (SQL Developer)

Connect to your Oracle account on `coeoracle.aus.edu`, open each script and press **Run Script (F5)**, in this order:

| Script | What it does |
|---|---|
| `01_schema.sql` | Drops old SkyPass objects, creates the 13 tables, constraints and sequences, plus the ALTER TABLE and DROP demos |
| `02_triggers.sql` | Business-rule triggers (traveler owner, flight capacity, review only if flown, payment = fares, date of birth) |
| `03_data.sql` | Sample data (12 customers, 20 flights, 12 bookings, 26 tickets, …) |
| `04_dml.sql` | INSERT / UPDATE / DELETE demonstrations (run once; to repeat, start again from 01) |
| `05_queries.sql` | Q1–Q30: every query type the project asks for, each with its purpose |
| `06_views.sql` | `V_BOOKING_SUMMARY`, `V_AIRLINE_STATS` and queries on them (**the GUI needs these views**) |

Or open `run_all.sql` and press F5 to run all six in one go.

## 2. Run the GUI (Apache NetBeans)

1. **File → Open Project…** and pick the `SkyPass` folder. NetBeans recognises it as a Maven project and downloads the Oracle driver and FlatLaf on the first build.
2. In `src/main/resources/`, copy `db.properties.example` to `db.properties` and put in your Oracle username and password:
   ```properties
   db.user=your_username
   db.password=your_password
   ```
   `db.properties` is listed in `.gitignore`, so your password stays on your computer and is never pushed to GitHub.
3. Right-click the project → **Run**. The main class is `skypass.App`.

You need to be on the AUS network (or VPN). If the connection fails, the opening screen tells you why and offers **Retry**.

## Where things are

```
SkyPass/src/main/java/skypass/
  App.java              starts the app (splash, then main window)
  MainFrame.java        sidebar + pages
  db/Db.java            connection (reads your account from resources/db.properties)
  db/OracleErrors.java  turns ORA- errors into plain messages
  dao/                  all SQL used by the GUI (PreparedStatements)
  ui/pages/             Overview, Customers, Flights, Bookings, Reviews, Reports
  ui/anim/              plane animations (splash, sliding nav, route, departures board, boarding pass)
  ui/                   theme, form/table building blocks, validation, toasts
```

## What the GUI covers (rubric item 9)

- **Insert / update / delete:** customers (with phones and saved travelers), flights (with amenities), bookings (tickets and payment saved in one transaction), reviews.
- **Search / display:** a search box on every page, sortable tables.
- **Input validation:** checked before anything reaches Oracle; the bad field is outlined in red with the reason under it. Database rule violations (triggers, constraints) appear as plain-English messages.
- **Multi-table reports:** the Reports page (booking summary, airline performance, flight load factor), the Overview's departures board, and each booking's ticket list.
