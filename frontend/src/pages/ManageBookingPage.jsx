import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api.js';
import { isValidEmail, todayISO } from '../utils.js';
import BookingDetails from '../components/BookingDetails.jsx';
import Alert from '../components/Alert.jsx';

export default function ManageBookingPage() {
  const [params] = useSearchParams();
  const [form, setForm] = useState({ reference: params.get('ref') || '', email: '' });
  const [fieldErrors, setFieldErrors] = useState({});
  const [booking, setBooking] = useState(null);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);
  const [loading, setLoading] = useState(false);
  const [confirmingCancel, setConfirmingCancel] = useState(false);
  const [cancelling, setCancelling] = useState(false);

  async function lookup(e) {
    e.preventDefault();
    const errors = {};
    if (!form.reference.trim()) errors.reference = 'Booking reference is required.';
    if (!form.email.trim()) errors.email = 'Email is required.';
    else if (!isValidEmail(form.email)) errors.email = 'Please enter a valid email address.';
    setFieldErrors(errors);
    if (Object.keys(errors).length) return;

    setLoading(true);
    setError(null);
    setNotice(null);
    setBooking(null);
    setConfirmingCancel(false);
    try {
      setBooking(await api.lookupBooking(form.reference.trim(), form.email.trim()));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function cancel() {
    setCancelling(true);
    setError(null);
    try {
      const updated = await api.cancelBooking(booking.reference, form.email.trim());
      setBooking(updated);
      setNotice('Your booking has been cancelled and the room has been released.');
    } catch (err) {
      setError(err.message);
    } finally {
      setCancelling(false);
      setConfirmingCancel(false);
    }
  }

  const canCancel = booking && booking.status === 'CONFIRMED' && booking.checkIn >= todayISO();

  return (
    <div className="page narrow">
      <h1>Manage your booking</h1>
      <p className="muted">Enter your booking reference and the email address you booked with.</p>

      <form className="card lookup-form" onSubmit={lookup} noValidate>
        <label>
          Booking reference
          <input
            value={form.reference}
            placeholder="e.g. TC7KQ2M9XA"
            onChange={(e) => setForm({ ...form, reference: e.target.value.toUpperCase() })}
            className={`mono ${fieldErrors.reference ? 'invalid' : ''}`}
          />
          {fieldErrors.reference && <span className="field-error">{fieldErrors.reference}</span>}
        </label>
        <label>
          Email address
          <input
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            className={fieldErrors.email ? 'invalid' : ''}
          />
          {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
        </label>
        <button className="btn" type="submit" disabled={loading}>
          {loading ? 'Finding…' : 'Find booking'}
        </button>
      </form>

      <Alert>{error}</Alert>
      <Alert type="success">{notice}</Alert>

      {booking && (
        <div className="card">
          <BookingDetails booking={booking} />
          {canCancel && !confirmingCancel && (
            <button className="btn btn-danger" type="button" onClick={() => setConfirmingCancel(true)}>
              Cancel booking
            </button>
          )}
          {confirmingCancel && (
            <div className="confirm-cancel">
              <p>Cancel this booking? This can't be undone.</p>
              <div className="button-row">
                <button className="btn btn-secondary" type="button" onClick={() => setConfirmingCancel(false)} disabled={cancelling}>
                  Keep booking
                </button>
                <button className="btn btn-danger" type="button" onClick={cancel} disabled={cancelling}>
                  {cancelling ? 'Cancelling…' : 'Yes, cancel it'}
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
