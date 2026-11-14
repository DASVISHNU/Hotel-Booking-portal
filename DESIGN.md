# Design write-up

## 1. Architecture

```
React (Vite, :5173)  ──/api (proxied)──▶  Spring Boot REST API (:8080)  ──JPA/JDBC──▶  MySQL 8
  pages + components                      Controller → Service → Repository           Flyway-managed schema
```

- **Frontend (React).** There are four pages: Search, Hotel details/booking, Confirmation and Manage booking. All data comes from the REST API through one small `api.js` wrapper, which turns every backend error into a message the user can read. The search and the chosen dates are kept in the URL, so results can be bookmarked or shared. The frontend checks dates and form fields before sending, so the user gets instant feedback, but it never decides prices or availability.
- **Backend (Spring Boot).** The code has three layers:
  - **Controllers** (`web/`) only handle HTTP.
  - **Services** (`service/`) hold all the business rules: date validation, pricing, availability, booking, cancellation and alternatives.
  - **Repositories** (`repository/`) use Spring Data JPA.
  
  DTOs are Java records, so entities never leave the service layer. A single `@RestControllerAdvice` makes every error look the same.
- **Database (MySQL).** Flyway owns the schema (`V1__create_schema.sql`) and the sample data (`V2__seed_hotels.sql`). Hibernate runs with `ddl-auto=none`, so it never changes the schema on its own.
- **Configuration and secrets.** Database credentials come from environment variables or a git-ignored `backend/.env`. There are no passwords in the repository. A `Clock` bean set to Asia/Kolkata decides what "today" means, so the past-date rule doesn't depend on the server's time zone. It also makes the rules easy to test.

## 2. Database design

```
hotels 1 ──── * room_inventory        (one row per hotel per booked night)
hotels 1 ──── * bookings
```

| Table | Key columns | Important constraints |
|---|---|---|
| `hotels` | name, location, description, amenities (comma-separated), room_type, price_per_night, total_rooms | `price_per_night > 0`, `total_rooms > 0`, index on `location` |
| `room_inventory` | hotel_id, stay_date, total_rooms, booked_rooms | **UNIQUE(hotel_id, stay_date)**; **CHECK(0 ≤ booked_rooms ≤ total_rooms)**; FK to hotels |
| `bookings` | reference, hotel_id, guest_name, guest_email, check_in, check_out, nights, price_per_night, total_amount, status, created_at, cancelled_at, version | **UNIQUE(reference)**; CHECK(check_out > check_in); CHECK(status IN …); FK to hotels; index on (hotel_id, check_in, check_out) |

Why the tables look like this:

- **Availability is stored per night.** Each `room_inventory` row says how many rooms are booked on one night. To check a stay, the backend reads only the rows for its nights, instead of scanning every booking for overlaps. A row is created the first time a night is booked, so a missing row means nothing is booked that night.
- **The database is the last safety net.** Even if the application had a bug, the CHECK constraint stops `booked_rooms` from going above `total_rooms`.
- **Bookings store a copy of the price** (`price_per_night`, `total_amount`). If the hotel's price changes later, existing bookings keep the price they were booked at.
- **`version`** is used for optimistic locking on bookings (see §6).
- **The brief allows one room category per hotel,** so `room_type` and `total_rooms` are stored on the hotel itself.

## 3. API design

| Method | Path | Purpose | Success |
|---|---|---|---|
| GET | `/api/hotels` (`location`, `checkIn`, `checkOut` optional) | Browse/search. With dates, each hotel includes `availableRooms`, `nights` and `totalAmount` | 200 |
| GET | `/api/hotels/locations` | Locations for the search box | 200 |
| GET | `/api/hotels/{id}` | Hotel details (stay fields included if dates are given) | 200 |
| GET | `/api/hotels/{id}/availability` | Rooms left and the server-calculated quote, shown on the review step | 200 |
| GET | `/api/hotels/{id}/alternatives` | Surprise feature | 200 |
| POST | `/api/bookings` | Book one room | **201** |
| POST | `/api/bookings/lookup` | Get a booking by reference + email | 200 |
| POST | `/api/bookings/{reference}/cancel` | Cancel (body: email) | 200 |

**Errors** always have the same shape, with a stable `code` the frontend can react to:

```json
{ "timestamp": "...", "status": 409, "error": "Conflict", "code": "NO_AVAILABILITY",
  "message": "Sorry, Tea Valley Cottage is fully booked on the night of 2026-11-20. Please try different dates.",
  "path": "/api/bookings", "fieldErrors": [] }
```

