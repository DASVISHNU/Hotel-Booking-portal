import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import BookingDetails from '../components/BookingDetails.jsx';

export default function ConfirmationPage() {
  const { state } = useLocation();
  const booking = state?.booking;
  const [copied, setCopied] = useState(false);

  if (!booking) {
    return (
      <div className="page narrow">
        <p className="empty">
          No booking to show here. You can look up an existing booking on the <Link to="/manage-booking">Manage booking</Link> page.
        </p>
      </div>
    );
  }

  async function copyReference() {
    try {
      await navigator.clipboard.writeText(booking.reference);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  }

  return (
    <div className="page narrow">
      <div className="card confirmation">
        <div className="success-icon" aria-hidden="true">✓</div>
        <h1>Booking confirmed</h1>
        <p className="muted">Keep this reference. You'll need it with your email to view or cancel the booking.</p>
        <div className="reference-box">
          <span className="mono">{booking.reference}</span>
          <button type="button" className="btn btn-secondary btn-small" onClick={copyReference}>
            {copied ? 'Copied' : 'Copy'}
          </button>
        </div>
        <BookingDetails booking={booking} />
        <div className="button-row">
          <Link className="btn btn-secondary" to="/">
            Book another stay
          </Link>
          <Link className="btn" to={`/manage-booking?ref=${booking.reference}`}>
            Manage this booking
          </Link>
        </div>
      </div>
    </div>
  );
}
