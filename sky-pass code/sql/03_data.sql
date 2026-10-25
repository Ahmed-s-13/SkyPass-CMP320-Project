-- =====================================================================
-- SkyPass - 03_data.sql
-- Realistic sample data. Run after 01_schema.sql and 02_triggers.sql.
--
-- Conventions
--   * Money is in UAE dirhams (AED).
--   * Flight times are stored in UAE time (GST).
--   * Ticket fare = BaseFare x cabin multiplier
--       ECONOMY 1.0, PREMIUM_ECONOMY 1.5, BUSINESS 2.5, FIRST 4.0
--   * Payment amount = sum of the booking's ticket fares (trigger).
--   * Reviews only for airlines the customer has flown (trigger).
--   * Flights before 25 Oct 2026 have flown (ARRIVED); later ones are
--     upcoming.
-- =====================================================================
SET DEFINE OFF

-- ---------------------------------------------------------------------
-- AIRPORT (12)
-- ---------------------------------------------------------------------
INSERT INTO AIRPORT VALUES ('DXB', 'Dubai International Airport',          'Dubai',     'UAE');
INSERT INTO AIRPORT VALUES ('AUH', 'Zayed International Airport',          'Abu Dhabi', 'UAE');
INSERT INTO AIRPORT VALUES ('SHJ', 'Sharjah International Airport',        'Sharjah',   'UAE');
INSERT INTO AIRPORT VALUES ('DOH', 'Hamad International Airport',          'Doha',      'Qatar');
INSERT INTO AIRPORT VALUES ('LHR', 'London Heathrow Airport',              'London',    'UK');
INSERT INTO AIRPORT VALUES ('CDG', 'Paris Charles de Gaulle Airport',      'Paris',     'France');
INSERT INTO AIRPORT VALUES ('FRA', 'Frankfurt Airport',                    'Frankfurt', 'Germany');
INSERT INTO AIRPORT VALUES ('IST', 'Istanbul Airport',                     'Istanbul',  'Turkiye');
INSERT INTO AIRPORT VALUES ('JFK', 'John F. Kennedy International Airport','New York',  'USA');
INSERT INTO AIRPORT VALUES ('BOM', 'Chhatrapati Shivaji Maharaj International Airport', 'Mumbai', 'India');
INSERT INTO AIRPORT VALUES ('CAI', 'Cairo International Airport',          'Cairo',     'Egypt');
INSERT INTO AIRPORT VALUES ('SIN', 'Singapore Changi Airport',             'Singapore', 'Singapore');

-- ---------------------------------------------------------------------
-- AIRLINE (10)
-- ---------------------------------------------------------------------
INSERT INTO AIRLINE VALUES (1,  'EK', 'Emirates');
INSERT INTO AIRLINE VALUES (2,  'EY', 'Etihad Airways');
INSERT INTO AIRLINE VALUES (3,  'G9', 'Air Arabia');
INSERT INTO AIRLINE VALUES (4,  'FZ', 'flydubai');
INSERT INTO AIRLINE VALUES (5,  'QR', 'Qatar Airways');
INSERT INTO AIRLINE VALUES (6,  'BA', 'British Airways');
INSERT INTO AIRLINE VALUES (7,  'TK', 'Turkish Airlines');
INSERT INTO AIRLINE VALUES (8,  'LH', 'Lufthansa');
INSERT INTO AIRLINE VALUES (9,  'AF', 'Air France');
INSERT INTO AIRLINE VALUES (10, 'SQ', 'Singapore Airlines');

