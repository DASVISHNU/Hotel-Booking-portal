const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });

export const formatINR = (amount) => inr.format(Number(amount));

export function toISODate(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export const todayISO = () => toISODate(new Date());

export function addDays(iso, days) {
  const [y, m, d] = iso.split('-').map(Number);
  return toISODate(new Date(y, m - 1, d + days));
}

export function formatDate(iso) {
  if (!iso) return '';
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString('en-IN', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

export function formatDateTime(isoDateTime) {
  if (!isoDateTime) return '';
  return new Date(isoDateTime).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
}

/** Same rules as the backend, checked early so the user gets instant feedback. The backend still re-checks. */
export function validateDates(checkIn, checkOut) {
  if (!checkIn || !checkOut) return 'Please select both check-in and check-out dates.';
  if (checkIn < todayISO()) return 'Check-in date cannot be in the past.';
  if (checkOut <= checkIn) return 'Check-out date must be after the check-in date.';
  return null;
}

export function nightsLabel(n) {
  return `${n} night${n === 1 ? '' : 's'}`;
}

export const isValidEmail = (email) => /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email.trim());
