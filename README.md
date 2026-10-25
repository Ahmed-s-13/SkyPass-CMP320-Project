# SkyPass: Flight Booking Platform

CMP 320 Database Systems, Fall 2026 (American University of Sharjah).
Team: Ahmed Sherif, Chihab Ben Khadra, Tarik Mamluk.

## What's here

| Path | Contents |
|---|---|
| `DB-proj-stage1-report.docx` | Stage 1 report: ER model, relational mapping, keys and constraints (updated with the Stage 2 changes) |
| `DB-proj-ER.drawio`, `DB-proj-ER.png`, `SkyPass ER Diagram Draft.pdf` | ER diagram source and exports |
| `sky-pass code/sql/` | Oracle scripts 01–06: schema, triggers, sample data, DML demo, queries, views (`run_all.sql` runs them all) |
| `sky-pass code/SkyPass/` | Java Swing GUI (Maven project, opens in Apache NetBeans) |
| `docs/` | Stage 2 design spec, implementation plan and the manual QA checklist |

## Running it

See [`sky-pass code/README.md`](sky-pass%20code/README.md). In short:

1. Run `sky-pass code/sql/run_all.sql` in SQL Developer against your own Oracle account.
2. In `sky-pass code/SkyPass/src/main/resources/`, copy `db.properties.example` to `db.properties` and put in your Oracle username and password. `db.properties` is git-ignored, so it never gets pushed.
3. Open `sky-pass code/SkyPass` in NetBeans and run it (you must be on the AUS network or VPN).