| Status | Used for | Example codes |
|---|---|---|
| 400 | Bad input | `VALIDATION_FAILED` (with `fieldErrors`), `CHECK_IN_IN_PAST`, `INVALID_DATE_RANGE`, `STAY_TOO_LONG`, `INVALID_PARAMETER` |
| 404 | Not found | `HOTEL_NOT_FOUND`, `BOOKING_NOT_FOUND` |
| 409 | Conflicts with the current state | `NO_AVAILABILITY`, `ALREADY_CANCELLED`, `CANCELLATION_CLOSED`, `CONCURRENT_UPDATE` |
| 500 | Unexpected errors | `INTERNAL_ERROR` (logged, generic message) |

Lookup and cancel are **POST with a JSON body** rather than GET with query parameters. That keeps guest emails out of URLs, browser history and server access logs.

## 4. Booking logic

**Dates (`StayDates`).** Both dates are required. Check-in cannot be before today (Asia/Kolkata). Check-out must be after check-in. A stay can be at most 30 nights and can start at most 365 days ahead. These rules run on the backend every time, even though the frontend checks the same things first.

**Overlapping stays.** A stay occupies the nights from check-in up to, but not including, check-out. So a guest leaving on the 5th and another arriving on the 5th never compete for the same room-night, and back-to-back bookings are allowed. Any shared night counts against capacity.

**Availability.** A room is free for a stay only if it is free on **every** night of the stay. So rooms available = `total_rooms − (the most rooms booked on any single night of the stay)`.

**Pricing (`PriceCalculator`).** Total = `price_per_night × nights`, as a `BigDecimal` rounded to 2 decimal places. It is always calculated on the server. The review step shows the quote from `/availability`, and the booking itself calculates the total again; the client never sends a price. Taxes and discounts are not modelled (see assumptions).

**Booking (`BookingService.create`).** Everything below happens in one transaction:
1. Validate the dates.
2. Lock the hotel row (`SELECT … FOR UPDATE`).
3. Read the inventory rows for the stay's nights with a locking read, and fail with `409 NO_AVAILABILITY` if any night is full.
4. Add one booked room to every night.
5. Save the booking with a unique reference.

If anything fails, the whole transaction rolls back, so a failed booking never leaves some nights reserved. A test checks this.

**Booking reference.** The format is `TC` followed by 8 random characters from `SecureRandom`, using an alphabet without 0/O or 1/I so it's easy to read out. It is checked for uniqueness before saving, and a UNIQUE constraint backs this up.

**Cancellation.** The reference and email must both match. A wrong email returns the same 404 as a wrong reference, so nobody can find out whether a reference exists. The booking must still be `CONFIRMED`, and its check-in must be today or later. The service then locks the hotel row, removes one booked room from every night of the stay, and sets the status to `CANCELLED` with a timestamp. The freed room can be booked straight away.

**Assumptions**
- One room per booking, and one room category per hotel (as the brief allows).
- Prices are in INR with no taxes, fees or discounts. Payment is at the hotel.
- Free cancellation is allowed until the check-in date. Once the stay has started it can't be cancelled online.
- "Today" means today in India (Asia/Kolkata), and check-in today is allowed.
- Maximum stay is 30 nights, bookable up to 365 days ahead.
- Emails are compared without regard to case; references are stored and compared in uppercase.
- Cancelled bookings are kept (soft status change), not deleted, so there is a history.

## 5. Design decisions

| Decision | Alternative considered | Why / trade-off |
|---|---|---|
| A per-night `room_inventory` table | Counting overlapping bookings on every request | The brief asks for availability to be stored. Reads are a simple range query, and the CHECK constraint guards against overbooking. The cost: inventory and bookings must be kept in step. They are always updated in the same transaction. |
| Lock the hotel row while booking (pessimistic) | Optimistic retries, or `SERIALIZABLE` isolation | Simple and correct. Bookings for one hotel are processed one at a time, which is fine at this scale. Different hotels never block each other. |
| Inventory rows created on first booking | Pre-filling 365 days of rows per hotel | No scheduled job or pre-fill needed. Safe because the hotel row lock stops two transactions inserting the same night, and the UNIQUE key backs that up. |
| Flyway migrations | `ddl-auto=update` | The schema is versioned, can be reviewed, and runs the same way on every machine. |
| Records for DTOs, entities kept inside services | Returning entities from controllers | No lazy-loading surprises, and the API shape doesn't change when the database changes. |
| Validate on both frontend and backend | Backend only | Instant feedback for the user. The backend stays the source of truth. |
| H2 (MySQL mode) for tests | Testcontainers MySQL | Tests run anywhere with no Docker. The cost: they don't exercise MySQL-specific behaviour such as CHECK constraints or Flyway scripts. |

