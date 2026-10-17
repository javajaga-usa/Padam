# Padam

Clean, ad-free Tamil movie browsing for the big screen. Lists, search, details, official trailers and songs,
and where each film is streaming legally. No ads, no tracking, no pirate sources.

| Folder | What it is |
| --- | --- |
| [`android-tv/`](android-tv/README.md) | Native Android TV / Google TV app (Kotlin, Jetpack Compose for TV) |
| [`web/`](web/README.md) | For any other smart TV: run a small server on a home computer, open `padam.local` in the TV's browser |

Both use [TMDB](https://www.themoviedb.org) for film data; you need a free TMDB API key.

The Android APK is built by GitHub Actions on every push; add `TMDB_API_KEY` as a repository secret and
download the APK from the workflow run's artifacts.

This product uses the TMDB API but is not endorsed or certified by TMDB.
