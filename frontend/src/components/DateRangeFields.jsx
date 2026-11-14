import { addDays, todayISO } from '../utils.js';

/** Check-in / check-out pickers. Moving check-in past check-out pushes check-out to the next day. */
export default function DateRangeFields({ checkIn, checkOut, onChange }) {
  const today = todayISO();

  function changeCheckIn(value) {
    let nextCheckOut = checkOut;
    if (value && (!nextCheckOut || nextCheckOut <= value)) {
      nextCheckOut = addDays(value, 1);
    }
    onChange({ checkIn: value, checkOut: nextCheckOut });
  }

  return (
    <div className="date-fields">
      <label>
        Check-in
        <input type="date" min={today} value={checkIn} onChange={(e) => changeCheckIn(e.target.value)} />
      </label>
      <label>
        Check-out
        <input
          type="date"
          min={checkIn ? addDays(checkIn, 1) : addDays(today, 1)}
          value={checkOut}
          onChange={(e) => onChange({ checkIn, checkOut: e.target.value })}
        />
      </label>
    </div>
  );
}
