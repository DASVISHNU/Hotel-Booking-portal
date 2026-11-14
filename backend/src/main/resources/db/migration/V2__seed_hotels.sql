-- Sample data: 7 hotels across 5 locations. Prices in INR.
-- Tea Valley Cottage has only 2 rooms so "sold out" and the alternatives feature are easy to demo.

INSERT INTO hotels (name, location, description, amenities, room_type, price_per_night, total_rooms) VALUES
('Seabreeze Beach Resort', 'Goa',
 'Beachfront resort on Candolim beach with sea-facing rooms, a large lagoon pool and sunset views from the shack-style restaurant.',
 'Free WiFi,Swimming Pool,Beach Access,Breakfast Included,Air Conditioning,Spa',
 'Deluxe Sea View Room', 6500.00, 10),

('Palm Grove Homestay', 'Goa',
 'A quiet Portuguese-style villa in Assagao surrounded by palm trees, ten minutes from Anjuna beach. Ideal for a relaxed stay.',
 'Free WiFi,Garden,Breakfast Included,Free Parking,Bicycle Rental',
 'Garden Room', 3200.00, 5),

('Marine Drive Grand', 'Mumbai',
 'Business-friendly hotel on Marine Drive with views of the Queen''s Necklace, a rooftop lounge and quick access to Nariman Point.',
 'Free WiFi,Gym,Rooftop Restaurant,Airport Shuttle,Air Conditioning,24x7 Room Service',
 'Executive Room', 8900.00, 8),

('Pink City Heritage Haveli', 'Jaipur',
 'A restored 19th-century haveli in the old city with hand-painted frescoes, a courtyard and evening folk music performances.',
 'Free WiFi,Heritage Property,Rooftop Restaurant,Cultural Evenings,Air Conditioning',
 'Heritage Room', 4800.00, 6),

('Amber Fort View Hotel', 'Jaipur',
 'Modern hotel near Amer with views of Amber Fort, a pool and easy day trips to Nahargarh and Jaigarh forts.',
 'Free WiFi,Swimming Pool,Free Parking,Breakfast Included,Travel Desk',
 'Superior Room', 3900.00, 7),

('Garden City Suites', 'Bengaluru',
 'Serviced suites in Indiranagar close to cafes, metro and tech parks. Each suite has a kitchenette and work desk.',
 'Free WiFi,Kitchenette,Work Desk,Gym,Laundry Service,Air Conditioning',
 'Studio Suite', 5200.00, 9),

('Tea Valley Cottage', 'Munnar',
 'A small cottage stay inside a tea estate with misty valley views, guided plantation walks and home-cooked Kerala meals.',
 'Free WiFi,Mountain View,Plantation Tour,Breakfast Included,Bonfire',
 'Valley View Cottage', 4200.00, 2);
