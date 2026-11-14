import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api.js';
import { addDays, nightsLabel, todayISO, validateDates } from '../utils.js';
import DateRangeFields from '../components/DateRangeFields.jsx';
import HotelCard from '../components/HotelCard.jsx';
import Alert from '../components/Alert.jsx';

export default function SearchPage() {
  const [params, setParams] = useSearchParams();

  // The URL is the source of truth for the applied search, so results can be bookmarked and shared.
  const applied = {
    location: params.get('location') || '',
    checkIn: params.get('checkIn') || addDays(todayISO(), 1),
    checkOut: params.get('checkOut') || addDays(todayISO(), 3),
  };

  const [form, setForm] = useState(applied);
  const [formError, setFormError] = useState(null);
  const [locations, setLocations] = useState([]);
  const [hotels, setHotels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.locations().then(setLocations).catch(() => setLocations([]));
  }, []);

  useEffect(() => {
    const dateError = validateDates(applied.checkIn, applied.checkOut);
    if (dateError) {
      setError(dateError);
      setHotels([]);
      setLoading(false);
      return undefined;
    }
    let ignore = false;
    setLoading(true);
    setError(null);
    api
      .searchHotels(applied)
      .then((result) => !ignore && setHotels(result))
      .catch((e) => !ignore && setError(e.message))
      .finally(() => !ignore && setLoading(false));
    return () => {
      ignore = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [applied.location, applied.checkIn, applied.checkOut]);

  function submit(e) {
    e.preventDefault();
    const dateError = validateDates(form.checkIn, form.checkOut);
    setFormError(dateError);
    if (dateError) return;
    const next = { checkIn: form.checkIn, checkOut: form.checkOut };
    if (form.location.trim()) next.location = form.location.trim();
    setParams(next);
  }

  const nights = Math.round((new Date(applied.checkOut) - new Date(applied.checkIn)) / 86400000);

  return (
    <>
      <section className="hero">
        <h1>Find your next stay</h1>
        <p>Search by city, pick your dates, and book a room in under a minute.</p>
        <form className="search-form" onSubmit={submit} noValidate>
          <label className="location-field">
            Location
            <input
              list="locations"
              placeholder="Any location — e.g. Goa"
              value={form.location}
              onChange={(e) => setForm({ ...form, location: e.target.value })}
            />
            <datalist id="locations">
              {locations.map((l) => (
                <option key={l} value={l} />
              ))}
            </datalist>
          </label>
          <DateRangeFields
            checkIn={form.checkIn}
            checkOut={form.checkOut}
            onChange={(dates) => setForm({ ...form, ...dates })}
          />
          <button className="btn btn-large" type="submit">
            Search
          </button>
        </form>
        <Alert>{formError}</Alert>
      </section>

      <section>
        <div className="results-header">
          <h2>
            {applied.location ? `Hotels in “${applied.location}”` : 'All hotels'}
          </h2>
          {!error && !loading && (
            <span className="muted">
              {hotels.length} result{hotels.length === 1 ? '' : 's'} · {nightsLabel(nights)}
            </span>
          )}
        </div>

        <Alert>{error}</Alert>
        {loading && <p className="muted">Loading hotels…</p>}
        {!loading && !error && hotels.length === 0 && (
          <p className="empty">
            No hotels match “{applied.location}”. Try another location or clear the search.
          </p>
        )}
        <div className="hotel-grid">
          {!loading &&
            hotels.map((h) => (
              <HotelCard key={h.id} hotel={h} checkIn={applied.checkIn} checkOut={applied.checkOut} />
            ))}
        </div>
      </section>
    </>
  );
}
