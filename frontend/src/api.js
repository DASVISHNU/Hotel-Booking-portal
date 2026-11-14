// Thin wrapper over fetch. Every backend error has the same JSON shape
// ({ code, message, fieldErrors }), so the UI can show it directly.

export class ApiError extends Error {
  constructor(message, status, code, fieldErrors = []) {
    super(message);
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`/api${path}`, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    });
  } catch {
    throw new ApiError(
      'Cannot reach the server. Please check that the backend is running and try again.',
      0,
      'NETWORK_ERROR'
    );
  }

  const text = await response.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }

  if (!response.ok) {
    throw new ApiError(
      data?.message || `Request failed (${response.status}). Please try again.`,
      response.status,
      data?.code || 'UNKNOWN_ERROR',
      data?.fieldErrors || []
    );
  }
  return data;
}

function query(params) {
  const entries = Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '');
  const qs = new URLSearchParams(entries).toString();
  return qs ? `?${qs}` : '';
}

export const api = {
  locations: () => request('/hotels/locations'),
  searchHotels: ({ location, checkIn, checkOut }) => request(`/hotels${query({ location, checkIn, checkOut })}`),
  getHotel: (id, { checkIn, checkOut } = {}) => request(`/hotels/${id}${query({ checkIn, checkOut })}`),
  availability: (id, checkIn, checkOut) => request(`/hotels/${id}/availability${query({ checkIn, checkOut })}`),
  alternatives: (id, checkIn, checkOut) => request(`/hotels/${id}/alternatives${query({ checkIn, checkOut })}`),
  createBooking: (body) => request('/bookings', { method: 'POST', body: JSON.stringify(body) }),
  lookupBooking: (reference, email) =>
    request('/bookings/lookup', { method: 'POST', body: JSON.stringify({ reference, email }) }),
  cancelBooking: (reference, email) =>
    request(`/bookings/${encodeURIComponent(reference)}/cancel`, {
      method: 'POST',
      body: JSON.stringify({ email }),
    }),
};
