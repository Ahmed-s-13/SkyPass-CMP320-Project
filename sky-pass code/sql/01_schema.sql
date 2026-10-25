-- =====================================================================
-- SkyPass: Flight Booking Platform
-- CMP 320 Database Systems, Fall 2026, Stage 2
-- 01_schema.sql : DDL for the complete schema
--
-- Run in SQL Developer with "Run Script" (F5), connected as your own
-- Oracle account. The script can be re-run: it drops the old objects
-- first. Run the scripts in order: 01 -> 02 -> 03 -> 04 -> 05 -> 06.
-- =====================================================================
SET DEFINE OFF

-- ---------------------------------------------------------------------
-- 0. Clean up any objects left from a previous run.
--    Tables are dropped with CASCADE CONSTRAINTS so the order does not
--    matter; triggers are dropped together with their tables.
-- ---------------------------------------------------------------------
BEGIN
    FOR t IN (SELECT table_name FROM user_tables
              WHERE table_name IN ('TICKET', 'REVIEW', 'PAYMENT', 'BOOKING',
                                   'FLIGHT_AMENITY', 'FLIGHT', 'AIRCRAFT',
                                   'SERVES', 'AIRPORT', 'AIRLINE', 'TRAVELER',
                                   'CUSTOMER_PHONE', 'CUSTOMER',
                                   'FLIGHT_IMPORT_TMP')) LOOP
        EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS PURGE';
    END LOOP;
    FOR s IN (SELECT sequence_name FROM user_sequences
              WHERE sequence_name IN ('CUSTOMER_SEQ', 'AIRLINE_SEQ', 'FLIGHT_SEQ',
                                      'BOOKING_SEQ', 'PAYMENT_SEQ')) LOOP
        EXECUTE IMMEDIATE 'DROP SEQUENCE ' || s.sequence_name;
    END LOOP;
    FOR v IN (SELECT view_name FROM user_views
              WHERE view_name IN ('V_BOOKING_SUMMARY', 'V_AIRLINE_STATS')) LOOP
        EXECUTE IMMEDIATE 'DROP VIEW ' || v.view_name;
    END LOOP;
END;
/

-- ---------------------------------------------------------------------
-- 1. CUSTOMER (strong). Composite Name and Address are flattened.
--    Age is derived (not stored). "DateOfBirth <= today" cannot be a
--    CHECK in Oracle (SYSDATE is not allowed), so it is a trigger in 02.
-- ---------------------------------------------------------------------
CREATE TABLE CUSTOMER (
    CustomerID   NUMBER(6)      CONSTRAINT PK_CUSTOMER PRIMARY KEY,
    FirstName    VARCHAR2(40)   NOT NULL,
    LastName     VARCHAR2(40)   NOT NULL,
    Email        VARCHAR2(100)  NOT NULL,
    DateOfBirth  DATE           NOT NULL,
    Street       VARCHAR2(100)  NOT NULL,
    City         VARCHAR2(50)   NOT NULL,
    Country      VARCHAR2(50)   NOT NULL,
    CONSTRAINT UQ_CUSTOMER_EMAIL UNIQUE (Email),
    CONSTRAINT CK_CUSTOMER_EMAIL CHECK (Email LIKE '_%@_%._%')
);

