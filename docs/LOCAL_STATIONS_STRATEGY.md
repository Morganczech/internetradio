# Local Stations Strategy & Offline Handling

**Goal:** Define the station initialization strategy and offline behavior according to current UX rules. Clarify logic, remove offline fallbacks, and ensure clear user feedback.

## 1. No Seed JSON Files

*   **Removal:** Seed JSON files (`stations_cz.json`, etc.) are not used.
*   **No Local Data:** The app does not have an offline database of pre-filled stations. Initialization is purely online.
*   **Reason:** Simplified maintenance and data freshness.

## 2. Database Initialization ("Local Stations")

Process for loading initial stations on first start (Onboarding):

1.  **Region Determination:**
    *   Strictly based on `Locale.getDefault().country`.
    *   No IP or language guessing.

2.  **Online Only (RadioBrowser API):**
    *   Station list fetch occurs **ONLY if internet is available**.
    *   Calls RadioBrowser API for the specific country code.

3.  **Offline State during Initialization:**
    *   If **no internet** at first launch, the station database remains **EMPTY**.
    *   No random or global fallback stations are loaded.
    *   Flag `favorites_initialized` remains `false` -> initialization retry will happen on next app launch.

## 3. Offline UX – Empty State (Permanent Message)

Displaying a permanent message (e.g., in the center of the screen instead of a list):

*   **Display Condition:**
    1.  **Offline** status detected.
    2.  **AND** user has **NO stations** in the database (DB is empty).

*   **Message Content:**
    *   Text: *"You are not connected to the internet. Stations cannot be loaded or searched without a connection."*
    *   Visual: Offline Icon + Text.

*   **If user has stations:**
    *   If the user has at least one station (loaded previously), this permanent message is **NOT SHOWN**. The station list remains visible.

## 4. Offline Handling – Playback

Behavior when clicking a station in the list:

1.  **Click (Play):**
2.  **Check:** Immediate connectivity check (`isNetworkAvailable`).
3.  **If OFFLINE:**
    *   **STOP:** Playback does **NOT START**.
    *   **No Call:** `RadioService` is not called.
    *   **UI State:** UI remains in "Stopped" state (no loading spinner).
    *   **Feedback:** Show **immediate message** (Snackbar/Toast): *"No internet connection"*.

## 5. Offline Handling – Search

Behavior on the Search screen (`BrowseStationsScreen`):

*   **Offline:** API must not be called.
*   **Behavior:** Immediately display text info: *"Cannot search stations without internet connection"*.
*   **State:** No loading indicator (spinner) running until timeout.

## 6. Flag `favorites_initialized`

*   **TRUE:** Set only when stations are **successfully** downloaded and saved from the API.
*   **FALSE:** Remains false until initialization succeeds (e.g., during persistent offline state). This ensures the app attempts to get the basic station set until successful.

---
**Summary:**
This strategy eliminates "uncertain states". The user either sees data from the internet, cached data (saved stations), or clear information that operation is impossible without internet.
