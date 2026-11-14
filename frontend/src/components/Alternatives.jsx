import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { formatINR, nightsLabel } from '../utils.js';
import Alert from './Alert.jsx';

function shortDate(iso) {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short' });
}

function shiftLabel(days) {
  const n = Math.abs(days);
  return `${n} day${n === 1 ? '' : 's'} ${days > 0 ? 'later' : 'earlier'}`;
}

/**
 * Surprise feature: when the chosen stay is sold out, suggest the nearest free dates at this hotel
 * and other hotels in the same location that have rooms on the original dates.
 */
export default function Alternatives({ hotelId, location, checkIn, checkOut, onPickDates }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let ignore = false;
    setLoading(true);
    setError(null);
    api
      .alternatives(hotelId, checkIn, checkOut)
      .then((result) => !ignore && setData(result))
      .catch((e) => !ignore && setError(e.message))
      .finally(() => !ignore && setLoading(false));
    return () => {
      ignore = true;
    };
  }, [hotelId, checkIn, checkOut]);

  if (loading) return <p className="muted small">Looking for nearby dates and hotels…</p>;
  if (error) return <Alert>{error}</Alert>;
  if (!data) return null;

  const { alternativeDates, similarHotels } = data;
  const nothing = alternativeDates.length === 0 && similarHotels.length === 0;

  return (
    <section className="alternatives">
      <h4>Flexible dates &amp; nearby stays</h4>
      {nothing && <p className="muted small">No free rooms within a week of these dates or at other hotels in {location}. Please try different dates.</p>}

      {alternativeDates.length > 0 && (
        <>
          <p className="muted small">Same hotel, same {nightsLabel(alternativeDates[0].nights)}, slightly different dates:</p>
          <div className="date-options">
            {alternativeDates.map((o) => (
              <button
                key={o.checkIn}
                type="button"
                className="date-option"
                onClick={() => onPickDates(o.checkIn, o.checkOut)}
              >
                <strong>
                  {shortDate(o.checkIn)} → {shortDate(o.checkOut)}
                </strong>
                <span className="muted small">{shiftLabel(o.daysShifted)}</span>
                <span className="small">
                  {o.availableRooms} left · {formatINR(o.totalAmount)}
                </span>
              </button>
            ))}
          </div>
        </>
      )}

      {similarHotels.length > 0 && (
        <>
          <p className="muted small">Other hotels in {location} with rooms on your dates:</p>
          <ul className="similar-hotels">
            {similarHotels.map((h) => (
              <li key={h.id}>
                <Link to={`/hotels/${h.id}?checkIn=${checkIn}&checkOut=${checkOut}`}>{h.name}</Link>
                <span className="small">
                  {formatINR(h.pricePerNight)}/night · {formatINR(h.totalAmount)} total · {h.availableRooms} left
                </span>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
}