-- Multi-valued attribute Customer.Phone
CREATE TABLE CUSTOMER_PHONE (
    CustomerID   NUMBER(6)      NOT NULL,
    Phone        VARCHAR2(20)   NOT NULL,
    CONSTRAINT PK_CUSTOMER_PHONE PRIMARY KEY (CustomerID, Phone),
    CONSTRAINT FK_PHONE_CUSTOMER FOREIGN KEY (CustomerID)
        REFERENCES CUSTOMER (CustomerID) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- 2. TRAVELER (weak entity, owner CUSTOMER, partial key TravelerNo).
-- ---------------------------------------------------------------------
CREATE TABLE TRAVELER (
    CustomerID   NUMBER(6)      NOT NULL,
    TravelerNo   NUMBER(3)      NOT NULL,
    FullName     VARCHAR2(80)   NOT NULL,
    DateOfBirth  DATE           NOT NULL,
    Relationship VARCHAR2(10)   NOT NULL,
    PassportNo   VARCHAR2(15)   NOT NULL,
    CONSTRAINT PK_TRAVELER PRIMARY KEY (CustomerID, TravelerNo),
    CONSTRAINT FK_TRAVELER_CUSTOMER FOREIGN KEY (CustomerID)
        REFERENCES CUSTOMER (CustomerID) ON DELETE CASCADE,
    CONSTRAINT UQ_TRAVELER_PASSPORT UNIQUE (CustomerID, PassportNo),
    CONSTRAINT CK_TRAVELER_NO CHECK (TravelerNo > 0),
    CONSTRAINT CK_TRAVELER_REL CHECK (Relationship IN
        ('Self', 'Spouse', 'Child', 'Parent', 'Sibling', 'Friend', 'Other'))
);

-- ---------------------------------------------------------------------
-- 3. AIRLINE, AIRPORT and the M:N relationship SERVES.
-- ---------------------------------------------------------------------
CREATE TABLE AIRLINE (
    AirlineID    NUMBER(4)      CONSTRAINT PK_AIRLINE PRIMARY KEY,
    IATACode     CHAR(2)        NOT NULL,
    Name         VARCHAR2(60)   NOT NULL,
    CONSTRAINT UQ_AIRLINE_IATA UNIQUE (IATACode),
    CONSTRAINT CK_AIRLINE_IATA CHECK (REGEXP_LIKE(IATACode, '^[A-Z0-9]{2}$'))
);

-- Composite attribute Location is flattened into City, Country.
CREATE TABLE AIRPORT (
    AirportCode  CHAR(3)        CONSTRAINT PK_AIRPORT PRIMARY KEY,
    Name         VARCHAR2(80)   NOT NULL,
    City         VARCHAR2(50)   NOT NULL,
    Country      VARCHAR2(50)   NOT NULL,
    CONSTRAINT CK_AIRPORT_CODE CHECK (REGEXP_LIKE(AirportCode, '^[A-Z]{3}$'))
);

CREATE TABLE SERVES (
    AirlineID    NUMBER(4)      NOT NULL,
    AirportCode  CHAR(3)        NOT NULL,
    CONSTRAINT PK_SERVES PRIMARY KEY (AirlineID, AirportCode),
    CONSTRAINT FK_SERVES_AIRLINE FOREIGN KEY (AirlineID)
        REFERENCES AIRLINE (AirlineID) ON DELETE CASCADE,
    CONSTRAINT FK_SERVES_AIRPORT FOREIGN KEY (AirportCode)
        REFERENCES AIRPORT (AirportCode) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- 4. AIRCRAFT (1:N Owns from AIRLINE).
--    UNIQUE (AirlineID, TailNo) is the target of the composite FK from
--    FLIGHT, which guarantees a flight's aircraft belongs to its airline.
-- ---------------------------------------------------------------------
CREATE TABLE AIRCRAFT (
    TailNo       VARCHAR2(10)   CONSTRAINT PK_AIRCRAFT PRIMARY KEY,
    AirlineID    NUMBER(4)      NOT NULL,
    Model        VARCHAR2(40)   NOT NULL,
    Capacity     NUMBER(3)      NOT NULL,
    CONSTRAINT FK_AIRCRAFT_AIRLINE FOREIGN KEY (AirlineID)
        REFERENCES AIRLINE (AirlineID),
    CONSTRAINT UQ_AIRCRAFT_AIRLINE UNIQUE (AirlineID, TailNo),
    CONSTRAINT CK_AIRCRAFT_CAPACITY CHECK (Capacity > 0)
);

-- ---------------------------------------------------------------------
-- 5. FLIGHT. Duration is derived (ArrTime - DepTime); times are stored
--    in UAE time (GST) so the subtraction gives the real duration.
--    The composite FK to AIRCRAFT is added below with ALTER TABLE.
-- ---------------------------------------------------------------------
CREATE TABLE FLIGHT (
    FlightID     NUMBER(6)      CONSTRAINT PK_FLIGHT PRIMARY KEY,
    FlightNo     VARCHAR2(7)    NOT NULL,
    AirlineID    NUMBER(4)      NOT NULL,
    TailNo       VARCHAR2(10)   NOT NULL,
    OriginCode   CHAR(3)        NOT NULL,
    DestCode     CHAR(3)        NOT NULL,
    DepTime      DATE           NOT NULL,
    ArrTime      DATE           NOT NULL,
    BaseFare     NUMBER(8,2)    NOT NULL,
    Status       VARCHAR2(10)   DEFAULT 'SCHEDULED' NOT NULL,
    CONSTRAINT FK_FLIGHT_AIRLINE FOREIGN KEY (AirlineID)
        REFERENCES AIRLINE (AirlineID),
    CONSTRAINT FK_FLIGHT_ORIGIN FOREIGN KEY (OriginCode)
        REFERENCES AIRPORT (AirportCode),
    CONSTRAINT FK_FLIGHT_DEST FOREIGN KEY (DestCode)
        REFERENCES AIRPORT (AirportCode),
    CONSTRAINT UQ_FLIGHT_NO_DEP UNIQUE (FlightNo, DepTime),
    CONSTRAINT CK_FLIGHT_ROUTE CHECK (OriginCode <> DestCode),
    CONSTRAINT CK_FLIGHT_TIMES CHECK (ArrTime > DepTime),
    CONSTRAINT CK_FLIGHT_FARE CHECK (BaseFare > 0),
    CONSTRAINT CK_FLIGHT_STATUS CHECK (Status IN
        ('SCHEDULED', 'DELAYED', 'DEPARTED', 'ARRIVED', 'CANCELLED'))
);

-- Multi-valued attribute Flight.Amenities
CREATE TABLE FLIGHT_AMENITY (
    FlightID     NUMBER(6)      NOT NULL,
    Amenity      VARCHAR2(30)   NOT NULL,
    CONSTRAINT PK_FLIGHT_AMENITY PRIMARY KEY (FlightID, Amenity),
    CONSTRAINT FK_AMENITY_FLIGHT FOREIGN KEY (FlightID)
        REFERENCES FLIGHT (FlightID) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- 6. BOOKING (1:N Makes) and PAYMENT (1:1 PaidBy).
--    TotalPrice is derived (sum of ticket fares), not stored.
-- ---------------------------------------------------------------------
CREATE TABLE BOOKING (
    BookingRef   VARCHAR2(8)    CONSTRAINT PK_BOOKING PRIMARY KEY,
    CustomerID   NUMBER(6)      NOT NULL,
    BookingDate  DATE           DEFAULT SYSDATE NOT NULL,
    Status       VARCHAR2(10)   DEFAULT 'PENDING' NOT NULL,
    CONSTRAINT FK_BOOKING_CUSTOMER FOREIGN KEY (CustomerID)
        REFERENCES CUSTOMER (CustomerID),
    CONSTRAINT CK_BOOKING_STATUS CHECK (Status IN ('PENDING', 'CONFIRMED', 'CANCELLED'))
);

-- The 1:1 is mapped by putting BookingRef in PAYMENT as NOT NULL + UNIQUE.
CREATE TABLE PAYMENT (
    PaymentID    NUMBER(8)      CONSTRAINT PK_PAYMENT PRIMARY KEY,
    BookingRef   VARCHAR2(8)    NOT NULL,
    Method       VARCHAR2(15)   NOT NULL,
    Amount       NUMBER(10,2)   NOT NULL,
    PaidAt       TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT FK_PAYMENT_BOOKING FOREIGN KEY (BookingRef)
        REFERENCES BOOKING (BookingRef),
    CONSTRAINT UQ_PAYMENT_BOOKING UNIQUE (BookingRef),
    CONSTRAINT CK_PAYMENT_METHOD CHECK (Method IN ('CARD', 'WALLET', 'BANK_TRANSFER')),
    CONSTRAINT CK_PAYMENT_AMOUNT CHECK (Amount > 0)
);

-- ---------------------------------------------------------------------
-- 7. REVIEW (M:N Reviews between CUSTOMER and AIRLINE).
--    Stage 1 called the comment column "Comment"; COMMENT is a reserved
--    word in Oracle, so the column is named ReviewComment.
-- ---------------------------------------------------------------------
CREATE TABLE REVIEW (
    CustomerID    NUMBER(6)      NOT NULL,
    AirlineID     NUMBER(4)      NOT NULL,
    Rating        NUMBER(1)      NOT NULL,
    ReviewComment VARCHAR2(500),
    ReviewDate    DATE           DEFAULT SYSDATE NOT NULL,
    CONSTRAINT PK_REVIEW PRIMARY KEY (CustomerID, AirlineID),
    CONSTRAINT FK_REVIEW_CUSTOMER FOREIGN KEY (CustomerID)
        REFERENCES CUSTOMER (CustomerID) ON DELETE CASCADE,
    CONSTRAINT FK_REVIEW_AIRLINE FOREIGN KEY (AirlineID)
        REFERENCES AIRLINE (AirlineID),
    CONSTRAINT CK_REVIEW_RATING CHECK (Rating BETWEEN 1 AND 5)
);

-- ---------------------------------------------------------------------
-- 8. TICKET: ternary relationship BOOKING - TRAVELER - FLIGHT.
--    PK = keys of all three participants (traveler key is composite).
-- ---------------------------------------------------------------------
CREATE TABLE TICKET (
    BookingRef   VARCHAR2(8)    NOT NULL,
    CustomerID   NUMBER(6)      NOT NULL,
    TravelerNo   NUMBER(3)      NOT NULL,
    FlightID     NUMBER(6)      NOT NULL,
    SeatNo       VARCHAR2(4)    NOT NULL,
    CabinClass   VARCHAR2(15)   DEFAULT 'ECONOMY' NOT NULL,
    Fare         NUMBER(8,2)    NOT NULL,
    CONSTRAINT PK_TICKET PRIMARY KEY (BookingRef, CustomerID, TravelerNo, FlightID),
    CONSTRAINT FK_TICKET_BOOKING FOREIGN KEY (BookingRef)
        REFERENCES BOOKING (BookingRef) ON DELETE CASCADE,
    CONSTRAINT FK_TICKET_TRAVELER FOREIGN KEY (CustomerID, TravelerNo)
        REFERENCES TRAVELER (CustomerID, TravelerNo),
    CONSTRAINT FK_TICKET_FLIGHT FOREIGN KEY (FlightID)
        REFERENCES FLIGHT (FlightID),
    CONSTRAINT UQ_TICKET_SEAT UNIQUE (FlightID, SeatNo),
    CONSTRAINT UQ_TICKET_TRAVELER UNIQUE (FlightID, CustomerID, TravelerNo),
    CONSTRAINT CK_TICKET_SEAT CHECK (REGEXP_LIKE(SeatNo, '^[1-9][0-9]?[A-K]$')),
    CONSTRAINT CK_TICKET_CABIN CHECK (CabinClass IN
        ('ECONOMY', 'PREMIUM_ECONOMY', 'BUSINESS', 'FIRST')),
    CONSTRAINT CK_TICKET_FARE CHECK (Fare >= 0)
);

-- ---------------------------------------------------------------------
-- 9. Sequences for new surrogate keys created from the GUI.
--    Sample data (03) uses small fixed IDs, so sequences start at 1000.
--    New booking references are 'SP' || BOOKING_SEQ.NEXTVAL (e.g. SP1000).
-- ---------------------------------------------------------------------
CREATE SEQUENCE CUSTOMER_SEQ START WITH 1000 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE AIRLINE_SEQ  START WITH 1000 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE FLIGHT_SEQ   START WITH 1000 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE BOOKING_SEQ  START WITH 1000 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE PAYMENT_SEQ  START WITH 1000 INCREMENT BY 1 NOCACHE;

-- ---------------------------------------------------------------------
-- 10. ALTER TABLE demonstration.
--  a) Composite FK: a flight's aircraft must belong to the airline that
--     operates the flight (BR8). Added after both tables exist.
--  b) Passport numbers must be 6-15 capital letters or digits.
-- ---------------------------------------------------------------------
ALTER TABLE FLIGHT ADD CONSTRAINT FK_FLIGHT_AIRCRAFT
    FOREIGN KEY (AirlineID, TailNo) REFERENCES AIRCRAFT (AirlineID, TailNo);

ALTER TABLE TRAVELER ADD CONSTRAINT CK_TRAVELER_PASSPORT
    CHECK (REGEXP_LIKE(PassportNo, '^[A-Z0-9]{6,15}$'));

-- ---------------------------------------------------------------------
-- 11. DROP demonstration on a temporary object.
--     A staging table used to check a partner airline's schedule file
--     before it is loaded; it is no longer needed afterwards.
-- ---------------------------------------------------------------------
CREATE TABLE FLIGHT_IMPORT_TMP (
    FlightNo     VARCHAR2(7),
    OriginCode   CHAR(3),
    DestCode     CHAR(3),
    BaseFare     NUMBER(8,2)
);

INSERT INTO FLIGHT_IMPORT_TMP VALUES ('EK001', 'DXB', 'LHR', 2150);
SELECT * FROM FLIGHT_IMPORT_TMP;

DROP TABLE FLIGHT_IMPORT_TMP PURGE;

-- Check: list the SkyPass tables and their constraint counts.
SELECT table_name, COUNT(*) AS constraint_count
FROM user_constraints
WHERE table_name IN ('CUSTOMER', 'CUSTOMER_PHONE', 'TRAVELER', 'AIRLINE', 'AIRPORT',
                     'SERVES', 'AIRCRAFT', 'FLIGHT', 'FLIGHT_AMENITY', 'BOOKING',
                     'PAYMENT', 'REVIEW', 'TICKET')
GROUP BY table_name
ORDER BY table_name;
