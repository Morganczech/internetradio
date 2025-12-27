Touch Radio v1.1.0 — Privacy & UX Simplification Release

This release removes the optional location feature and simplifies
the “Local stations” behavior to work purely based on the device
region (Locale).

The app no longer requests any location permission and behaves
fully deterministically across devices.

Changes

• Removed LocationService and related code
• Removed ACCESS_COARSE_LOCATION from manifest
• Local stations are now resolved using system Locale only
• Simplified startup and permissions UX
• Notifications permission remains on-demand when starting playback
• Improved offline startup behavior

Technical

• Refactored RadioViewModel (removed hybrid location states)
• Updated AllStationsScreen (removed location banner logic)
• Version bumped to 1.1.0 (code 3)

The project remains feature-complete and maintained in stable mode.


SHA-256 Checksum: 0d897cb0dd36092dab865b4fe92398c4c0afd0bb0b51a6c4a483c239c5e4d6a0
