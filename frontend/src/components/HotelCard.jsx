import { Link } from 'react-router-dom';
import AvailabilityBadge from './AvailabilityBadge.jsx';
import { formatINR, nightsLabel } from '../utils.js';

const BANNERS = [
  'linear-gradient(135deg, #0f766e, #14b8a6)',
  'linear-gradient(135deg, #1d4ed8, #60a5fa)',
  'linear-gradient(135deg, #be185d, #f472b6)',
  'linear-gradient(135deg, #b45309, #fbbf24)',
  'linear-gradient(135deg, #6d28d9, #a78bfa)',
  'linear-gradient(135deg, #15803d, #4ade80)',
  'linear-gradient(135deg, #b91c1c, #f87171)',
];

export function bannerFor(id) {
  return BANNERS[Number(id) % BANNERS.length];
}

export function initials(name) {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join('');
}

export default function HotelCard({ hotel, checkIn, checkOut }) {
  const link = `/hotels/${hotel.id}?checkIn=${checkIn}&checkOut=${checkOut}`;
  const soldOut = hotel.availableRooms === 0;
  const shown = hotel.amenities.slice(0, 4);
  const extra = hotel.amenities.length - shown.length;

  return (
    <article className="hotel-card">
      <div className="hotel-banner" style={{ background: bannerFor(hotel.id) }}>
        <span className="banner-initials">{initials(hotel.name)}</span>
        <span className="banner-location">{hotel.location}</span>
      </div>
      <div className="hotel-body">
        <h3>
          <Link to={link}>{hotel.name}</Link>
        </h3>
        <p className="muted small">
          {hotel.location} · {hotel.roomType}
        </p>
        <p className="description">{hotel.description}</p>
        <ul className="amenities">
          {shown.map((a) => (
            <li key={a}>{a}</li>
          ))}
          {extra > 0 && <li>+{extra} more</li>}
        </ul>
      </div>
      <div className="hotel-footer">
        <div>
          <div className="price">
            {formatINR(hotel.pricePerNight)}
            <span> / night</span>
          </div>
          {hotel.totalAmount != null && (
            <div className="muted small">
              {formatINR(hotel.totalAmount)} for {nightsLabel(hotel.nights)}
            </div>
          )}
        </div>
        <div className="footer-right">
          <AvailabilityBadge rooms={hotel.availableRooms} />
          <Link className={`btn ${soldOut ? 'btn-secondary' : ''}`} to={link}>
            {soldOut ? 'See alternatives' : 'View & book'}
          </Link>
        </div>
      </div>
    </article>
  );
}
