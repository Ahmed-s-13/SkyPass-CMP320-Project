-- =====================================================================
-- SkyPass - 05_queries.sql
-- SELECT queries required by the project (section 7 of the brief).
-- Each query states its purpose and the requirement it demonstrates.
-- Run after 04_dml.sql. Money is in AED.
-- =====================================================================
SET DEFINE OFF

-- =====================================================================
-- A. FILTERING, EXPRESSIONS AND ORDERING
-- =====================================================================

-- Q1  [WHERE with comparison + logical operators, ORDER BY]
-- Purpose: upcoming departures from Dubai that cost more than AED 2,000,
-- so a customer looking for long-haul options sees them in date order.
SELECT FlightNo, OriginCode, DestCode, DepTime, BaseFare, Status
FROM FLIGHT
WHERE OriginCode = 'DXB'
  AND BaseFare > 2000
  AND (Status = 'SCHEDULED' OR Status = 'DELAYED')
ORDER BY DepTime;

-- Q2  [LIKE]
-- Purpose: find customers using a Gmail address, e.g. for an email
-- deliverability check, sorted by last name.
SELECT CustomerID, FirstName, LastName, Email
FROM CUSTOMER
WHERE Email LIKE '%@gmail.com'
ORDER BY LastName, FirstName;

-- Q3  [IN]
-- Purpose: list the airports in the Gulf region served by the platform.
SELECT AirportCode, Name, City, Country
FROM AIRPORT
WHERE Country IN ('UAE', 'Qatar')
ORDER BY Country, City;

-- Q4  [BETWEEN]
-- Purpose: show all flights departing in November 2026, for the
-- monthly operations schedule.
SELECT FlightNo, OriginCode || ' -> ' || DestCode AS Route, DepTime, Status
FROM FLIGHT
WHERE DepTime BETWEEN TO_DATE('2026-11-01', 'YYYY-MM-DD')
                  AND TO_DATE('2026-11-30 23:59', 'YYYY-MM-DD HH24:MI')
ORDER BY DepTime;

-- Q5a [IS NULL]
-- Purpose: reviews left as a rating only, with no written comment,
-- so the support team can ask those customers for more detail.
SELECT CustomerID, AirlineID, Rating, ReviewDate
FROM REVIEW
WHERE ReviewComment IS NULL
ORDER BY ReviewDate DESC;

-- Q5b [IS NOT NULL]
-- Purpose: written reviews, best rated first, for the airline pages.
SELECT CustomerID, AirlineID, Rating, ReviewComment
FROM REVIEW
WHERE ReviewComment IS NOT NULL
ORDER BY Rating DESC, ReviewDate DESC;

-- Q6  [calculated expressions + column aliases; derived attributes]
-- Purpose: each flight's derived duration and what a seat costs in each
-- cabin (fare = base fare x cabin multiplier).
SELECT FlightNo,
       OriginCode || ' -> ' || DestCode                          AS Route,
       ROUND((ArrTime - DepTime) * 1440)                          AS DurationMinutes,
       TRUNC(ROUND((ArrTime - DepTime) * 1440) / 60) || 'h '
         || MOD(ROUND((ArrTime - DepTime) * 1440), 60) || 'm'     AS Duration,
       BaseFare                                                   AS EconomyFare,
       BaseFare * 1.5                                             AS PremiumEconomyFare,
       BaseFare * 2.5                                             AS BusinessFare,
       BaseFare * 4.0                                             AS FirstFare
FROM FLIGHT
ORDER BY DurationMinutes DESC;

-- Q7  [calculated expression on dates: derived Customer.Age]
-- Purpose: customer ages (derived from date of birth), oldest first.
SELECT CustomerID,
       FirstName || ' ' || LastName                       AS CustomerName,
       DateOfBirth,
       TRUNC(MONTHS_BETWEEN(SYSDATE, DateOfBirth) / 12)   AS Age
FROM CUSTOMER
ORDER BY Age DESC;

