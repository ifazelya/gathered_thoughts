# Gathered Thoughts

A local-only Android app that records voice memos, transcribes them on-device, auto-categorizes them with an LLM, and lets you query your memo database in natural language.

## Features

- **Record** voice memos with live on-device transcription (Android `SpeechRecognizer`)
- **Auto-categorize** notes via OpenAI API (`Task`, `Idea`, `Journal`, `Reminder`)
- **Query** your memo database with natural-language questions ("What tasks did I mention this week?")
- **Browse** notes with category filters and keyword search
- **Edit / delete** notes on the detail screen
- **Graceful fallbacks** when mic permission is denied or API calls fail

## Tech Stack

- Kotlin + Jetpack Compose (Material 3)
- Room (local persistence)
- MVVM with `StateFlow` (no DI framework)
- Single-activity + Navigation Compose
- OkHttp for LLM API calls (categorization + query)

## Setup

1. Open the project in **Android Studio** (Ladybug or newer recommended).
2. Copy `local.properties.example` to `local.properties` and set:
   - `sdk.dir` — path to your Android SDK
   - `OPENAI_API_KEY` — your OpenAI API key
3. Run on an emulator or physical device (API 26+).

```properties
sdk.dir=/Users/you/Library/Android/sdk
OPENAI_API_KEY=sk-your-key-here
```

> `local.properties` is gitignored. Never commit your API key.

## Project Structure

```
app/src/main/java/com/gatheredthoughts/voicenotes/
├── data/
│   ├── NoteEntity.kt
│   ├── NoteDao.kt
│   ├── AppDatabase.kt
│   ├── NotesRepository.kt
│   ├── CategorizationRepository.kt   ← auto-title/category on save
│   └── QueryRepository.kt            ← natural-language memo queries
├── ui/
│   ├── record/   RecordScreen, RecordViewModel
│   ├── list/     ListScreen, ListViewModel
│   ├── query/    QueryScreen, QueryViewModel
│   └── detail/   DetailScreen, DetailViewModel
└── MainActivity.kt
```

## Usage

1. **Record Screen** — tap the mic FAB to start/stop recording. On stop, the transcript is categorized, saved to Room, and you navigate to the list.
2. **List Screen** — filter by category, keyword search, or tap ✨ to open **Ask Gathered Thoughts**.
3. **Query Screen** — ask a natural-language question; the LLM searches your memos and returns an answer plus relevant notes.
4. **Detail Screen** — edit title, transcript, or category; save or delete with confirmation.

## Fallbacks

**Categorization:** category → `Journal`, title → first 5 words of transcript.

**Query:** falls back to local keyword matching if the API fails.

The app never blocks on a failed API call.

## Non-Goals

No backend, cloud sync, Hilt, WorkManager, or Play Store deployment — debug builds only.
