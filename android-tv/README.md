# Padam — Tamil movies for Android TV

A clean, ad-free Android TV app for browsing Tamil films. Built with Kotlin and Jetpack Compose for TV.

## What it does

- **Home**: a full-screen backdrop that follows the focused film, with rows for In theatres, Streaming on your apps, Popular, Top rated, Coming soon, genres and Classics.
- **Search**: Tamil-only results as you type.
- **Details**: synopsis, director, music director, cast, rating, runtime, plus:
  - **Play trailer** and every official trailer, song and clip, opened in the YouTube app.
  - **Watch on ...** buttons for the OTT apps that hold the film in your region (from TMDB / JustWatch data). Pressing one opens that app, or its Play Store page if it isn't installed.
  - More like this.
- **My List** and **Recently viewed** rows, saved on the TV.
- **Actor pages**: select anyone in the cast to see their biography and Tamil films.
- **Free on YouTube** (optional): when a rights holder has put the full film on YouTube, a button plays it.
  Only videos YouTube marks as licensed content, over an hour long, whose title names the film are shown.
- No ads, no tracking, no analytics, no accounts.

Padam never streams video itself. Playback goes to apps that hold the rights, so nothing breaks when a pirate site goes down and nothing exposes you to their pop-ups or malware.

## Install on the TV

Every push to main publishes the newest build at one fixed link:

    https://github.com/javajaga-usa/Padam/releases/download/tv-latest/padam.apk

On the TV, install the free **Downloader** app (by AFTVnews), type that link, and choose Install.
Allow "Install unknown apps" for Downloader when asked. Later updates install over the old one and
keep My List. If you installed a build from before this link existed, uninstall it once first.

## Setup

1. Get a free TMDB key at https://www.themoviedb.org/settings/api (the v3 API key or the v4 read access token both work).
2. Put it in `local.properties` at the project root (this file is git-ignored):

   ```
   sdk.dir=/path/to/Android/sdk
   TMDB_API_KEY=your_key_here
   # optional, enables "Free on YouTube" (YouTube Data API v3 key from console.cloud.google.com)
   YOUTUBE_API_KEY=your_youtube_key
   # optional
   TMDB_REGION=IN
   TMDB_LANGUAGE=en-IN
   ```

   Environment variables with the same names also work, which is how CI passes the key.
   If the APK was built without a key, the app asks for one on first launch and keeps it on the TV.
3. Build and install:

   ```
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

Requires JDK 17+, Android SDK 35. Runs on Android TV 8.0 (API 26) and later.

## CI

`.github/workflows/android-tv.yml` (at the repo root) builds the debug APK on every push. Add `TMDB_API_KEY` as a repository secret and download the APK from the workflow run's artifacts.

## Layout

```
app/src/main/java/com/padam/tv/
  MainActivity.kt        navigation (home, search, movie/{id})
  PadamApp.kt            service locator
  data/                  TMDB API, models, repository with in-memory cache
  ui/home, ui/search, ui/details, ui/components, ui/theme
  util/Launchers.kt      YouTube and OTT app hand-off
```

This product uses the TMDB API but is not endorsed or certified by TMDB.
