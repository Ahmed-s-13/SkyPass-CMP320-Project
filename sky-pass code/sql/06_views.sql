-- =====================================================================
-- SkyPass - 06_views.sql
-- Two business views, plus queries that use them. The GUI's Bookings
-- page and Reports page read from these views.
-- =====================================================================
SET DEFINE OFF

-- ---------------------------------------------------------------------
-- V1. V_BOOKING_SUMMARY
-- One row per booking: who made it, how many tickets it has, its
-- derived TotalPrice (sum of ticket fares) and how it was paid.
-- Combines BOOKING, CUSTOMER, TICKET and PAYMENT.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW V_BOOKING_SUMMARY AS
SELECT b.BookingRef,
       b.BookingDate,
       b.Status,
       c.CustomerID,
       c.FirstName || ' ' || c.LastName  AS CustomerName,
       c.Email,
       COUNT(t.FlightID)                 AS TicketCount,
       NVL(SUM(t.Fare), 0)               AS TotalPrice,
       p.Method                          AS PaymentMethod,
       p.Amount                          AS AmountPaid,
       p.PaidAt
FROM BOOKING b
JOIN CUSTOMER c     ON c.CustomerID = b.CustomerID
LEFT JOIN TICKET t  ON t.BookingRef = b.BookingRef
LEFT JOIN PAYMENT p ON p.BookingRef = b.BookingRef
GROUP BY b.BookingRef, b.BookingDate, b.Status, c.CustomerID, c.FirstName,
         c.LastName, c.Email, p.Method, p.Amount, p.PaidAt;

-- ---------------------------------------------------------------------
-- V2. V_AIRLINE_STATS
-- One row per airline: fleet size, flights, tickets sold and revenue
-- (cancelled bookings excluded) and the derived AvgRating.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW V_AIRLINE_STATS AS
SELECT a.AirlineID,
       a.IATACode,
       a.Name,
       (SELECT COUNT(*) FROM AIRCRAFT ac WHERE ac.AirlineID = a.AirlineID)   AS FleetSize,
       (SELECT COUNT(*) FROM FLIGHT f WHERE f.AirlineID = a.AirlineID)       AS Flights,
       (SELECT COUNT(*)
          FROM TICKET t
          JOIN FLIGHT f  ON f.FlightID = t.FlightID
          JOIN BOOKING b ON b.BookingRef = t.BookingRef
         WHERE f.AirlineID = a.AirlineID
           AND b.Status <> 'CANCELLED')                                        AS TicketsSold,
       (SELECT NVL(SUM(t.Fare), 0)
          FROM TICKET t
          JOIN FLIGHT f  ON f.FlightID = t.FlightID
          JOIN BOOKING b ON b.BookingRef = t.BookingRef
         WHERE f.AirlineID = a.AirlineID
           AND b.Status <> 'CANCELLED')                                        AS Revenue,
       (SELECT ROUND(AVG(r.Rating), 2) FROM REVIEW r WHERE r.AirlineID = a.AirlineID) AS AvgRating,
       (SELECT COUNT(*) FROM REVIEW r WHERE r.AirlineID = a.AirlineID)       AS ReviewCount
FROM AIRLINE a;

-- =====================================================================
-- Queries against the views
-- =====================================================================

-- QV1. Purpose: the customers who have spent the most on confirmed
-- bookings (uses the derived TotalPrice from V_BOOKING_SUMMARY).
SELECT CustomerName, COUNT(*) AS Bookings, SUM(TotalPrice) AS TotalSpent
FROM V_BOOKING_SUMMARY
WHERE Status = 'CONFIRMED'
GROUP BY CustomerName
ORDER BY TotalSpent DESC;

-- QV2. Purpose: payment audit. Any booking whose recorded payment does
-- not match its ticket total, or that has no payment, needs attention.
-- (Expected result: no rows.)
SELECT BookingRef, CustomerName, TotalPrice, AmountPaid
FROM V_BOOKING_SUMMARY
WHERE AmountPaid IS NULL
   OR AmountPaid <> TotalPrice;

-- QV3. Purpose: airline performance league table: well-rated airlines
-- (average 4 or more) ordered by the revenue they bring in.
SELECT IATACode, Name, AvgRating, ReviewCount, TicketsSold, Revenue
FROM V_AIRLINE_STATS
WHERE AvgRating >= 4
ORDER BY Revenue DESC;

-- QV4. Purpose: airlines on the platform that have not sold a ticket
-- yet, for the partnerships team to follow up.
SELECT IATACode, Name, FleetSize, Flights
FROM V_AIRLINE_STATS
WHERE TicketsSold = 0
ORDER BY Name;
