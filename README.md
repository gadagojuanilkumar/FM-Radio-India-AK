# FM Radio India (AK)

An Android app for listening to Indian FM radio stations online, with support for stations from other countries too. Built with Kotlin and Jetpack Compose.

## Features

- Stream thousands of live radio stations, with background playback and notification / lock-screen controls
- Browse by country, state and language, or search by station name
- **Near me**: finds stations in your region and the closest ones by distance (asks for location permission)
- **FM Directory**: a list of Akashvani and private FM stations by state, city and frequency, with a switch to show only the ones that can be played online
- Favorites list saved on your phone
- **Pause and resume**: after pausing, Play continues from where you stopped (up to 5 minutes), instead of jumping to live
- **Sleep timer**: stop playing after 15, 30, 45, 60 or 90 minutes
- Tapping the notification opens the app
- Saves station lists and logos on your phone, so it uses less mobile data and still shows stations offline

## Screens

Add screenshots here, for example `docs/home.png` and `docs/now-playing.png`.

## How it works

| Part | File |
|---|---|
| Screens (stations list, Near me, FM Directory, Now Playing) | `MainActivity.kt` |
| Background playback and media notification (Media3 ExoPlayer) | `PlaybackService.kt` |
| Sleep timer | `SleepTimer.kt` |
| Station lists, downloads and on-device cache | `StationRepository.kt` |
| Favorites | `FavoritesStore.kt` |
| Location for "Near me" | `LocationHelper.kt` |

## Data sources

- **Radio Browser** (radio-browser.info): the live, community-maintained station database used for the main station lists
- **`data/fm_directory.json`**: the FM directory of Akashvani and private stations
- **`data/state_fix.json`**: corrects stations that Radio Browser has tagged with the wrong state
- **`data/stations.json`**: your own custom stations. Add entries like this and the app picks them up within about an hour of pushing to `main`:

```json
[
  {
    "id": "my-001",
    "name": "My Station",
    "state": "Telangana",
    "streamUrl": "https://example.com/stream",
    "logoUrl": ""
  }
]
```

The app reads these three files from the `main` branch of this repository, so changes to them go live without releasing a new app version.

## Build it yourself

1. Install the latest stable **Android Studio**.
2. Clone the repo:
   ```
   git clone https://github.com/gadagojuanilkumar/FM-Radio-India-AK.git
   ```
3. Open the folder in Android Studio and wait for Gradle sync to finish.
4. Connect a phone with USB debugging turned on and press **Run**.

Requirements: Android 8.0 (API 26) or newer.

## Release builds (GitHub Actions)

Every push to `main` runs `.github/workflows/build.yml`, which builds a signed APK and a Play Store bundle (AAB) and attaches them to the run as downloads. It needs these repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## Credits

Station data from [Radio Browser](https://www.radio-browser.info). Built with the help of Claude AI.
