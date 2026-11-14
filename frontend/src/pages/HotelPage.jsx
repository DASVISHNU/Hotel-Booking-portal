import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { api } from '../api.js';
import { addDays, formatDate, formatINR, isValidEmail, nightsLabel, todayISO, validateDates } from '../utils.js';
import DateRangeFields from '../components/DateRangeFields.jsx';
import AvailabilityBadge from '../components/AvailabilityBadge.jsx';
import Alternatives from '../components/Alternatives.jsx';
import Alert from '../components/Alert.jsx';
import { bannerFor, initials } from '../components/HotelCard.jsx';

export default function HotelPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const checkIn = params.get('checkIn') || addDays(todayISO(), 1);
  const checkOut = params.get('checkOut') || addDays(checkIn, 2);
  const dateError = validateDates(checkIn, checkOut);

  const [hotel, setHotel] = useState(null);
  const [hotelError, setHotelError] = useState(null);

  const [quote, setQuote] = useState(null);
  const [quoteLoading, setQuoteLoading] = useState(false);
  const [quoteError, setQuoteError] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const [guest, setGuest] = useState({ guestName: '', email: '' });
  const [fieldErrors, setFieldErrors] = useState({});
  const [step, setStep] = useState('details'); // 'details' -> 'review'
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);

  useEffect(() => {
    let ignore = false;
    setHotelError(null);
    api
      .getHotel(id)
      .then((h) => !ignore && setHotel(h))
      .catch((e) => !ignore && setHotelError(e.message));
    return () => {
      ignore = true;
    };
  }, [id]);

  // Availability + backend-calculated total for the chosen dates.
  useEffect(() => {
    setStep('details');
    setSubmitError(null);
    if (dateError) {
      setQuote(null);
      return undefined;
    }
    let ignore = false;
    setQuoteLoading(true);
    setQuoteError(null);
    api
      .availability(id, checkIn, checkOut)
      .then((q) => !ignore && setQuote(q))
      .catch((e) => {
        if (!ignore) {
          setQuote(null);
          setQuoteError(e.message);
        }
      })
      .finally(() => !ignore && setQuoteLoading(false));
    return () => {
      ignore = true;
    };
  }, [id, checkIn, checkOut, dateError, refreshKey]);

  function changeDates(dates) {
    setParams({ checkIn: dates.checkIn, checkOut: dates.checkOut }, { replace: true });
  }

  function validateGuest() {
    const errors = {};
    if (!guest.guestName.trim()) errors.guestName = 'Name is required.';
    else if (guest.guestName.trim().length > 100) errors.guestName = 'Name must be at most 100 characters.';
    if (!guest.email.trim()) errors.email = 'Email is required.';
    else if (!isValidEmail(guest.email)) errors.email = 'Please enter a valid email address.';
    return errors;
  }

  function review(e) {
    e.preventDefault();
    const errors = validateGuest();
    setFieldErrors(errors);
    if (Object.keys(errors).length === 0) setStep('review');
  }

  async function confirm() {
    setSubmitting(true);
    setSubmitError(null);
    try {
      const booking = await api.createBooking({
        hotelId: Number(id),
        checkIn,
        checkOut,
        guestName: guest.guestName.trim(),
        email: guest.email.trim(),
      });
      navigate('/booking/confirmation', { state: { booking } });
    } catch (e) {
      setSubmitError(e.message);
      if (e.fieldErrors?.length) {
        setFieldErrors(Object.fromEntries(e.fieldErrors.map((f) => [f.field, f.message])));
        setStep('details');
      }
      if (e.code === 'NO_AVAILABILITY' || e.code === 'CONCURRENT_UPDATE') {
        // Someone took the last room while this user was reviewing: refresh availability.
        setStep('details');
        setRefreshKey((k) => k + 1);
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (hotelError) {
    return (
      <div className="page">
        <Alert>{hotelError}</Alert>
        <Link to="/">← Back to search</Link>
      </div>
    );
  }
  if (!hotel) return <p className="muted page">Loading hotel…</p>;

  const soldOut = quote && !quote.available;

  return (
    <div className="page">
      <Link to={`/?checkIn=${checkIn}&checkOut=${checkOut}`} className="back-link">
        ← Back to results
      </Link>

      <div className="hotel-layout">
        <section className="hotel-info">
          <div className="hotel-banner large" style={{ background: bannerFor(hotel.id) }}>
            <span className="banner-initials">{initials(hotel.name)}</span>
            <span className="banner-location">{hotel.location}</span>
          </div>
          <h1>{hotel.name}</h1>
          <p className="muted">
            {hotel.location} · {hotel.roomType} · {hotel.totalRooms} rooms
          </p>
          <p>{hotel.description}</p>
          <h3>Amenities</h3>
          <ul className="amenities">
            {hotel.amenities.map((a) => (
              <li key={a}>{a}</li>
            ))}
          </ul>
          <p className="price">
            {formatINR(hotel.pricePerNight)}
            <span> / night</span>
          </p>
        </section>

        <aside className="booking-panel">
          <h2>Book a room</h2>
          <DateRangeFields checkIn={checkIn} checkOut={checkOut} onChange={changeDates} />
          <Alert>{dateError}</Alert>
          <Alert>{quoteError}</Alert>
          {quoteLoading && <p className="muted small">Checking availability…</p>}

          {quote && !quoteLoading && (
            <div className="quote">
              <div className="quote-row">
                <span>
                  {formatDate(quote.checkIn)} → {formatDate(quote.checkOut)}
                </span>
                <AvailabilityBadge rooms={quote.availableRooms} />
              </div>
              <div className="quote-row">
                <span>
                  {formatINR(quote.pricePerNight)} × {nightsLabel(quote.nights)}
                </span>
                <strong>{formatINR(quote.totalAmount)}</strong>
              </div>
            </div>
          )}

          {soldOut && (
            <Alternatives
              hotelId={hotel.id}
              location={hotel.location}
              checkIn={checkIn}
              checkOut={checkOut}
              onPickDates={(inDate, outDate) => changeDates({ checkIn: inDate, checkOut: outDate })}
            />
          )}

          {quote && quote.available && !quoteLoading && step === 'details' && (
            <form onSubmit={review} noValidate className="guest-form">
              <label>
                Full name
                <input
                  value={guest.guestName}
                  onChange={(e) => setGuest({ ...guest, guestName: e.target.value })}
                  className={fieldErrors.guestName ? 'invalid' : ''}
                  maxLength={100}
                  autoComplete="name"
                />
                {fieldErrors.guestName && <span className="field-error">{fieldErrors.guestName}</span>}
              </label>
              <label>
                Email address
                <input
                  type="email"
                  value={guest.email}
                  onChange={(e) => setGuest({ ...guest, email: e.target.value })}
                  className={fieldErrors.email ? 'invalid' : ''}
                  maxLength={150}
                  autoComplete="email"
                />
                {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
              </label>
              <Alert>{submitError}</Alert>
              <button className="btn btn-large btn-block" type="submit">
                Review booking
              </button>
            </form>
          )}

          {quote && quote.available && step === 'review' && (
            <div className="review">
              <h3>Review and confirm</h3>
              <dl className="details">
                <dt>Hotel</dt>
                <dd>{hotel.name}</dd>
                <dt>Dates</dt>
                <dd>
                  {formatDate(checkIn)} → {formatDate(checkOut)}
                </dd>
                <dt>Guest</dt>
                <dd>
                  {guest.guestName.trim()} ({guest.email.trim()})
                </dd>
                <dt>Price</dt>
                <dd>
                  {formatINR(quote.pricePerNight)} × {nightsLabel(quote.nights)}
                </dd>
                <dt>Total to pay</dt>
                <dd className="total">{formatINR(quote.totalAmount)}</dd>
              </dl>
              <p className="muted small">Pay at the hotel. Free cancellation until your check-in date.</p>
              <Alert>{submitError}</Alert>
              <div className="button-row">
                <button className="btn btn-secondary" type="button" onClick={() => setStep('details')} disabled={submitting}>
                  Edit details
                </button>
                <button className="btn" type="button" onClick={confirm} disabled={submitting}>
                  {submitting ? 'Booking…' : 'Confirm booking'}
                </button>
              </div>
            </div>
          )}
        </aside>
      </div>
    </div>
  );
}