-- =====================================================================
-- B. JOINS
-- =====================================================================

-- Q8  [equi-join / inner join]
-- Purpose: every booking with the name and email of the customer who
-- made it (equi-join written with a WHERE condition).
SELECT b.BookingRef, b.BookingDate, b.Status,
       c.FirstName || ' ' || c.LastName AS CustomerName, c.Email
FROM BOOKING b, CUSTOMER c
WHERE b.CustomerID = c.CustomerID
ORDER BY b.BookingDate;

-- Q9  [inner join, ANSI syntax]
-- Purpose: the seat and cabin of every traveler on every ticket.
SELECT t.BookingRef, tr.FullName, tr.Relationship, t.FlightID, t.SeatNo, t.CabinClass
FROM TICKET t
INNER JOIN TRAVELER tr
        ON tr.CustomerID = t.CustomerID
       AND tr.TravelerNo = t.TravelerNo
ORDER BY t.BookingRef, tr.FullName;

-- Q10 [natural join]
-- Purpose: each airline's fleet. AIRLINE and AIRCRAFT share only the
-- column AirlineID, so NATURAL JOIN joins on it.
SELECT IATACode, Name AS Airline, TailNo, Model, Capacity
FROM AIRLINE NATURAL JOIN AIRCRAFT
ORDER BY IATACode, TailNo;

-- Q11 [left outer join]
-- Purpose: how many flights each aircraft is assigned to, including
-- aircraft that are idle (no flights yet, shown as 0).
SELECT a.TailNo, a.Model, COUNT(f.FlightID) AS FlightsAssigned
FROM AIRCRAFT a
LEFT OUTER JOIN FLIGHT f ON f.TailNo = a.TailNo
GROUP BY a.TailNo, a.Model
ORDER BY FlightsAssigned, a.TailNo;

-- Q12 [left outer join + IS NULL]
-- Purpose: customers who have never made a booking, for a welcome offer.
SELECT c.CustomerID, c.FirstName, c.LastName, c.Email
FROM CUSTOMER c
LEFT OUTER JOIN BOOKING b ON b.CustomerID = c.CustomerID
WHERE b.BookingRef IS NULL
ORDER BY c.CustomerID;

-- Q13 [self-join]
-- Purpose: suggest return flights. Pairs two flights of the same airline
-- where the second flies the reverse route after the first one lands.
SELECT ob.FlightNo AS OutboundFlight, ob.OriginCode || ' -> ' || ob.DestCode AS OutboundRoute,
       ob.DepTime  AS OutboundDeparts,
       rt.FlightNo AS ReturnFlight,   rt.OriginCode || ' -> ' || rt.DestCode AS ReturnRoute,
       rt.DepTime  AS ReturnDeparts,
       ROUND(rt.DepTime - ob.ArrTime, 1) AS DaysAtDestination
FROM FLIGHT ob
JOIN FLIGHT rt
  ON rt.AirlineID  = ob.AirlineID
 AND rt.OriginCode = ob.DestCode
 AND rt.DestCode   = ob.OriginCode
 AND rt.DepTime    > ob.ArrTime
ORDER BY ob.DepTime;

-- Q14 [join of three or more tables]
-- Purpose: the full itinerary of every booking: who flies, on which
-- airline and flight, between which cities, in which seat.
SELECT b.BookingRef,
       tr.FullName                     AS Traveler,
       al.Name                         AS Airline,
       f.FlightNo,
       org.City || ' -> ' || dst.City  AS Route,
       f.DepTime,
       t.SeatNo, t.CabinClass, t.Fare
FROM BOOKING b
JOIN TICKET   t   ON t.BookingRef  = b.BookingRef
JOIN TRAVELER tr  ON tr.CustomerID = t.CustomerID AND tr.TravelerNo = t.TravelerNo
JOIN FLIGHT   f   ON f.FlightID    = t.FlightID
JOIN AIRLINE  al  ON al.AirlineID  = f.AirlineID
JOIN AIRPORT  org ON org.AirportCode = f.OriginCode
JOIN AIRPORT  dst ON dst.AirportCode = f.DestCode
ORDER BY b.BookingRef, f.DepTime, tr.FullName;

