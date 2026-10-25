-- =====================================================================
-- SkyPass - 02_triggers.sql
-- Business rules from Stage 1 section 5.3 that a CHECK or FOREIGN KEY
-- cannot express. Each trigger raises a readable error (-20001..-20005)
-- that the GUI shows to the user as-is.
--
-- Note on BR2 ('Self' traveler): TRAVELER.PassportNo is NOT NULL and
-- CUSTOMER has no passport column, so a trigger cannot create the Self
-- traveler. The GUI creates it in the same transaction as the customer.
-- =====================================================================
SET DEFINE OFF

-- ---------------------------------------------------------------------
-- BR1 / BR2: a date of birth cannot be in the future.
-- (Oracle does not allow SYSDATE inside a CHECK constraint.)
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_CUSTOMER_DOB
BEFORE INSERT OR UPDATE OF DateOfBirth ON CUSTOMER
FOR EACH ROW
BEGIN
    IF :NEW.DateOfBirth > SYSDATE THEN
        RAISE_APPLICATION_ERROR(-20001, 'Date of birth cannot be in the future.');
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_TRAVELER_DOB
BEFORE INSERT OR UPDATE OF DateOfBirth ON TRAVELER
FOR EACH ROW
BEGIN
    IF :NEW.DateOfBirth > SYSDATE THEN
        RAISE_APPLICATION_ERROR(-20001, 'Date of birth cannot be in the future.');
    END IF;
END;
/

-- ---------------------------------------------------------------------
-- BR5: every traveler on a booking must belong to the customer who
-- made that booking (TICKET.CustomerID = BOOKING.CustomerID).
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_TICKET_TRAVELER_OWNER
BEFORE INSERT OR UPDATE OF BookingRef, CustomerID ON TICKET
FOR EACH ROW
DECLARE
    v_owner BOOKING.CustomerID%TYPE;
BEGIN
    SELECT CustomerID INTO v_owner
    FROM BOOKING
    WHERE BookingRef = :NEW.BookingRef;

    IF v_owner <> :NEW.CustomerID THEN
        RAISE_APPLICATION_ERROR(-20002,
            'A ticket''s traveler must belong to the customer who made the booking.');
    END IF;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        NULL;  -- the foreign key FK_TICKET_BOOKING reports the missing booking
END;
/

-- ---------------------------------------------------------------------
-- BR8: the number of tickets on a flight cannot exceed the capacity of
-- its aircraft. Statement-level (no FOR EACH ROW) so it can read TICKET
-- without the "mutating table" error, after the whole statement ran.
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_TICKET_CAPACITY
AFTER INSERT OR UPDATE OF FlightID ON TICKET
DECLARE
    v_full_flight FLIGHT.FlightNo%TYPE;
BEGIN
    SELECT MIN(f.FlightNo) INTO v_full_flight
    FROM FLIGHT f
    JOIN AIRCRAFT a ON a.TailNo = f.TailNo
    WHERE (SELECT COUNT(*) FROM TICKET t WHERE t.FlightID = f.FlightID) > a.Capacity;

    IF v_full_flight IS NOT NULL THEN
        RAISE_APPLICATION_ERROR(-20003,
            'Flight ' || v_full_flight || ' is full: every seat on this aircraft is taken.');
    END IF;
END;
/

-- ---------------------------------------------------------------------
-- BR11: a customer may only review an airline they have flown with,
-- i.e. they hold a ticket (in a booking that is not cancelled) on a
-- flight operated by that airline.
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_REVIEW_FLOWN
BEFORE INSERT OR UPDATE OF CustomerID, AirlineID ON REVIEW
FOR EACH ROW
DECLARE
    v_trips NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_trips
    FROM TICKET t
    JOIN BOOKING b ON b.BookingRef = t.BookingRef
    JOIN FLIGHT f  ON f.FlightID = t.FlightID
    WHERE b.CustomerID = :NEW.CustomerID
      AND f.AirlineID  = :NEW.AirlineID
      AND b.Status    <> 'CANCELLED';

    IF v_trips = 0 THEN
        RAISE_APPLICATION_ERROR(-20004,
            'A customer can only review an airline they have flown with.');
    END IF;
END;
/

-- ---------------------------------------------------------------------
-- BR6: the payment amount must equal the sum of the booking's ticket
-- fares. Tickets are inserted before the payment (same transaction).
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_PAYMENT_AMOUNT
BEFORE INSERT OR UPDATE OF Amount, BookingRef ON PAYMENT
FOR EACH ROW
DECLARE
    v_total NUMBER(10,2);
BEGIN
    SELECT NVL(SUM(Fare), 0) INTO v_total
    FROM TICKET
    WHERE BookingRef = :NEW.BookingRef;

    IF :NEW.Amount <> v_total THEN
        RAISE_APPLICATION_ERROR(-20005,
            'Payment amount ' || TO_CHAR(:NEW.Amount, 'FM999,990.00') ||
            ' must equal the booking total ' || TO_CHAR(v_total, 'FM999,990.00') || '.');
    END IF;
END;
/

-- Check: all triggers should show STATUS = ENABLED.
SELECT trigger_name, table_name, status
FROM user_triggers
ORDER BY table_name, trigger_name;
