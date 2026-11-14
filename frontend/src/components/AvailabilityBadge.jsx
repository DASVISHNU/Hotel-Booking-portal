export default function AvailabilityBadge({ rooms }) {
  if (rooms === null || rooms === undefined) return null;
  if (rooms === 0) return <span className="badge badge-red">Sold out</span>;
  if (rooms <= 2) return <span className="badge badge-amber">Only {rooms} left</span>;
  return <span className="badge badge-green">{rooms} rooms available</span>;
}