-- =====================================================================
-- C. AGGREGATES AND GROUPING
-- =====================================================================

-- Q15 [COUNT, SUM, AVG, MIN, MAX]
-- Purpose: overall ticket sales figures for the platform.
SELECT COUNT(*)            AS TicketsSold,
       SUM(Fare)           AS TotalRevenue,
       ROUND(AVG(Fare), 2) AS AverageFare,
       MIN(Fare)           AS CheapestTicket,
       MAX(Fare)           AS MostExpensiveTicket
FROM TICKET;

-- Q16 [GROUP BY]
-- Purpose: ticket revenue per airline, highest first.
SELECT al.Name AS Airline, COUNT(*) AS TicketsSold, SUM(t.Fare) AS Revenue
FROM TICKET t
JOIN FLIGHT  f  ON f.FlightID   = t.FlightID
JOIN AIRLINE al ON al.AirlineID = f.AirlineID
GROUP BY al.Name
ORDER BY Revenue DESC;

-- Q17 [GROUP BY]
-- Purpose: how popular each cabin class is and what it earns on average.
SELECT CabinClass, COUNT(*) AS Tickets, ROUND(AVG(Fare), 2) AS AverageFare,
       MIN(Fare) AS LowestFare, MAX(Fare) AS HighestFare
FROM TICKET
GROUP BY CabinClass
ORDER BY Tickets DESC;

-- Q18 [GROUP BY ... HAVING]
-- Purpose: repeat customers: those with more than one booking.
SELECT c.CustomerID, c.FirstName || ' ' || c.LastName AS CustomerName,
       COUNT(*) AS Bookings
FROM CUSTOMER c
JOIN BOOKING b ON b.CustomerID = c.CustomerID
GROUP BY c.CustomerID, c.FirstName, c.LastName
HAVING COUNT(*) > 1
ORDER BY Bookings DESC;

-- Q19 [GROUP BY ... HAVING]
-- Purpose: busiest departure airports: airports with 3 or more departures.
SELECT ap.AirportCode, ap.City, COUNT(*) AS Departures
FROM FLIGHT f
JOIN AIRPORT ap ON ap.AirportCode = f.OriginCode
GROUP BY ap.AirportCode, ap.City
HAVING COUNT(*) >= 3
ORDER BY Departures DESC;

-- =====================================================================
-- D. SUBQUERIES
-- =====================================================================

-- Q20 [single-row subquery]
-- Purpose: flights whose base fare is above the average base fare.
SELECT FlightNo, OriginCode, DestCode, BaseFare
FROM FLIGHT
WHERE BaseFare > (SELECT AVG(BaseFare) FROM FLIGHT)
ORDER BY BaseFare DESC;

-- Q21 [single-row subquery]
-- Purpose: the most expensive ticket ever sold and who travelled on it.
SELECT t.BookingRef, tr.FullName, t.FlightID, t.CabinClass, t.Fare
FROM TICKET t
JOIN TRAVELER tr ON tr.CustomerID = t.CustomerID AND tr.TravelerNo = t.TravelerNo
WHERE t.Fare = (SELECT MAX(Fare) FROM TICKET);

-- Q22 [multi-row subquery with IN]
-- Purpose: customers who have flown with Emirates (e.g. to target an
-- Emirates promotion).
SELECT CustomerID, FirstName, LastName
FROM CUSTOMER
WHERE CustomerID IN (SELECT b.CustomerID
                     FROM BOOKING b
                     JOIN TICKET t ON t.BookingRef = b.BookingRef
                     JOIN FLIGHT f ON f.FlightID = t.FlightID
                     JOIN AIRLINE al ON al.AirlineID = f.AirlineID
                     WHERE al.IATACode = 'EK')
ORDER BY CustomerID;

