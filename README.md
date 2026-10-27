# TMDB

An Android movie discovery app powered by [The Movie Database (TMDb)](https://www.themoviedb.org/). Search for movies, browse paginated results, and open movie details. Search results and movie data are cached locally with Room.

## Features

- Search TMDb for movies by title.
- Browse results in a scrollable list with poster art and release dates.
- Load additional pages of results.
- View movie details, including poster, release date, rating, and overview.
- Show a built-in poster placeholder when poster artwork is missing or fails to load, and display `N/A` for unavailable descriptions, release dates, or ratings.
- Cache search results and movie details locally so previously loaded data remains available when the network is unavailable.
- Show loading, empty, retryable error, and pagination-error states.
- Use lifecycle-aware Compose state collection and Hilt-based ViewModel injection.

## Screens

### Search

Enter a movie title and submit the search. Blank queries cannot be submitted.

### Search results

View matching titles and release dates. Select a movie to open its detail screen, or select **Load More** to request the next results page. Search results are backed by the local Room database and update as cached results change.

### Movie details

View the selected movie's poster, title, release date, rating, and overview. When a network request fails, the repository attempts to return the cached movie if available.

## Architecture

The app uses a lightweight MVVM and repository structure:

```text
Compose screens
    ↓ user actions / observed UI state
ViewModels
    ↓
MovieRepository interface
    ↓
DefaultMovieRepository
    ├── TMDb API service (Retrofit)
    └── Local database (Room)
```

- **UI:** Jetpack Compose screens render state and forward user actions. Navigation is defined in `ui/navigation`.
- **ViewModels:** `SearchViewModel`, `ResultsViewModel`, and `DetailViewModel` expose state as `StateFlow`. The results and detail ViewModels receive route arguments through `SavedStateHandle`.
- **Repository:** `MovieRepository` describes search, pagination, observation, and detail operations. `DefaultMovieRepository` coordinates the network and local database, maps DTOs and entities, preserves detailed movie data when search summaries are refreshed, and falls back to cached data for failed detail requests.
- **Data:** Retrofit communicates with TMDb; Room stores movies and query-to-result ordering. Hilt provides the API service, database, repository, and ViewModels.
- **Domain:** `domain/model/Movie.kt` provides the movie model used by the UI and repository interface.

## Tech stack

- Kotlin
- Jetpack Compose and Material 3
- AndroidX Navigation Compose
- AndroidX Lifecycle / ViewModel
- Hilt dependency injection
- Retrofit and Gson
- Room
- Coil for image loading
- Gradle version catalog (`gradle/libs.versions.toml`)

## Setup

1. Install Android Studio with the Android SDK required by this project and a compatible JDK. The Gradle daemon toolchain is configured in `gradle/gradle-daemon-jvm.properties`.
2. Obtain a TMDb API key from your TMDb account.
3. Add the key to the root `local.properties` file (this file is ignored by Git):

   ```properties
   TMDB_API_KEY=your_tmdb_api_key
   ```

   Alternatively, set a `TMDB_API_KEY` environment variable or pass `-PTMDB_API_KEY=your_tmdb_api_key` to Gradle.
4. Sync Gradle, then run the `app` configuration or build from the project root:

   ```powershell
   .\gradlew.bat :app:assembleDebug
   ```

Gradle fails early if `TMDB_API_KEY` is missing. **A key placed in an Android app is still recoverable from the built APK.** `local.properties` keeps it out of source control; it does not make the key secret at runtime. For a public production app, proxy TMDb requests through a backend you control and rotate keys that have been exposed.

## Tests

Run the JVM unit tests from the project root:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Tests cover movie mapping, repository search/pagination and cache behavior, and search/results/detail ViewModel state transitions. Repository tests use Robolectric and an in-memory Room database; no live TMDb account or network is required to run tests. Gradle configuration still requires a `TMDB_API_KEY`, so set the environment variable or local property before running Gradle.

## Database schema

Room schemas are exported under `app/schemas`. The database is currently at schema version 2 and includes a migration from version 1.
