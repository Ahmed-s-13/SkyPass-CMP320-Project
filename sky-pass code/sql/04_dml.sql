-- =====================================================================
-- SkyPass - 04_dml.sql
-- INSERT / UPDATE / DELETE demonstrations that respect every declared
-- constraint and trigger. Run ONCE, after 03_data.sql. To start over,
-- re-run 01 -> 02 -> 03 first.
-- =====================================================================
SET DEFINE OFF

-- =====================================================================
-- INSERT
-- =====================================================================

-- I1. A new customer registers with two phone numbers. As in the GUI,
--     the customer is also stored as their own 'Self' traveler.
INSERT INTO CUSTOMER (CustomerID, FirstName, LastName, Email, DateOfBirth, Street, City, Country)
VALUES (13, 'Youssef', 'Amin', 'youssef.amin@gmail.com', DATE '1994-07-08',
        'Al Barsha 1, Street 27, Villa 4', 'Dubai', 'UAE');

INSERT INTO CUSTOMER_PHONE (CustomerID, Phone) VALUES (13, '+971504411223');
INSERT INTO CUSTOMER_PHONE (CustomerID, Phone) VALUES (13, '+97143998877');

INSERT INTO TRAVELER (CustomerID, TravelerNo, FullName, DateOfBirth, Relationship, PassportNo)
VALUES (13, 1, 'Youssef Amin', DATE '1994-07-08', 'Self', 'N7204518');

-- I2. Rohan saves a friend as a travel companion (weak entity: the
--     next TravelerNo is computed for that customer only).
INSERT INTO TRAVELER (CustomerID, TravelerNo, FullName, DateOfBirth, Relationship, PassportNo)
SELECT 3, NVL(MAX(TravelerNo), 0) + 1, 'Arjun Nair', DATE '1996-03-21', 'Friend', 'Z5521730'
FROM TRAVELER
WHERE CustomerID = 3;

-- I3. Youssef books EK001 to London on 8 Nov 2026 in economy.
--     Order: booking -> ticket(s) -> payment (the payment trigger
--     checks that the amount equals the sum of the fares).
INSERT INTO BOOKING (BookingRef, CustomerID, Status)
VALUES ('SP0113', 13, 'CONFIRMED');               -- BookingDate takes DEFAULT SYSDATE

INSERT INTO TICKET (BookingRef, CustomerID, TravelerNo, FlightID, SeatNo, CabinClass, Fare)
SELECT 'SP0113', 13, 1, FlightID, '33C', 'ECONOMY', BaseFare * 1.0
FROM FLIGHT
WHERE FlightID = 16;

INSERT INTO PAYMENT (PaymentID, BookingRef, Method, Amount)   -- PaidAt takes DEFAULT SYSTIMESTAMP
SELECT 13, 'SP0113', 'CARD', SUM(Fare)
FROM TICKET
WHERE BookingRef = 'SP0113';

SELECT b.BookingRef, b.Status, t.SeatNo, t.Fare, p.Amount, p.PaidAt
FROM BOOKING b
JOIN TICKET t  ON t.BookingRef = b.BookingRef
JOIN PAYMENT p ON p.BookingRef = b.BookingRef
WHERE b.BookingRef = 'SP0113';

-- =====================================================================
-- UPDATE
-- =====================================================================

-- U1. flydubai FZ445 to Mumbai is delayed by 45 minutes: shift both
--     times and change the status.
UPDATE FLIGHT
SET Status  = 'DELAYED',
    DepTime = DepTime + 45 / 1440,
    ArrTime = ArrTime + 45 / 1440
WHERE FlightNo = 'FZ445'
  AND DepTime = TO_DATE('2026-11-05 22:10', 'YYYY-MM-DD HH24:MI');

-- U2. Mariam's wallet payment has cleared, so her booking is confirmed.
UPDATE BOOKING
SET Status = 'CONFIRMED'
WHERE BookingRef = 'SP0108'
  AND Status = 'PENDING';

-- U3. Emirates raises the base fare of its upcoming flights by 5%.
--     Tickets already issued keep the fare recorded when they were
--     issued (BR12), so only future bookings are affected.
UPDATE FLIGHT
SET BaseFare = ROUND(BaseFare * 1.05, 2)
WHERE Status = 'SCHEDULED'
  AND AirlineID = (SELECT AirlineID FROM AIRLINE WHERE IATACode = 'EK');

-- U4. Rohan moves from Sharjah to Dubai.
UPDATE CUSTOMER
SET Street = 'Dubai Silicon Oasis, Axis Residence 2, Apt 610',
    City   = 'Dubai'
WHERE CustomerID = 3;

-- U5. After British Airways refunded the broken seat screens, James
--     raises his rating from 2 to 3 and edits his comment.
UPDATE REVIEW
SET Rating        = 3,
    ReviewComment = 'Left an hour late and two seat screens failed, but BA refunded us quickly.',
    ReviewDate    = SYSDATE
WHERE CustomerID = 5
  AND AirlineID = (SELECT AirlineID FROM AIRLINE WHERE IATACode = 'BA');

SELECT FlightNo, DepTime, BaseFare, Status
FROM FLIGHT
WHERE FlightNo IN ('FZ445', 'EK001', 'EK002', 'EK203')
ORDER BY DepTime;

-- =====================================================================
-- DELETE
-- =====================================================================

-- D1. Ahmed's home landline was disconnected (multi-valued attribute).
DELETE FROM CUSTOMER_PHONE
WHERE CustomerID = 7
  AND Phone = '+97142345678';

-- D2. Omar removes his son's saved traveler profile. Yousef holds no
--     tickets, so FK_TICKET_TRAVELER does not block the delete.
DELETE FROM TRAVELER
WHERE CustomerID = 1
  AND TravelerNo = 3;

-- D3. Remove the cancelled booking SP0110.
--     PAYMENT -> BOOKING is RESTRICT, so the payment record goes first;
--     TICKET -> BOOKING is ON DELETE CASCADE, so its ticket goes with it.
DELETE FROM PAYMENT
WHERE BookingRef IN (SELECT BookingRef FROM BOOKING WHERE Status = 'CANCELLED');

DELETE FROM BOOKING
WHERE Status = 'CANCELLED';

SELECT COUNT(*) AS tickets_left_for_sp0110
FROM TICKET
WHERE BookingRef = 'SP0110';

-- D4. Integrity check (left commented out on purpose). Deleting a
--     customer who still has bookings is rejected by FK_BOOKING_CUSTOMER
--     with ORA-02292: integrity constraint violated - child record found.
-- DELETE FROM CUSTOMER WHERE CustomerID = 1;

COMMIT;