-- Q23 [multi-row subquery with ALL]
-- Purpose: flights more expensive than every Air Arabia flight, i.e.
-- the routes a budget traveler would not find on the low-cost carrier.
SELECT FlightNo, OriginCode, DestCode, BaseFare
FROM FLIGHT
WHERE BaseFare > ALL (SELECT f.BaseFare
                      FROM FLIGHT f
                      JOIN AIRLINE al ON al.AirlineID = f.AirlineID
                      WHERE al.IATACode = 'G9')
ORDER BY BaseFare;

-- Q24 [multi-row subquery with ANY]
-- Purpose: aircraft that are larger than at least one aircraft in the
-- Emirates fleet.
SELECT TailNo, Model, Capacity
FROM AIRCRAFT
WHERE Capacity > ANY (SELECT ac.Capacity
                      FROM AIRCRAFT ac
                      JOIN AIRLINE al ON al.AirlineID = ac.AirlineID
                      WHERE al.IATACode = 'EK')
ORDER BY Capacity DESC;

-- Q25 [multi-row subquery with NOT IN]
-- Purpose: customers who have not written any review yet.
SELECT CustomerID, FirstName, LastName
FROM CUSTOMER
WHERE CustomerID NOT IN (SELECT CustomerID FROM REVIEW)
ORDER BY CustomerID;

-- Q26 [correlated subquery]
-- Purpose: flights priced above their own airline's average base fare
-- (the inner query is re-evaluated for each airline of the outer row).
SELECT f.FlightNo, f.AirlineID, f.BaseFare
FROM FLIGHT f
WHERE f.BaseFare > (SELECT AVG(f2.BaseFare)
                    FROM FLIGHT f2
                    WHERE f2.AirlineID = f.AirlineID)
ORDER BY f.AirlineID, f.BaseFare DESC;

-- Q27 [correlated subquery with EXISTS]
-- Purpose: airlines that have at least one written review.
SELECT al.IATACode, al.Name
FROM AIRLINE al
WHERE EXISTS (SELECT 1
              FROM REVIEW r
              WHERE r.AirlineID = al.AirlineID
                AND r.ReviewComment IS NOT NULL)
ORDER BY al.Name;

-- Q28 [aggregate function with a subquery]
-- Purpose: the airline(s) with the highest average rating (derived
-- attribute Airline.AvgRating). The subquery nests two aggregates.
SELECT al.Name, ROUND(AVG(r.Rating), 2) AS AvgRating, COUNT(*) AS Reviews
FROM REVIEW r
JOIN AIRLINE al ON al.AirlineID = r.AirlineID
GROUP BY al.Name
HAVING AVG(r.Rating) = (SELECT MAX(AVG(Rating))
                        FROM REVIEW
                        GROUP BY AirlineID);

-- Q29 [aggregate function with a subquery]
-- Purpose: bookings whose total price (derived Booking.TotalPrice) is
-- above the average booking total.
SELECT BookingRef, SUM(Fare) AS TotalPrice
FROM TICKET
GROUP BY BookingRef
HAVING SUM(Fare) > (SELECT AVG(SUM(Fare))
                    FROM TICKET
                    GROUP BY BookingRef)
ORDER BY TotalPrice DESC;

-- Q30 [three levels of nested subqueries]
-- Purpose: customers who have booked travel to the United Kingdom.
-- Level 1: bookings -> Level 2: tickets -> Level 3: flights ->
-- Level 4: UK airports.
SELECT CustomerID, FirstName, LastName, Email
FROM CUSTOMER
WHERE CustomerID IN (
        SELECT CustomerID
        FROM BOOKING
        WHERE BookingRef IN (
                SELECT BookingRef
                FROM TICKET
                WHERE FlightID IN (
                        SELECT FlightID
                        FROM FLIGHT
                        WHERE DestCode IN (
                                SELECT AirportCode
                                FROM AIRPORT
                                WHERE Country = 'UK'))))
ORDER BY CustomerID;
