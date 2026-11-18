# WESAL / وصال

This repository is the starting point for the WESAL Android social app.

## Planned features
- User profiles and follow relationships
- Posts, likes, and comments
- Direct messaging
- Verification requests and reporting

## Important implementation note
Username-and-password sign-in without email or phone requires a secure authentication service on the server. Do not store plain-text passwords in the Android app or database. The Supabase publishable key may be used in the client; service-role keys must remain server-side and must never be committed to this repository.

This file records the project scope; it does not mean the app or backend is complete.