## 6. Simultaneous bookings: two users want the last room

How it works today:

1. Both requests start a transaction and call `findByIdForUpdate(hotelId)`, which is `SELECT … FOR UPDATE` on the hotel row. Only one gets the lock; the other **waits**.
2. The first request reads the inventory rows with a locking read, sees 1 room left, takes it, and commits. Committing releases the lock.
3. The second request now gets the lock. It reads the inventory with a locking read, which always sees the latest committed data even under MySQL's default REPEATABLE READ. It finds 0 rooms left and returns **`409 NO_AVAILABILITY`**. The UI shows the message, refreshes availability, and offers alternatives.
4. Safety nets: the `CHECK (booked_rooms <= total_rooms)` constraint and `UNIQUE(hotel_id, stay_date)` would reject an overbooking even if the code had a bug. Lock timeouts and deadlocks become `409 CONCURRENT_UPDATE` ("please try again").
5. Two simultaneous **cancellations** of the same booking are stopped by the `@Version` column. The second one fails its version check, and its room release is rolled back along with it, so a room is never released twice.

The test `twoGuestsRacingForTheLastRoomOnlyOneWins` starts two threads at the same moment against a one-room hotel and checks that exactly one succeeds.

What I'd do at larger scale: lock only the inventory rows for the requested nights instead of the whole hotel, so bookings for different dates don't wait for each other. Alternatively, use an atomic conditional update (`UPDATE room_inventory SET booked_rooms = booked_rooms + 1 WHERE … AND booked_rooms < total_rooms`) and check the number of rows updated. For very high traffic, a short-lived "hold" (reserve for 10 minutes while the user fills in the form) would avoid users losing the room on the confirm step.

## 7. Surprise feature

**What it does: "Flexible dates & nearby stays".** When a hotel is sold out for the selected dates, the booking panel shows:
1. **Up to 3 nearby date options at the same hotel**, for the same number of nights and up to ±7 days away. Each shows the rooms left and the total, and the closest options are tried first. Clicking one switches the dates straight into a bookable state.
2. **Up to 3 other hotels in the same location** that have rooms on the original dates, cheapest first, with their totals.

**Why I chose it.** A sold-out message is a dead end: the user has to guess new dates by trial and error. Travel dates are often a little flexible, and a guest who wanted Goa usually cares more about Goa than about one particular hotel. This turns "no" into "here's what you *can* book" with one click. It's small, but it solves a real problem, and it connects directly to the availability rules above.

**How I implemented it.**
- Backend: `GET /api/hotels/{id}/alternatives` → `AlternativesService`. It loads the hotel's inventory for the whole ±7-day window in **one query** into a date-to-booked map, then tries shifts of ±1, ±2, … ±7 days and keeps the first 3 windows where every night has a free room. The same-city hotels are found with a single grouped query (`max(booked_rooms)` per hotel for the stay), then filtered and sorted by price. It never suggests dates in the past.
- Frontend: the `Alternatives` component appears only when `/availability` says sold out. Picking a date option updates the URL dates, which re-checks availability and shows the booking form.
- Tested by `alternativesSuggestNearbyFreeDatesWhenSoldOut`.

## 8. Limitations and improvements

- **No email delivery.** The confirmation is shown on screen only. Next step: send a confirmation email with the reference, and a calendar (.ics) attachment.
- **One room per booking, one room category per hotel.** I would add room types, multiple rooms per booking, and guest counts.
- **The whole hotel is locked during a booking** (see §6). I would move to locking only the needed nights, or to the conditional-update approach.
- **No real hotel images.** The UI shows coloured banners. I would add image URLs or uploads.
- **Tests use H2, not MySQL.** I would add Testcontainers MySQL tests that also run the Flyway migrations, plus frontend tests (Vitest + React Testing Library) and an end-to-end test (Playwright).
- **No pagination** on hotel search. That's fine for the sample data but needed for real volumes.
- **Out of scope per the brief:** authentication, payments, admin panel, rate limiting, and deployment (Docker Compose would be the next step for one-command setup).
