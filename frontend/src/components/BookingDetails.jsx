import { formatDate, formatDateTime, formatINR, nightsLabel } from '../utils.js';

export default function BookingDetails({ booking }) {
  const cancelled = booking.status === 'CANCELLED';
  return (
    <dl className="details">
      <dt>Reference</dt>
      <dd className="mono">{booking.reference}</dd>
      <dt>Status</dt>
      <dd>
        <span className={`badge ${cancelled ? 'badge-red' : 'badge-green'}`}>{cancelled ? 'Cancelled' : 'Confirmed'}</span>
      </dd>
      <dt>Hotel</dt>
      <dd>
        {booking.hotelName}, {booking.hotelLocation}
      </dd>
      <dt>Guest</dt>
      <dd>
        {booking.guestName} ({booking.email})
      </dd>
      <dt>Check-in</dt>
      <dd>{formatDate(booking.checkIn)}</dd>
      <dt>Check-out</dt>
      <dd>{formatDate(booking.checkOut)}</dd>
      <dt>Stay</dt>
      <dd>
        {nightsLabel(booking.nights)} × {formatINR(booking.pricePerNight)}
      </dd>
      <dt>Total</dt>
      <dd className="total">{formatINR(booking.totalAmount)}</dd>
      <dt>Booked on</dt>
      <dd>{formatDateTime(booking.createdAt)}</dd>
      {cancelled && (
        <>
          <dt>Cancelled on</dt>
          <dd>{formatDateTime(booking.cancelledAt)}</dd>
        </>
      )}
    </dl>
  );
}
