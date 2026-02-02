App Major Features
i. Secure Biometric Login
Utilizes the androidx.biometric library to provide hardware-level security. Returning users can authenticate using Fingerprint or Face Unlock, ensuring that booking data remains secure.
ii. Smart Venue Discovery (Map Integration)
Integrates the Google Places SDK to allow users to filter and locate sports venues (e.g., "Futsal", "Badminton") near their current location. The map provides real-time data including venue names, ratings, and navigation support.
iii. Digital Booking & QR Ticketing
A paperless booking system where successful reservations generate a unique QR Code using the ZXing Library. This QR code contains encrypted booking details (Venue, Date, Time) and is generated locally on the device to optimize performance.
iv. Admin Verification Scanner
A dedicated feature for venue managers to scan user tickets. It performs a real-time Atomic Transaction in Firebase Firestore to verify the booking status. It instantly detects invalid or previously used tickets to prevent fraud.
v. Social Team Finder
A real-time "Team Feed" where users can host or join games. The system handles concurrency ensuring that once a team slot is filled, it is instantly locked for other users.
