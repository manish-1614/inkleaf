# Phase 2 Context — Architecture Refactor: ViewModel & Single-Pass Data Flow

## Problem Statement
Opening large (1-5 MB) Markdown documents causes hard crashes ("app has stopped" system dialog) and the reader reloads from scratch on configuration changes. Root causes identified in analysis (2026-08-26):

1. **4x full-file reads per open**: `SafDocumentRepository.getDocumentMetadata()` reads the entire file to compute SHA-256 fingerprint (SafDocumentRepository.kt:63). It is called once in `MainActivity` (intent handler / HomeScreen callback), then `ReaderScreen.loadData()` calls `getDocumentMetadata()` AGAIN plus `readDocumentContent()` again.
2. **UI state lives in composables**: `uiState` is `remember {}` in ReaderScreen; `openDocumentUri` is plain `mutableStateOf` in MainActivity. Rotation/process death loses everything and triggers full reload churn.
3. **Memory amplification**: raw markdown string + CommonMark AST + BlockModel list where each block duplicates text in both `.text` and `.runs` fields.
4. **Fingerprint requires whole content in RAM** even though only a hash is needed.

## User Decisions (locked, 2026-08-26)
- **Full refactor approved**: introduce ViewModel layer + restructure data flow. Do NOT keep current structure.
- **Large-file strategy**: standard refactor now; windowed/partial parsing for huge files is out of scope for this phase (files are 1-5 MB).
- **Remote images**: keep Coil remote image loading for now (explicit user exception to zero-network PRD rule). Do not change image loading in this phase.

## Scope
In scope:
- New `ReaderViewModel` (AndroidViewModel) owning document load, UiState, blocks cache keyed by fingerprint
- `SavedStateHandle` for document URI so process death / config change restores without re-read where possible
- Single-pass load: stream SAF input once, computing SHA-256 incrementally during the same read that produces content
- Move metadata+content read into one repository call returning both (e.g., `openDocument(uri): LoadedDocument`)
- Parse on `Dispatchers.Default` inside ViewModel (Phase 1 moved it off main thread in the composable; move ownership to ViewModel)
- Memory diet: release raw markdown after parse; avoid retaining duplicate strings where cheap to do so WITHOUT changing BlockModel public shape used by renderer
- MainActivity/HomeScreen refactored to delegate loading to ViewModel(s); remove duplicate `getDocumentMetadata` calls at open time
- Preserve existing behavior: TOC, search, themes, scroll restore/position save

Out of scope:
- Windowed/chunked parsing of huge files
- WebView pooling/virtualization (Phase 3)
- Syntax highlight caching (Phase 3)
- Search debounce (Phase 3)
- Recents serialization fix (Phase 4)

## Constraints
- AGENTS.md roles apply (Block Kernel & Plugin Engineer patterns)
- No new heavy dependencies; ViewModel + lifecycle-viewmodel-ktx are acceptable additions
- Keep `UiState` sealed-class pattern but relocate it out of the composable layer
- Build verification command: `cmd /c "gradlew.bat compileDebugKotlin --console=plain"`

## Verification Expectations
- Code inspection confirms single SAF read per open action
- Rotation mid-load does not restart loading
- Warm-cache reopen skips re-parse when fingerprint matches
- All existing reader features still work after refactor
