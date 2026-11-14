-- One-time MySQL setup. Run as root:   mysql -u root -p < database/setup.sql
-- Pick your own password below and put the same value in backend/.env (DB_PASSWORD).
-- Tables and sample data are created automatically by Flyway when the backend starts
-- (see backend/src/main/resources/db/migration).

CREATE DATABASE IF NOT EXISTS hotel_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'hotel_app'@'localhost' IDENTIFIED BY 'change_me';
GRANT ALL PRIVILEGES ON hotel_booking.* TO 'hotel_app'@'localhost';
FLUSH PRIVILEGES;
