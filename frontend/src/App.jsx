import { Link, NavLink, Route, Routes } from 'react-router-dom';
import SearchPage from './pages/SearchPage.jsx';
import HotelPage from './pages/HotelPage.jsx';
import ConfirmationPage from './pages/ConfirmationPage.jsx';
import ManageBookingPage from './pages/ManageBookingPage.jsx';

export default function App() {
  return (
    <>
      <header className="site-header">
        <div className="container header-inner">
          <Link to="/" className="brand">
            Stay<span>Easy</span>
          </Link>
          <nav>
            <NavLink to="/" end>Find hotels</NavLink>
            <NavLink to="/manage-booking">Manage booking</NavLink>
          </nav>
        </div>
      </header>

      <main className="container">
        <Routes>
          <Route path="/" element={<SearchPage />} />
          <Route path="/hotels/:id" element={<HotelPage />} />
          <Route path="/booking/confirmation" element={<ConfirmationPage />} />
          <Route path="/manage-booking" element={<ManageBookingPage />} />
          <Route path="*" element={<p className="empty">Page not found. <Link to="/">Go to search</Link></p>} />
        </Routes>
      </main>

      <footer className="site-footer">
        <div className="container">Hotel Booking Portal — demo project</div>
      </footer>
    </>
  );
}