-- ---------------------------------------------------------------------
-- SERVES (36): which airports each airline flies to (M:N)
-- ---------------------------------------------------------------------
INSERT INTO SERVES VALUES (1, 'DXB');
INSERT INTO SERVES VALUES (1, 'LHR');
INSERT INTO SERVES VALUES (1, 'JFK');
INSERT INTO SERVES VALUES (1, 'CDG');
INSERT INTO SERVES VALUES (1, 'BOM');
INSERT INTO SERVES VALUES (1, 'SIN');
INSERT INTO SERVES VALUES (1, 'FRA');
INSERT INTO SERVES VALUES (1, 'CAI');
INSERT INTO SERVES VALUES (2, 'AUH');
INSERT INTO SERVES VALUES (2, 'LHR');
INSERT INTO SERVES VALUES (2, 'JFK');
INSERT INTO SERVES VALUES (2, 'BOM');
INSERT INTO SERVES VALUES (3, 'SHJ');
INSERT INTO SERVES VALUES (3, 'CAI');
INSERT INTO SERVES VALUES (3, 'BOM');
INSERT INTO SERVES VALUES (3, 'IST');
INSERT INTO SERVES VALUES (4, 'DXB');
INSERT INTO SERVES VALUES (4, 'BOM');
INSERT INTO SERVES VALUES (4, 'IST');
INSERT INTO SERVES VALUES (4, 'CAI');
INSERT INTO SERVES VALUES (5, 'DOH');
INSERT INTO SERVES VALUES (5, 'CDG');
INSERT INTO SERVES VALUES (5, 'LHR');
INSERT INTO SERVES VALUES (5, 'DXB');
INSERT INTO SERVES VALUES (6, 'LHR');
INSERT INTO SERVES VALUES (6, 'DXB');
INSERT INTO SERVES VALUES (6, 'JFK');
INSERT INTO SERVES VALUES (7, 'IST');
INSERT INTO SERVES VALUES (7, 'DXB');
INSERT INTO SERVES VALUES (7, 'FRA');
INSERT INTO SERVES VALUES (8, 'FRA');
INSERT INTO SERVES VALUES (8, 'DXB');
INSERT INTO SERVES VALUES (9, 'CDG');
INSERT INTO SERVES VALUES (9, 'DXB');
INSERT INTO SERVES VALUES (10, 'SIN');
INSERT INTO SERVES VALUES (10, 'DXB');

-- ---------------------------------------------------------------------
-- AIRCRAFT (12). A6-AOA has no flights yet (used by the outer join).
-- ---------------------------------------------------------------------
INSERT INTO AIRCRAFT VALUES ('A6-EUA', 1,  'Airbus A380-800',     489);
INSERT INTO AIRCRAFT VALUES ('A6-EGO', 1,  'Boeing 777-300ER',    354);
INSERT INTO AIRCRAFT VALUES ('A6-BLA', 2,  'Boeing 787-9',        299);
INSERT INTO AIRCRAFT VALUES ('A6-ANA', 3,  'Airbus A320',         168);
INSERT INTO AIRCRAFT VALUES ('A6-AOA', 3,  'Airbus A321neo',      215);
INSERT INTO AIRCRAFT VALUES ('A6-FMA', 4,  'Boeing 737 MAX 8',    174);
INSERT INTO AIRCRAFT VALUES ('A7-BEA', 5,  'Boeing 777-300ER',    354);
INSERT INTO AIRCRAFT VALUES ('G-XWBA', 6,  'Airbus A350-1000',    331);
INSERT INTO AIRCRAFT VALUES ('TC-LGA', 7,  'Airbus A350-900',     329);
INSERT INTO AIRCRAFT VALUES ('D-AIXA', 8,  'Airbus A350-900',     293);
INSERT INTO AIRCRAFT VALUES ('F-HTYA', 9,  'Airbus A350-900',     324);
INSERT INTO AIRCRAFT VALUES ('9V-SMA', 10, 'Airbus A350-900',     253);

-- ---------------------------------------------------------------------
-- FLIGHT (20)
-- (FlightID, FlightNo, AirlineID, TailNo, Origin, Dest, Dep, Arr, BaseFare, Status)
-- EK001 and TK761 appear twice on different dates: same FlightNo,
-- different DepTime (the alternate key is (FlightNo, DepTime)).
-- ---------------------------------------------------------------------
INSERT INTO FLIGHT VALUES (1,  'EK001', 1, 'A6-EUA', 'DXB', 'LHR',
    TO_DATE('2026-09-14 07:45', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-09-14 15:35', 'YYYY-MM-DD HH24:MI'), 2150, 'ARRIVED');
INSERT INTO FLIGHT VALUES (2,  'EK002', 1, 'A6-EUA', 'LHR', 'DXB',
    TO_DATE('2026-09-21 16:00', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-09-21 23:00', 'YYYY-MM-DD HH24:MI'), 2150, 'ARRIVED');
INSERT INTO FLIGHT VALUES (3,  'EK203', 1, 'A6-EGO', 'DXB', 'JFK',
    TO_DATE('2026-10-03 08:30', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-10-03 22:40', 'YYYY-MM-DD HH24:MI'), 4200, 'ARRIVED');
INSERT INTO FLIGHT VALUES (4,  'EY011', 2, 'A6-BLA', 'AUH', 'LHR',
    TO_DATE('2026-10-02 09:40', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-10-02 17:20', 'YYYY-MM-DD HH24:MI'), 1980, 'ARRIVED');
INSERT INTO FLIGHT VALUES (5,  'G9501', 3, 'A6-ANA', 'SHJ', 'CAI',
    TO_DATE('2026-10-10 10:15', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-10-10 14:20', 'YYYY-MM-DD HH24:MI'), 650,  'ARRIVED');
INSERT INTO FLIGHT VALUES (6,  'G9502', 3, 'A6-ANA', 'CAI', 'SHJ',
    TO_DATE('2026-10-17 15:30', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-10-17 19:30', 'YYYY-MM-DD HH24:MI'), 650,  'ARRIVED');
INSERT INTO FLIGHT VALUES (7,  'FZ445', 4, 'A6-FMA', 'DXB', 'BOM',
    TO_DATE('2026-11-05 22:10', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-11-06 01:15', 'YYYY-MM-DD HH24:MI'), 720,  'SCHEDULED');
INSERT INTO FLIGHT VALUES (8,  'QR039', 5, 'A7-BEA', 'DOH', 'CDG',
    TO_DATE('2026-09-28 08:00', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-09-28 15:00', 'YYYY-MM-DD HH24:MI'), 2400, 'ARRIVED');
INSERT INTO FLIGHT VALUES (9,  'BA107', 6, 'G-XWBA', 'DXB', 'LHR',
    TO_DATE('2026-10-01 14:25', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-10-01 22:05', 'YYYY-MM-DD HH24:MI'), 2300, 'ARRIVED');
INSERT INTO FLIGHT VALUES (10, 'TK761', 7, 'TC-LGA', 'DXB', 'IST',
    TO_DATE('2026-10-05 11:50', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-10-05 16:40', 'YYYY-MM-DD HH24:MI'), 1350, 'ARRIVED');
INSERT INTO FLIGHT VALUES (11, 'TK762', 7, 'TC-LGA', 'IST', 'DXB',
    TO_DATE('2026-10-12 21:00', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-10-13 01:20', 'YYYY-MM-DD HH24:MI'), 1350, 'ARRIVED');
INSERT INTO FLIGHT VALUES (12, 'LH631', 8, 'D-AIXA', 'DXB', 'FRA',
    TO_DATE('2026-10-08 09:05', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-10-08 16:10', 'YYYY-MM-DD HH24:MI'), 1900, 'ARRIVED');
INSERT INTO FLIGHT VALUES (13, 'AF655', 9, 'F-HTYA', 'DXB', 'CDG',
    TO_DATE('2026-12-20 10:30', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-12-20 17:50', 'YYYY-MM-DD HH24:MI'), 2050, 'SCHEDULED');
INSERT INTO FLIGHT VALUES (14, 'SQ495', 10, '9V-SMA', 'DXB', 'SIN',
    TO_DATE('2026-10-06 03:40', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-10-06 11:10', 'YYYY-MM-DD HH24:MI'), 2600, 'ARRIVED');
INSERT INTO FLIGHT VALUES (15, 'EY012', 2, 'A6-BLA', 'LHR', 'AUH',
    TO_DATE('2026-10-09 21:30', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-10-10 05:00', 'YYYY-MM-DD HH24:MI'), 1980, 'ARRIVED');
INSERT INTO FLIGHT VALUES (16, 'EK001', 1, 'A6-EUA', 'DXB', 'LHR',
    TO_DATE('2026-11-08 07:45', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-11-08 15:35', 'YYYY-MM-DD HH24:MI'), 2150, 'SCHEDULED');
INSERT INTO FLIGHT VALUES (17, 'EY101', 2, 'A6-BLA', 'AUH', 'JFK',
    TO_DATE('2026-11-15 10:00', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-11-16 00:10', 'YYYY-MM-DD HH24:MI'), 3900, 'SCHEDULED');
INSERT INTO FLIGHT VALUES (18, 'G9503', 3, 'A6-ANA', 'SHJ', 'IST',
    TO_DATE('2026-11-03 08:20', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-11-03 12:50', 'YYYY-MM-DD HH24:MI'), 820,  'SCHEDULED');
INSERT INTO FLIGHT VALUES (19, 'TK761', 7, 'TC-LGA', 'DXB', 'IST',
    TO_DATE('2026-12-12 11:50', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-12-12 16:40', 'YYYY-MM-DD HH24:MI'), 1350, 'SCHEDULED');
INSERT INTO FLIGHT VALUES (20, 'BA109', 6, 'G-XWBA', 'DXB', 'LHR',
    TO_DATE('2027-01-04 14:25', 'YYYY-MM-DD HH24:MI'), TO_DATE('2027-01-04 22:05', 'YYYY-MM-DD HH24:MI'), 2300, 'SCHEDULED');

-- ---------------------------------------------------------------------
-- FLIGHT_AMENITY. G9502 (flight 6) has no amenities on purpose.
-- ---------------------------------------------------------------------
INSERT INTO FLIGHT_AMENITY VALUES (1,  'Wi-Fi');
INSERT INTO FLIGHT_AMENITY VALUES (1,  'Meals');
INSERT INTO FLIGHT_AMENITY VALUES (1,  'Power outlets');
INSERT INTO FLIGHT_AMENITY VALUES (1,  'Entertainment');
INSERT INTO FLIGHT_AMENITY VALUES (2,  'Wi-Fi');
INSERT INTO FLIGHT_AMENITY VALUES (2,  'Meals');
INSERT INTO FLIGHT_AMENITY VALUES (2,  'Power outlets');
INSERT INTO FLIGHT_AMENITY VALUES (2,  'Entertainment');
INSERT INTO FLIGHT_AMENITY VALUES (3,  'Wi-Fi');
INSERT INTO FLIGHT_AMENITY VALUES (3,  'Meals');
INSERT INTO FLIGHT_AMENITY VALUES (3,  'Power outlets');
INSERT INTO FLIGHT_AMENITY VALUES (3,  'Entertainment');
INSERT INTO FLIGHT_AMENITY VALUES (4,  'Wi-Fi');
INSERT INTO FLIGHT_AMENITY VALUES (4,  'Meals');
INSERT INTO FLIGHT_AMENITY VALUES (4,  'Entertainment');
INSERT INTO FLIGHT_AMENITY VALUES (5,  'Power outlets');
INSERT INTO FLIGHT_AMENITY VALUES (7,  'Wi-Fi');
INSERT INTO FLIGHT_AMENITY VALUES (8,  'Wi-Fi');
INSERT INTO FLIGHT_AMENITY VALUES (8,  'Meals');
INSERT INTO FLIGHT_AMENITY VALUES (8,  'Power outlets');
INSERT INTO FLIGHT_AMENITY VALUES (8,  'Entertainment');
INSERT INTO FLIGHT_AMENITY VALUES (9,  'Wi-Fi');
INSERT INTO FLIGHT_AMENITY VALUES (9,  'Meals');
INSERT INTO FLIGHT_AMENITY VALUES (9,  'Entertainment');
INSERT INTO FLIGHT_AMENITY VALUES (10, 'Meals');
INSERT INTO FLIGHT_AMENITY VALUES (10, 'Entertainment');
INSERT INTO FLIGHT_AMENITY VALUES (11, 'Meals');
INSERT INTO FLIGHT_AMENITY VALUES (11, 'Entertainment');
INSERT INTO FLIGHT_AMENITY VALUES (12, 'Wi-Fi');
INSERT INTO FLIGHT_AMENITY VALUES (12, 'Meals');
INSERT INTO FLIGHT_AMENITY VALUES (12, 'Power outlets');
INSERT INTO FLIGHT_AMENITY VALUES (13, 'Meals');
INSERT INTO FLIGHT_AMENITY VALUES (13, 'Entertainment');
INSERT INTO FLIGHT_AMENITY VALUES (14, 'Wi-Fi');
INSERT INTO FLIGHT_AMENITY VALUES (14, 'Meals');
INSERT INTO FLIGHT_AMENITY VALUES (14, 'Power outlets');
INSERT INTO FLIGHT_AMENITY VALUES (14, 'Entertainment');
INSERT INTO FLIGHT_AMENITY VALUES (14, 'Extra legroom');
INSERT INTO FLIGHT_AMENITY VALUES (15, 'Wi-Fi');
INSERT INTO FLIGHT_AMENITY VALUES (15, 'Meals');
INSERT INTO FLIGHT_AMENITY VALUES (15, 'Entertainment');
INSERT INTO FLIGHT_AMENITY VALUES (16, 'Wi-Fi');
INSERT INTO FLIGHT_AMENITY VALUES (16, 'Meals');
INSERT INTO FLIGHT_AMENITY VALUES (16, 'Power outlets');
INSERT INTO FLIGHT_AMENITY VALUES (16, 'Entertainment');
INSERT INTO FLIGHT_AMENITY VALUES (17, 'Wi-Fi');
INSERT INTO FLIGHT_AMENITY VALUES (17, 'Meals');
INSERT INTO FLIGHT_AMENITY VALUES (17, 'Entertainment');
INSERT INTO FLIGHT_AMENITY VALUES (18, 'Power outlets');
INSERT INTO FLIGHT_AMENITY VALUES (19, 'Meals');
INSERT INTO FLIGHT_AMENITY VALUES (19, 'Entertainment');
INSERT INTO FLIGHT_AMENITY VALUES (20, 'Wi-Fi');
INSERT INTO FLIGHT_AMENITY VALUES (20, 'Meals');
INSERT INTO FLIGHT_AMENITY VALUES (20, 'Entertainment');

-- ---------------------------------------------------------------------
-- CUSTOMER (12). Customer 12 has no bookings (used by the outer join).
-- ---------------------------------------------------------------------
INSERT INTO CUSTOMER VALUES (1,  'Omar',        'Haddad',      'omar.haddad@gmail.com',      DATE '1990-03-14', 'Al Wasl Road, Villa 12',             'Dubai',     'UAE');
INSERT INTO CUSTOMER VALUES (2,  'Layla',       'Mansour',     'layla.mansour@outlook.com',  DATE '1987-07-22', 'Corniche Street, Tower B, Apt 1504', 'Abu Dhabi', 'UAE');
INSERT INTO CUSTOMER VALUES (3,  'Rohan',       'Mehta',       'rohan.mehta@yahoo.com',      DATE '1995-11-02', 'Al Nahda 2, Building 7',             'Sharjah',   'UAE');
INSERT INTO CUSTOMER VALUES (4,  'Sara',        'Al Mazrouei', 'sara.almazrouei@gmail.com',  DATE '1999-01-30', 'Khalifa City A, Street 21',          'Abu Dhabi', 'UAE');
INSERT INTO CUSTOMER VALUES (5,  'James',       'Whitfield',   'j.whitfield@protonmail.com', DATE '1982-05-09', 'Dubai Marina, Marina Gate 1, Apt 2203', 'Dubai',  'UAE');
INSERT INTO CUSTOMER VALUES (6,  'Fatima Zahra','Benali',      'fz.benali@gmail.com',        DATE '1993-09-18', 'Al Majaz 3, Jamal Abdul Nasser Street', 'Sharjah', 'UAE');
INSERT INTO CUSTOMER VALUES (7,  'Ahmed',       'Khalil',      'ahmed.khalil@hotmail.com',   DATE '1978-12-05', 'Jumeirah 1, Street 14B',             'Dubai',     'UAE');
INSERT INTO CUSTOMER VALUES (8,  'Mariam',      'Youssef',     'mariam.youssef@gmail.com',   DATE '2001-04-27', 'University City, Block C',           'Sharjah',   'UAE');
INSERT INTO CUSTOMER VALUES (9,  'Daniel',      'Okafor',      'daniel.okafor@gmail.com',    DATE '1989-08-11', 'Business Bay, Executive Towers',     'Dubai',     'UAE');
INSERT INTO CUSTOMER VALUES (10, 'Noura',       'Al Suwaidi',  'noura.suwaidi@outlook.com',  DATE '1996-02-19', 'Al Rashidiya, Street 9',             'Ajman',     'UAE');
INSERT INTO CUSTOMER VALUES (11, 'Elena',       'Petrova',     'elena.petrova@mail.com',     DATE '1991-06-03', 'JLT Cluster D, Apt 908',             'Dubai',     'UAE');
INSERT INTO CUSTOMER VALUES (12, 'Karim',       'Nassar',      'karim.nassar@gmail.com',     DATE '1985-10-25', 'Al Reem Island, Sun Tower, Apt 3107','Abu Dhabi', 'UAE');

-- CUSTOMER_PHONE (15): some customers have two numbers.
INSERT INTO CUSTOMER_PHONE VALUES (1,  '+971501234567');
INSERT INTO CUSTOMER_PHONE VALUES (1,  '+97143456789');
INSERT INTO CUSTOMER_PHONE VALUES (2,  '+971502345678');
INSERT INTO CUSTOMER_PHONE VALUES (3,  '+971553456789');
INSERT INTO CUSTOMER_PHONE VALUES (4,  '+971504567890');
INSERT INTO CUSTOMER_PHONE VALUES (5,  '+971525678901');
INSERT INTO CUSTOMER_PHONE VALUES (5,  '+447700900123');
INSERT INTO CUSTOMER_PHONE VALUES (6,  '+971566789012');
INSERT INTO CUSTOMER_PHONE VALUES (7,  '+971507890123');
INSERT INTO CUSTOMER_PHONE VALUES (7,  '+97142345678');
INSERT INTO CUSTOMER_PHONE VALUES (8,  '+971558901234');
INSERT INTO CUSTOMER_PHONE VALUES (9,  '+971509012345');
INSERT INTO CUSTOMER_PHONE VALUES (10, '+971561234098');
INSERT INTO CUSTOMER_PHONE VALUES (11, '+971527654321');
INSERT INTO CUSTOMER_PHONE VALUES (12, '+971503216549');

-- ---------------------------------------------------------------------
-- TRAVELER (20): every customer is also their own 'Self' traveler
-- (TravelerNo 1), plus 8 saved companions.
-- ---------------------------------------------------------------------
INSERT INTO TRAVELER VALUES (1,  1, 'Omar Haddad',          DATE '1990-03-14', 'Self',    'N4821937');
INSERT INTO TRAVELER VALUES (1,  2, 'Hana Haddad',          DATE '1992-06-01', 'Spouse',  'N5512840');
INSERT INTO TRAVELER VALUES (1,  3, 'Yousef Haddad',        DATE '2018-09-12', 'Child',   'N7730215');
INSERT INTO TRAVELER VALUES (2,  1, 'Layla Mansour',        DATE '1987-07-22', 'Self',    'LB2209871');
INSERT INTO TRAVELER VALUES (2,  2, 'Nadia Mansour',        DATE '1960-02-14', 'Parent',  'LB1130448');
INSERT INTO TRAVELER VALUES (3,  1, 'Rohan Mehta',          DATE '1995-11-02', 'Self',    'Z4410982');
INSERT INTO TRAVELER VALUES (4,  1, 'Sara Al Mazrouei',     DATE '1999-01-30', 'Self',    'N6620394');
INSERT INTO TRAVELER VALUES (5,  1, 'James Whitfield',      DATE '1982-05-09', 'Self',    '533829104');
INSERT INTO TRAVELER VALUES (5,  2, 'Claire Whitfield',     DATE '1984-03-30', 'Spouse',  '533829187');
INSERT INTO TRAVELER VALUES (5,  3, 'Oliver Whitfield',     DATE '2015-12-01', 'Child',   '533829255');
INSERT INTO TRAVELER VALUES (6,  1, 'Fatima Zahra Benali',  DATE '1993-09-18', 'Self',    'TZ7781203');
INSERT INTO TRAVELER VALUES (7,  1, 'Ahmed Khalil',         DATE '1978-12-05', 'Self',    'A30918275');
INSERT INTO TRAVELER VALUES (7,  2, 'Rania Khalil',         DATE '1981-07-07', 'Spouse',  'A30918390');
INSERT INTO TRAVELER VALUES (7,  3, 'Zain Khalil',          DATE '2010-04-20', 'Child',   'A30918412');
INSERT INTO TRAVELER VALUES (8,  1, 'Mariam Youssef',       DATE '2001-04-27', 'Self',    'N8845120');
INSERT INTO TRAVELER VALUES (9,  1, 'Daniel Okafor',        DATE '1989-08-11', 'Self',    'B02784561');
INSERT INTO TRAVELER VALUES (9,  2, 'Chidi Okafor',         DATE '1992-01-15', 'Sibling', 'B02791334');
INSERT INTO TRAVELER VALUES (10, 1, 'Noura Al Suwaidi',     DATE '1996-02-19', 'Self',    'N9031276');
INSERT INTO TRAVELER VALUES (11, 1, 'Elena Petrova',        DATE '1991-06-03', 'Self',    '758392016');
INSERT INTO TRAVELER VALUES (12, 1, 'Karim Nassar',         DATE '1985-10-25', 'Self',    'RL0482913');

-- ---------------------------------------------------------------------
-- BOOKING (12)
-- ---------------------------------------------------------------------
INSERT INTO BOOKING VALUES ('SP0101', 1,  DATE '2026-08-20', 'CONFIRMED');
INSERT INTO BOOKING VALUES ('SP0102', 2,  DATE '2026-09-01', 'CONFIRMED');
INSERT INTO BOOKING VALUES ('SP0103', 3,  DATE '2026-09-15', 'CONFIRMED');
INSERT INTO BOOKING VALUES ('SP0104', 4,  DATE '2026-09-05', 'CONFIRMED');
INSERT INTO BOOKING VALUES ('SP0105', 5,  DATE '2026-08-28', 'CONFIRMED');
INSERT INTO BOOKING VALUES ('SP0106', 6,  DATE '2026-09-20', 'CONFIRMED');
INSERT INTO BOOKING VALUES ('SP0107', 7,  DATE '2026-09-12', 'CONFIRMED');
INSERT INTO BOOKING VALUES ('SP0108', 8,  DATE '2026-10-18', 'PENDING');
INSERT INTO BOOKING VALUES ('SP0109', 9,  DATE '2026-09-10', 'CONFIRMED');
INSERT INTO BOOKING VALUES ('SP0110', 10, DATE '2026-10-15', 'CANCELLED');
INSERT INTO BOOKING VALUES ('SP0111', 11, DATE '2026-09-22', 'CONFIRMED');
INSERT INTO BOOKING VALUES ('SP0112', 1,  DATE '2026-09-25', 'CONFIRMED');

-- ---------------------------------------------------------------------
-- TICKET (26): (BookingRef, CustomerID, TravelerNo, FlightID, Seat, Cabin, Fare)
-- ---------------------------------------------------------------------
-- SP0101: Omar + Hana, Dubai-London return on Emirates, economy (4 x 2150 = 8600)
INSERT INTO TICKET VALUES ('SP0101', 1, 1, 1,  '32A', 'ECONOMY',          2150);
INSERT INTO TICKET VALUES ('SP0101', 1, 2, 1,  '32B', 'ECONOMY',          2150);
INSERT INTO TICKET VALUES ('SP0101', 1, 1, 2,  '41A', 'ECONOMY',          2150);
INSERT INTO TICKET VALUES ('SP0101', 1, 2, 2,  '41B', 'ECONOMY',          2150);
-- SP0102: Layla + her mother, Abu Dhabi-London return, business (4 x 4950 = 19800)
INSERT INTO TICKET VALUES ('SP0102', 2, 1, 4,  '2A',  'BUSINESS',         4950);
INSERT INTO TICKET VALUES ('SP0102', 2, 2, 4,  '2D',  'BUSINESS',         4950);
INSERT INTO TICKET VALUES ('SP0102', 2, 1, 15, '3A',  'BUSINESS',         4950);
INSERT INTO TICKET VALUES ('SP0102', 2, 2, 15, '3D',  'BUSINESS',         4950);
-- SP0103: Rohan, Sharjah-Cairo return on Air Arabia (2 x 650 = 1300)
INSERT INTO TICKET VALUES ('SP0103', 3, 1, 5,  '14C', 'ECONOMY',          650);
INSERT INTO TICKET VALUES ('SP0103', 3, 1, 6,  '9F',  'ECONOMY',          650);
-- SP0104: Sara, Doha-Paris, premium economy (2400 x 1.5 = 3600)
INSERT INTO TICKET VALUES ('SP0104', 4, 1, 8,  '21A', 'PREMIUM_ECONOMY',  3600);
-- SP0105: the Whitfield family, Dubai-London on BA (3 x 2300 = 6900)
INSERT INTO TICKET VALUES ('SP0105', 5, 1, 9,  '45A', 'ECONOMY',          2300);
INSERT INTO TICKET VALUES ('SP0105', 5, 2, 9,  '45B', 'ECONOMY',          2300);
INSERT INTO TICKET VALUES ('SP0105', 5, 3, 9,  '45C', 'ECONOMY',          2300);
-- SP0106: Fatima Zahra, Dubai-Istanbul return (2 x 1350 = 2700)
INSERT INTO TICKET VALUES ('SP0106', 6, 1, 10, '18A', 'ECONOMY',          1350);
INSERT INTO TICKET VALUES ('SP0106', 6, 1, 11, '22F', 'ECONOMY',          1350);
-- SP0107: the Khalil family, Dubai-New York, business (3 x 10500 = 31500)
INSERT INTO TICKET VALUES ('SP0107', 7, 1, 3,  '7A',  'BUSINESS',         10500);
INSERT INTO TICKET VALUES ('SP0107', 7, 2, 3,  '7D',  'BUSINESS',         10500);
INSERT INTO TICKET VALUES ('SP0107', 7, 3, 3,  '7G',  'BUSINESS',         10500);
-- SP0108: Mariam, Dubai-Mumbai on flydubai (720)
INSERT INTO TICKET VALUES ('SP0108', 8, 1, 7,  '12C', 'ECONOMY',          720);
-- SP0109: Daniel + his brother, Dubai-Frankfurt, premium economy (2 x 2850 = 5700)
INSERT INTO TICKET VALUES ('SP0109', 9, 1, 12, '24A', 'PREMIUM_ECONOMY',  2850);
INSERT INTO TICKET VALUES ('SP0109', 9, 2, 12, '24B', 'PREMIUM_ECONOMY',  2850);
-- SP0110: Noura, Dubai-Paris (2050), booking later cancelled
INSERT INTO TICKET VALUES ('SP0110', 10, 1, 13, '30E', 'ECONOMY',         2050);
-- SP0111: Elena, Dubai-Singapore, first class (2600 x 4 = 10400)
INSERT INTO TICKET VALUES ('SP0111', 11, 1, 14, '1A',  'FIRST',           10400);
-- SP0112: Omar again, Dubai-Istanbul return (2 x 1350 = 2700)
INSERT INTO TICKET VALUES ('SP0112', 1, 1, 10, '18B', 'ECONOMY',          1350);
INSERT INTO TICKET VALUES ('SP0112', 1, 1, 11, '22E', 'ECONOMY',          1350);

-- ---------------------------------------------------------------------
-- PAYMENT (12): one per booking, amount = sum of its ticket fares.
-- ---------------------------------------------------------------------
INSERT INTO PAYMENT VALUES (1,  'SP0101', 'CARD',          8600,  TIMESTAMP '2026-08-20 10:12:00');
INSERT INTO PAYMENT VALUES (2,  'SP0102', 'CARD',          19800, TIMESTAMP '2026-09-01 18:40:00');
INSERT INTO PAYMENT VALUES (3,  'SP0103', 'WALLET',        1300,  TIMESTAMP '2026-09-15 09:05:00');
INSERT INTO PAYMENT VALUES (4,  'SP0104', 'CARD',          3600,  TIMESTAMP '2026-09-05 21:17:00');
INSERT INTO PAYMENT VALUES (5,  'SP0105', 'CARD',          6900,  TIMESTAMP '2026-08-28 12:30:00');
INSERT INTO PAYMENT VALUES (6,  'SP0106', 'BANK_TRANSFER', 2700,  TIMESTAMP '2026-09-21 08:55:00');
INSERT INTO PAYMENT VALUES (7,  'SP0107', 'CARD',          31500, TIMESTAMP '2026-09-12 16:02:00');
INSERT INTO PAYMENT VALUES (8,  'SP0108', 'WALLET',        720,   TIMESTAMP '2026-10-18 23:48:00');
INSERT INTO PAYMENT VALUES (9,  'SP0109', 'CARD',          5700,  TIMESTAMP '2026-09-10 11:20:00');
INSERT INTO PAYMENT VALUES (10, 'SP0110', 'CARD',          2050,  TIMESTAMP '2026-10-15 14:09:00');
INSERT INTO PAYMENT VALUES (11, 'SP0111', 'CARD',          10400, TIMESTAMP '2026-09-22 19:33:00');
INSERT INTO PAYMENT VALUES (12, 'SP0112', 'WALLET',        2700,  TIMESTAMP '2026-09-25 07:41:00');

-- ---------------------------------------------------------------------
-- REVIEW (10): only for airlines the customer has flown with.
-- Two reviews have no comment (rating only).
-- ---------------------------------------------------------------------
INSERT INTO REVIEW VALUES (1,  1,  5, 'Smooth A380 flight and a very helpful cabin crew.',                 DATE '2026-09-24');
INSERT INTO REVIEW VALUES (1,  7,  4, 'Great food on board, but boarding in Istanbul was slow.',           DATE '2026-10-14');
INSERT INTO REVIEW VALUES (2,  2,  5, 'Business seats were comfortable and the service was attentive.',    DATE '2026-10-11');
INSERT INTO REVIEW VALUES (3,  3,  3, 'Fine for the price, but the legroom is tight.',                     DATE '2026-10-18');
INSERT INTO REVIEW VALUES (4,  5,  5, 'Premium economy felt close to business class.',                     DATE '2026-09-30');
INSERT INTO REVIEW VALUES (5,  6,  2, 'Left an hour late and two of our seat screens did not work.',       DATE '2026-10-03');
INSERT INTO REVIEW VALUES (6,  7,  4, NULL,                                                                DATE '2026-10-14');
INSERT INTO REVIEW VALUES (7,  1,  4, 'The flat beds made the long flight to New York easy with kids.',    DATE '2026-10-05');
INSERT INTO REVIEW VALUES (9,  8,  3, NULL,                                                                DATE '2026-10-09');
INSERT INTO REVIEW VALUES (11, 10, 5, 'The best first-class trip I have taken.',                           DATE '2026-10-07');

COMMIT;

-- Check: row count per table.
SELECT 'CUSTOMER' AS table_name, COUNT(*) AS row_count FROM CUSTOMER
UNION ALL SELECT 'CUSTOMER_PHONE', COUNT(*) FROM CUSTOMER_PHONE
UNION ALL SELECT 'TRAVELER',       COUNT(*) FROM TRAVELER
UNION ALL SELECT 'AIRLINE',        COUNT(*) FROM AIRLINE
UNION ALL SELECT 'AIRPORT',        COUNT(*) FROM AIRPORT
UNION ALL SELECT 'SERVES',         COUNT(*) FROM SERVES
UNION ALL SELECT 'AIRCRAFT',       COUNT(*) FROM AIRCRAFT
UNION ALL SELECT 'FLIGHT',         COUNT(*) FROM FLIGHT
UNION ALL SELECT 'FLIGHT_AMENITY', COUNT(*) FROM FLIGHT_AMENITY
UNION ALL SELECT 'BOOKING',        COUNT(*) FROM BOOKING
UNION ALL SELECT 'PAYMENT',        COUNT(*) FROM PAYMENT
UNION ALL SELECT 'TICKET',         COUNT(*) FROM TICKET
UNION ALL SELECT 'REVIEW',         COUNT(*) FROM REVIEW;
