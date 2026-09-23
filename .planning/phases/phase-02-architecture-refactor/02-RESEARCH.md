# Phase 2: Architecture Refactor — ViewModel & Single-Pass Data Flow — Research

**Researched:** 2026-08-26
**Domain:** Android ViewModel architecture, SAF streaming I/O, Compose state hoisting, Kotlin coroutines
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **Full refactor approved**: introduce ViewModel layer + restructure data flow. Do NOT keep current structure.
- **Large-file strategy**: standard refactor now; windowed/partial parsing for huge files is out of scope for this phase (files are 1-5 MB).
- **Remote images**: keep Coil remote image loading for now (explicit user exception to zero-network PRD rule). Do not change image loading in this phase.

### Scope (locked)
- New `ReaderViewModel` (AndroidViewModel) owning document load, UiState, blocks cache keyed by fingerprint
- `SavedStateHandle` for document URI so process death / config change restores without re-read where possible
- Single-pass load: stream SAF input once, computing SHA-256 incrementally during the same read that produces content
- Move metadata+content read into one repository call returning both (e.g., `openDocument(uri): LoadedDocument`)
- Parse on `Dispatchers.Default` inside ViewModel
- Memory diet: release raw markdown after parse; avoid retaining duplicate strings where cheap to do so WITHOUT changing BlockModel public shape used by renderer
- MainActivity/HomeScreen refactored to delegate loading to ViewModel(s); remove duplicate `getDocumentMetadata` calls at open time
- Preserve existing behavior: TOC, search, themes, scroll restore/position save

### Constraints
- AGENTS.md roles apply (Block Kernel & Plugin Engineer patterns)
- No new heavy dependencies; ViewModel + lifecycle-viewmodel-ktx are acceptable additions
- Keep `UiState` sealed-class pattern but relocate it out of the composable layer
- Build verification command: `cmd /c "gradlew.bat compileDebugKotlin --console=plain"`

### Deferred Ideas (OUT OF SCOPE)
- Windowed/chunked parsing of huge files
- WebView pooling/virtualization (Phase 3)
- Syntax highlight caching (Phase 3)
- Search debounce (Phase 3)
- Recents serialization fix (Phase 4)
</user_constraints>

---

## Summary

- **Root cause confirmed**: every document open performs up to 4 full-file reads — `MainActivity` calls `getDocumentMetadata()` (which internally calls `readDocumentContent()`) at MainActivity.kt:40 and again at MainActivity.kt:68, then `ReaderScreen.loadData()` calls `getDocumentMetadata()` (another full internal read) plus `readDocumentContent()` at ReaderScreen.kt:63-64. All four go through `SafDocumentRepository.getDocumentMetadata` → `readDocumentContent` (SafDocumentRepository.kt:63-64, 22-34).
- **Fix shape**: split the repository into a *cheap* cursor-query-only metadata function (for recents bookkeeping — no stream read at all) and one `openDocument(uri)` single-pass streaming read that fills content and computes SHA-256 incrementally in the same loop, returning `LoadedDocument`. One stream read per open action, total.
- **Dependency**: exactly one new artifact — `androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0`, verified present on Google Maven (live maven-metadata.xml fetch this session) and matching the `lifecycle-runtime-ktx:2.7.0` already in build.gradle.kts:53. Instantiation via a `viewModelFactory { initializer { … } }` companion factory using `APPLICATION_KEY`; the bare `viewModel()` default constructor cannot work because repositories need a `Context`.
- **Blocks cache belongs in the ViewModel**, not `ArtifactCache`: ArtifactCache (ArtifactCache.kt) is shaped for render artifacts (Bitmap/Vector/Html/Text) with plugin/renderer/theme composite keys and a 2 MB bitmap-oriented LRU — storing `List<BlockModel>` would require a serialization hack and fights its eviction sizing. A tiny fingerprint-keyed map (cap 2 entries) inside `ReaderViewModel` gives config-change survival for free and satisfies the "warm reopen skips re-parse" criterion.
- **Memory diet**: do NOT touch `BlockModel`'s duplicated `text`/`runs` fields this phase. `.text` has 8 renderer/search/presenter call sites, and converting it to a derived getter silently changes `equals`/`copy` semantics and risks text-vs-runs divergence from the parser. The locked constraint ("WITHOUT changing BlockModel public shape") points squarely at the safe option: never retain the raw markdown string past parse completion.

**Primary recommendation:** Introduce `ReaderViewModel` with a companion `viewModelFactory`, backed by a refactored `SafDocumentRepository` exposing `getDocumentInfo(uri)` (cursor-only, no read) and `openDocument(uri): LoadedDocument?` (single stream pass, incremental SHA-256), an in-VM fingerprint-keyed block cache, and `SavedStateHandle` URI persistence; UI becomes a thin collector.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Document load orchestration | ViewModel (`ReaderViewModel`) | Repository | Load is stateful, must survive config change; VM owns UiState |
| SAF stream read + hashing | Repository (Dispatchers.IO) | — | Pure I/O, no Android UI deps beyond ContentResolver |
| Fingerprint→blocks caching | ViewModel | — | In-memory, lifetime = VM lifetime (survives rotation) |
| URI persistence across process death | ViewModel `SavedStateHandle` | OS Bundle | System-provided mechanism, no navigation library needed |
| Markdown parsing | Domain parser (Dispatchers.Default) | — | CPU-bound; invoked from VM coroutine |
| Scroll position save/restore trigger | UI (list index observation) + ViewModel (debounce + persist) | DataStore | Index originates in `LazyListState` (UI tier); persistence logic moves to VM |
| Theme selection | Repository flow (unchanged) | — | Already correct; collected in MainActivity |

---

## Standard Stack

### Core (additions)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.7.0 | `viewModel()` composable + `ViewModelStoreOwner` integration in Compose | Same release train as `lifecycle-runtime-ktx:2.7.0` already present (build.gradle.kts:53); version verified against Google Maven metadata fetched live this session `[VERIFIED: dl.google.com maven-metadata.xml]` |

Transitively provided by the above (no explicit adds needed): `lifecycle-viewmodel` (provides `ViewModelProvider.Factory`, `CreationExtras`, `APPLICATION_KEY`, `SavedStateHandle`) and `lifecycle-viewmodel-ktx` (provides `viewModelScope`). The CONTEXT constraint names these as acceptable additions `[VERIFIED: CONTEXT.md Constraints]`.

### Optional (recommended, tiny)
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.7.0 | `collectAsStateWithLifecycle()` for StateFlow | Optional; plain `collectAsState()` (already used at MainActivity.kt:32) is acceptable for an activity-scoped, always-resumed screen. Add only if planner wants lifecycle-safety polish. `[ASSUMED]` version pairing — same train, not separately verified |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Companion `viewModelFactory` DSL | Full DI (Hilt/Koin) | Hilt adds kapt/ksp + heavy dependency — violates "no new heavy dependencies" lock. Plain factory is idiomatic for a two-repo app. |
| `rememberSaveable` for URI | ViewModel + SavedStateHandle | rememberSaveable works at Activity level but leaves load ownership in composables — exactly the bug being fixed. SavedStateHandle keeps state with the object that acts on it. |
| `DigestInputStream` wrapper | Manual `MessageDigest.update(buf, 0, n)` per chunk | Equivalent; manual update is more transparent inside a single read loop. Either is fine. |

**Installation (build.gradle.kts dependencies block):**
```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
```

---

## Per-Question Findings

### Q1. ViewModel Integration

**Dependency:** `androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0` — confirmed on Google Maven (versions list includes 2.7.0 stable; latest stable now higher, but pinning to 2.7.0 matches the project's existing lifecycle artifacts and avoids pulling newer transitive Kotlin requirements). `[VERIFIED: Google Maven metadata, fetched 2026-08-26]`

**Instantiation — factory is mandatory.** `viewModel()` with no factory requires a zero-arg `ViewModel` constructor. Both repositories need a `Context` (SafDocumentRepository.kt:20, ReaderPreferencesRepository.kt:27), so a plain `ViewModel()` won't fit. Recommended pattern:

```kotlin
// Source: androidx.lifecycle.viewmodel.viewModelFactory DSL (lifecycle-viewmodel 2.7.0)
class ReaderViewModel(
    private val safRepository: SafDocumentRepository,
    private val preferencesRepository: ReaderPreferencesRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                ReaderViewModel(
                    SafDocumentRepository(app),
                    ReaderPreferencesRepository(app),
                    this.createSavedStateHandle()
                )
            }
        }
    }
}
```

In `MainActivity.setContent`:
```kotlin
val readerViewModel: ReaderViewModel = viewModel(factory = ReaderViewModel.Factory)
```
Scoped to the Activity → survives configuration changes automatically; destroyed on finish. `this.createSavedStateHandle()` (a `CreationExtras` extension in lifecycle-viewmodel 2.7.0+) wires `SavedStateHandle` correctly under the `viewModel()` composable, which supplies `DEFAULT_ARGS_KEY`. `[CITED: developer.android.com/topic/libraries/architecture/viewmodel — factory & SavedStateHandle patterns; ASSUMED exact DSL signatures, verify at compile time]`

Note: `AndroidViewModel` was mentioned in CONTEXT as an option, but injecting `Application` via `APPLICATION_KEY` into a plain `ViewModel` is equivalent and keeps the class testable. Either satisfies the lock.

**Repo construction moves out of `onCreate`** (currently MainActivity.kt:27-28) into the factory, eliminating the double-lifecycle problem where MainActivity holds `lateinit` refs that composables also receive as params (MainActivity.kt:57-58). HomeScreen continues receiving `preferencesRepository` directly for recents/favorites/theme flows — giving HomeScreen its own ViewModel is unnecessary churn for this phase (its operations are fire-and-forget DataStore edits).

### Q2. Single-Pass Read Design

Current waste, traced precisely:

| Call site | What happens | Stream reads |
|-----------|-------------|--------------|
| MainActivity.kt:40 (intent handler) | `getDocumentMetadata(uri)` → internally `readDocumentContent(uri)` (SafDocumentRepository.kt:63-64) | 1 |
| MainActivity.kt:68 (HomeScreen callback) | Same | 1 |
| ReaderScreen.kt:63 | `getDocumentMetadata(documentUri)` again | 1 |
| ReaderScreen.kt:64 | `readDocumentContent(documentUri)` again | 1 |

**Key insight:** MainActivity's two calls only use `metadata.displayName`/`metadata.uriString` for `addRecentDocument` (MainActivity.kt:42, 70) — they throw away the fingerprint and thus the entire file read. Splitting the API fixes this structurally:

```kotlin
// Source: derived from SafDocumentRepository.kt current structure (design sketch)
data class LoadedDocument(
    val metadata: DocumentMetadata,   // displayName, sizeBytes, uriString, fingerprint
    val content: String
)

class SafDocumentRepository(private val context: Context) {

    /** Cursor query ONLY — displayName + SIZE + persist permission. Never opens a stream. */
    suspend fun getDocumentInfo(uri: Uri): DocumentInfo?   // replaces metadata-for-recents use

    /** THE single-pass open. One openInputStream, ever. */
    suspend fun openDocument(uri: Uri): LoadedDocument? = withContext(Dispatchers.IO) {
        // 1. Cursor query (displayName, sizeBytes) — reuse existing code, SafDocumentRepository.kt:40-51
        // 2. takePersistableUriPermission — reuse, SafDocumentRepository.kt:54-61
        // 3. Single streaming read with incremental digest:
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        // capacity hint from cursor SIZE; SIZE can be -1/unknown → fall back to default growth
        val bytesOut = java.io.ByteArrayOutputStream(sizeBytes.coerceAtLeast(0L).toInt())
            context.contentResolver.openInputStream(uri)?.use { input ->
                while (true) {
                    val n = input.read(buffer)
                    if (n == -1) break
                    bytesOut.write(buffer, 0, n)
                    digest.update(buffer, 0, n)      // incremental SHA-256, same read
                }
            } ?: return@withContext null
        val content = String(bytesOut.toByteArray(), Charsets.UTF_8)
        LoadedDocument(DocumentMetadata(/*…fingerprint = hex(digest.digest())*/), content)
    }
}
```

Design notes:
- **`StringBuilder` hint → use `ByteArrayOutputStream(sizeHint)` instead.** Hashing must happen over raw bytes before decoding, so accumulate bytes, not chars. `ByteArrayOutputStream` accepts an initial capacity; the cursor `SIZE` column provides it (may be missing/-1 for some providers → coerce/fallback). This also removes the current line-reader overhead (readLine allocates a String per line; ReaderScreen-scale docs have tens of thousands of lines).
- **Behavioral divergence warning — CRLF.** The current `BufferedReader.readLine()` implementation (SafDocumentRepository.kt:25-31) normalizes `\r\n` → `\n`. Byte-exact accumulation preserves `\r\n`, so fingerprints for CRLF files will differ from all previously stored fingerprints. Consequence: DataStore scroll-position keys (`scroll_offset_$fingerprint`, ReaderPreferencesRepository.kt:33) reset once per affected document. Two options for the planner:
  - **(a)** Accept the reset (fingerprints are ephemeral cache keys; one-time loss). Simplest.
  - **(b)** Preserve parity: hash `content.toByteArray(UTF_8)` after decoding a normalized string — still ONE stream read, costs one extra CPU pass over ≤5 MB (~tens of ms). Only choose if scroll-position continuity is judged important.
- **Error/null semantics preserved:** `openInputStream` returning null (unresolvable URI) → return null → VM emits `UiState.Error`, matching today's nullable-metadata handling documented at ReaderScreen.kt:48.
- **Keep `computeFingerprint`'s hex format** (`%02x`, SafDocumentRepository.kt:81) so any non-CRLF documents keep their existing scroll keys.

### Q3. SavedStateHandle & Where `openDocumentUri` Lives

Current state: `var openDocumentUri by remember { mutableStateOf<Uri?>(null) }` (MainActivity.kt:33) — plain `remember`, so BOTH rotation and process death lose it, and loss triggers HomeScreen + full reload churn.

**Recommendation: move into `ReaderViewModel` via `SavedStateHandle`.**

```kotlin
// In ReaderViewModel init:
val savedUri: String? = savedStateHandle["document_uri"]
private var activeUri: Uri? = savedUri?.let(Uri::parse)

fun openDocument(uri: Uri) {
    savedStateHandle["document_uri"] = uri.toString()   // survives process death
    activeUri = uri
    load()
}
fun closeDocument() {
    savedStateHandle["document_uri"] = null
    activeUri = null
}
```

- Store as **`String`**, not `Uri` — strings are bulletproof in the saved-state Bundle; `Uri` is Parcelable and would likely work, but there's no upside.
- After process death: system recreates Activity → `viewModel()` creates a fresh VM whose `SavedStateHandle` is pre-populated from the OS saved-state bundle → VM sees a non-null URI → auto-runs `load()`. The document content itself is NOT persisted, so a re-read/re-parse after process death is unavoidable and acceptable (CONTEXT says "restores without re-read **where possible**").
- **Config change (rotation):** VM survives, `SavedStateHandle` untouched, no reload at all — satisfies "rotating mid-read does not restart loading."
- **`rememberSaveable` rejected** because it keeps the "which document is open" decision inside the composition instead of the object that performs loading; the refactor's whole point is moving that ownership down.
- **Navigation library: confirmed unnecessary.** The app is single-screen with a ternary composable switch (MainActivity.kt:53-81). Adding Navigation would be net-negative churn. Agree with keeping it out.
- Minor pre-existing gap (do not fix here unless free): `LaunchedEffect(intent)` at MainActivity.kt:36 only handles cold-start ACTION_VIEW; `onNewIntent` while running isn't handled. Out of scope; noted for hygiene backlog.

### Q4. Blocks Cache — ViewModel vs ArtifactCache

**What ArtifactCache offers** (ArtifactCache.kt):
- `RenderArtifact` sealed hierarchy: Bitmap / Vector / Html / Text artifacts (lines 15-24, 65-71, 92-114) — designed for *rendered output* of plugins, not parsed source models.
- Composite keying: `computeCacheKey(sourceText, pluginId, pluginVersion, rendererVersion, themeId, widthBucket, densityBucket)` (lines 30-43).
- Memory LRU capped at **2 MB sized for bitmaps** (line 15) + disk mirror under `cacheDir/inkleaf_artifacts`.

**Verdict: do NOT reuse ArtifactCache for blocks.**
1. Storing `List<BlockModel>` would mean serializing domain models into `TextArtifact` blobs — write/read/parse overhead per open, custom serializer maintenance, and no benefit (in-memory parse result is already available instantly).
2. Its LRU sizeOf treats unknown types as 1 KB (line 21) — a 5 MB document's blocks would thrash/misaccount the bitmap budget.
3. Disk persistence of blocks is useless: after process death you must re-read the file anyway (content isn't cached), so you might as well re-parse from the in-hand content.

**Recommendation: tiny in-ViewModel LRU keyed by fingerprint:**

```kotlin
// In ReaderViewModel — survives rotation, dies with process. Cap 2 entries.
private val blocksCache = object : LinkedHashMap<String, List<BlockModel>>(0, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<BlockModel>>): Boolean =
        size > 2
}
```

Load flow: `openDocument` yields `LoadedDocument` → if `blocksCache[fingerprint]` exists, emit immediately (skip parse — meets success criterion #3: "warm-cache reopen skips re-parse"; the file IS re-read once, which is allowed — the criterion governs parsing, and the file must be touched to learn its current fingerprint). On miss: parse on Default, insert into cache.

### Q5. Memory Diet / BlockModel Duplication

`BlockModel.kt` duplication confirmed: `ParagraphBlock` (lines 25-30), `HeadingBlock` (32-38), `ListItemBlock` (111-122) each carry `text: String` **and** `runs: List<StyledTextRun>` where concatenated run texts ≈ `text` — roughly 2× the string footprint for prose-heavy documents.

**All `.text` consumers on block types (grep-verified):**
| Site | Usage |
|------|-------|
| ReaderScreen.kt:268-273 | `containsText` search helper (`HeadingBlock/ParagraphBlock/ListItemBlock.text`, `TableBlock` cell `.text`) |
| BlockItemPresenter.kt:74, 93, 496 | `renderStyledText(block.runs, block.text, searchQuery, themeMode)` |
| BlockItemPresenter.kt:551 | `renderStyledText(cell.runs, cell.text, …)` (TableCellModel) |
| TocDrawer.kt:102 | `heading.text` display |
| TableLayoutSpec.kt:15-16 | `TableCellModel.text` column-width measurement |
| MarkdownBlockParser.kt:127, 270, 284 | parser-internal construction logic |
| ReaderScreen.kt:80-84 | heading extraction (uses whole block, inherits `.text` indirectly) |

**Recommended (Option A — the locked-compliant choice): change nothing in BlockModel; stop retaining the raw markdown.**
- The locked constraint explicitly says "WITHOUT changing BlockModel public shape used by renderer." Any deduplication of `text`/`runs` alters the data-class surface (constructor params feed `copy()`, `equals`, `hashCode`).
- Concretely: `UiState.Success` holds `(LoadedDocument.metadata, List<BlockModel>)` — **not** the `content` string. Once `parseToBlocks(content)` returns, the only strong reference to the raw string is the local parameter, which goes out of scope. Do not store `LoadedDocument.content` in any field/state. This alone drops peak-retained memory by the full raw-markdown size (1–5 MB) immediately after parse, which is what success criterion #4 asks for ("No raw markdown string is retained after parsing completes").

**Rejected (Option B): derived `val text get() = runs.joinToString("") { it.text }`.** Syntactically invisible at call sites, but (a) removes constructor params → breaks `copy(text=…)`/positional construction in the parser, (b) silently excludes `text` from `equals`/`hashCode`/`toString`, and (c) bets that parser-produced `runs` always concatenate to `text` — unverified (parser builds them via separate code paths, e.g., MarkdownBlockParser.kt:44-52). Savings don't justify risk in this phase. Record as a possible Phase 4 hygiene item if memory profiling demands it.

Secondary diet win: the parser currently constructs `MarkdownBlockParser()` (and therefore a new CommonMark `Parser` with extension list) **per keystroke-free call** at ReaderScreen.kt:68 — hoisting one `MarkdownBlockParser` instance into the ViewModel removes repeated builder work and lets the CommonMark `Parser` be reused.

### Q6. Threading & Scroll Flows

**Parsing dispatcher:** Confirmed pattern — keep `withContext(Dispatchers.Default)` around `parseToBlocks`, but now launched from `viewModelScope` inside the VM instead of `rememberCoroutineScope` in the composable (moves ReaderScreen.kt:67-69 verbatim). `viewModelScope` dispatches on `Main.immediate`; the repository already forces `Dispatchers.IO` internally (SafDocumentRepository.kt:22, and the new `openDocument` must too), so the VM body stays on Main except the explicit `Default` block. Rationale comments at ReaderScreen.kt:65-66 (watchdog/main-thread freeze) remain accurate and should migrate with the code.

**CommonMark `Parser` thread-safety:** The org.commonmark `Parser` produced by `Parser.builder().build()` is immutable after construction and its `parse()` creates fresh per-call parser state, so a single instance is safely reusable across sequential (even concurrent) parses. `[ASSUMED — HIGH confidence from training knowledge of commonmark-java design; verify against vendored sources if paranoid]`. Even if wrong, the blast radius is nil: parses are serialized per-document within one VM coroutine, and falling back to per-call instantiation (today's behavior) is a one-line change. Low-risk item.

**Scroll restore migration (ReaderScreen.kt:86-98):**
- Restore target computation moves INTO the VM: after load succeeds, VM reads `preferencesRepository.getScrollPosition(fingerprint).first()` and exposes `pendingScrollIndex: Int?` alongside blocks in `UiState.Success`.
- The actual `listState.scrollToItem(index)` STAYS in the UI (LazyListState is composition-owned) via a `LaunchedEffect(pendingScrollIndex)` that consumes-and-clears (`vm.onScrollRestored()`).
- This makes restore survive rotation: today a rotation recreates the composable and re-fires the effect (benign but wasteful); after refactor, rotation re-collects the same VM state and the pending value is already consumed → no double-scroll.

**Scroll save migration (ReaderScreen.kt:100-106):**
- Replace the `LaunchedEffect(firstVisibleItemIndex)` + `delay(400)` + `rememberCoroutineScope` chain with `snapshotFlow { listState.firstVisibleItemIndex }.collect { vm.onScrollIndexChanged(it) }` inside a `LaunchedEffect(listState)`.
- VM implements the 400 ms trailing debounce with its own Job cancellation and persists via `preferencesRepository.saveScrollPosition(...)` in `viewModelScope` — same swallow-on-contention behavior as ReaderPreferencesRepository.kt:128-139. Proper DataStore write debouncing correctness review is explicitly Phase 4 (PERF-09) — do not redesign here, just relocate.

**DataStore contention:** Preferences DataStore serializes `edit` calls internally (single-writer actor); concurrent edits queue rather than corrupt. Existing try/catch in `saveScrollPosition` (ReaderPreferencesRepository.kt:129-138) covers the remaining failure mode (IO). No new hazard introduced by relocation. `[ASSUMED: DataStore single-actor edit semantics — well-documented behavior, not re-verified this session]`

### Q7. Risks & Gotchas (consolidated)

See dedicated section below.

---

## Recommended Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│ MainActivity (thin shell)                                           │
│  setContent {                                                       │
│    ReaderViewModel via viewModel(factory = Factory)                 │
│    themeMode ← prefs.themeFlow.collectAsState (unchanged)           │
│    uiOpen ← readerViewModel.uiOpen.collectAsState  (Boolean/Uri?)   │
│    if (uiOpen) ReaderScreen(vm, themeMode, onBack = vm::closeDoc)   │
│    else HomeScreen(onSelect = vm::openDocument, …)                  │
│  }                                                                  │
└──────┬──────────────────────────────────────────────────────────────┘
       │ collects StateFlow, forwards events
       ▼
┌─────────────────────────────────────────────────────────────────────┐
│ ReaderViewModel (activity-scoped, survives rotation)                │
│  state: StateFlow<ReaderUiState>                                    │
│    ReaderUiState = Closed | Loading | Error(msg)                    │
│                  | Ready(metadata, blocks, pendingScrollIndex)      │
│  SavedStateHandle["document_uri"]          ← process-death restore  │
│  blocksCache: LRU<fingerprint, List<BlockModel>> (cap 2)            │
│                                                                     │
│  openDocument(uri):                                                 │
│    handle[uri]= ; state=Loading                                     │
│    loaded = saf.openDocument(uri)        ── IO, ONE stream pass     │
│       (bytes→BAOS w/ SIZE hint, digest.update per chunk)            │
│    meta→prefs.addRecentDocument (fire-and-forget)                   │
│    blocks = blocksCache[fp] ?: withContext(Default){ parser.parse } │
│    savedOffset = prefs.getScrollPosition(fp).first()                │
│    state=Ready(meta, blocks, savedOffset) ; content released        │
│  onScrollIndexChanged(i) → 400ms debounce → prefs.saveScrollPos     │
│  closeDocument(): handle=null ; state=Closed                        │
└──────┬───────────────────────────┬──────────────────────────────────┘
       │                           │
       ▼                           ▼
┌──────────────────────┐   ┌─────────────────────────────────────────┐
│ SafDocumentRepository│   │ ReaderPreferencesRepository (unchanged) │
│  getDocumentInfo()   │   │  themeFlow / recents / favorites        │
│   cursor ONLY        │   │  getScrollPosition(fp): Flow<Int>       │
│  openDocument()      │   │  saveScrollPosition(fp, i, null)        │
│   1× openInputStream │   └─────────────────────────────────────────┘
│   +incremental SHA256│
│   → LoadedDocument?  │
└──────────────────────┘

ReaderScreen (UI only):
  collects vm.state; renders Loading/Error/LazyColumn(blocks)
  LaunchedEffect(pendingScrollIndex) → scrollToItem → vm.consumeScrollTarget()
  snapshotFlow { firstVisibleItemIndex } → vm.onScrollIndexChanged(it)
```

**Primary data flow (open action):** HomeScreen tap / ACTION_VIEW intent → `vm.openDocument(uri)` → SavedStateHandle write → single SAF stream pass (content + fingerprint simultaneously) → recents entry via cursor-only metadata → fingerprint cache lookup → parse-on-miss on Default → saved scroll offset fetched → `Ready` emitted → UI renders + restores scroll. Total stream opens: **1**.

### Project structure delta
```
app/src/main/java/com/inkleaf/app/
├── ui/
│   ├── MainActivity.kt              # slimmed: viewModel() wiring, theme collect, screen switch
│   └── reader/
│       ├── ReaderScreen.kt          # stateless presenter; UiState class REMOVED from here
│       └── ReaderUiState.kt         # relocated sealed class (per CONTEXT constraint)
├── viewmodel/                       # NEW package (or ui/reader/ — planner's call)
│   └── ReaderViewModel.kt           # VM + companion Factory + blocks cache
├── domain/model/BlockModel.kt       # UNCHANGED (locked)
├── domain/parser/MarkdownBlockParser.kt  # unchanged; instantiated ONCE by VM
└── data/saf/SafDocumentRepository.kt     # getDocumentInfo() added; openDocument() replaces pair
```

---

## Dependency Additions (exact coordinates)

```kotlin
// app/build.gradle.kts — dependencies block
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
```

That is the complete list. `viewModelScope`, `SavedStateHandle`, `viewModelFactory`/`initializer`/`createSavedStateHandle` arrive transitively via `lifecycle-viewmodel`/`lifecycle-viewmodel-ktx` 2.7.0 pulled by that artifact. No other additions permitted under the "no new heavy dependencies" lock.

---

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| androidx.lifecycle:lifecycle-viewmodel-compose | Google Maven | First release 2021 (2.4.0); official AndroidX | Official AndroidX release train | android/platform/frameworks/support (AOSP) | n/a (first-party, not npm/PyPI) | Approved — existence of 2.7.0 verified via live maven-metadata.xml fetch |

**Packages removed due to slopcheck [SLOP] verdict:** none (slopcheck is npm/PyPI-oriented; sole candidate is first-party AndroidX verified directly against Google's official Maven).
**Packages flagged as suspicious [SUS]:** none.

---

## Risks & Mitigations

| # | Risk | Severity | Likelihood | Mitigation |
|---|------|----------|------------|------------|
| R1 | **Fingerprint drift for CRLF files**: byte-exact read (new) vs `readLine()` normalization (old) changes hashes → one-time loss of persisted scroll positions keyed by old fingerprints | Low | Medium (only CRLF-authored files) | Decide between accept-reset (simplest) vs post-decode-content hashing for parity (still single stream read, ~tens of ms extra CPU). Planner decision point; document choice in plan. |
| R2 | **Rotation mid-load restart myth**: if load were triggered from composable `LaunchedEffect` keyed on VM creation, a race could double-fire | Med | Low | Load triggered exclusively from `openDocument()`/init-on-restored-uri inside VM; guard with `state is Loading` check or a `loadJob?.isActive` guard before relaunch. |
| R3 | **Derived-text refactor breaking renderer** | High | n/a | Eliminated by decision: BlockModel unchanged this phase (see Q5 Option A). |
| R4 | **CommonMark Parser reuse assumption** wrong → corrupted ASTs under concurrency | Med | Very Low | Parses are sequential within one VM; worst case revert to per-call `MarkdownBlockParser()` (today's pattern). Flagged `[ASSUMED]`. |
| R5 | **DataStore write storm** from scroll-index emissions during fling (pre-existing, now routed through VM) | Low | Medium | Preserve the existing 400 ms trailing-debounce relocation verbatim; real fix is Phase 4 PERF-09 — do not redesign now. |
| R6 | **SavedStateHandle restore loops**: restoring URI after process death triggers load; if load fails, user stuck in Error with no way Home | Low | Low | Error UI retains Back-to-Home affordance: `closeDocument()` clears handle + state → HomeScreen, mirroring today's `onBack`. |
| R7 | **`createSavedStateHandle()` misuse** — calling outside proper extras throws at runtime, not compile | Low | Low | Use it strictly inside `initializer {}` under `viewModel()` (which supplies `DEFAULT_ARGS_KEY`). Compile check catches signature errors; runtime path exercised in manual test matrix. |
| R8 | **Recents double-write regression**: today MainActivity writes recents twice per open path; naive refactor could drop the write entirely | Low | Medium | Plan must include recents write inside `openDocument()` flow (fire-and-forget in viewModelScope) using cursor-only `getDocumentInfo()` — never the full read. Explicit task acceptance criterion. |

---

## Validation Architecture

> Note: `.planning/config.json` sets `workflow.nyquist_validation: false` — no automated test-framework table required. Verification below follows the CONTEXT.md "Verification Expectations" and ROADMAP success criteria, which are inspection- and manual-UAT-based.

### Compile gate
```bash
cmd /c "gradlew.bat compileDebugKotlin --console=plain"
```
Run after every task commit. `gradlew.bat` confirmed present at repo root.

### Single-read verification (success criterion #1)
Add a temporary debug counter in `SafDocumentRepository`:
```kotlin
private var streamOpenCount = 0   // debug only — remove or guard with BuildConfig.DEBUG
companion object { var debugStreamOpens = 0 }  // or Log.d("SAFRead", "stream opened")
```
Log inside `openDocument()` right before `openInputStream`. Manual procedure:
1. Install debug build, enable Logcat filter `SAFRead`.
2. Open a 1–5 MB doc from HomeScreen → expect exactly **1** log line.
3. Open via external ACTION_VIEW (open-with from a file manager) → expect exactly **1** log line.
4. Rotate device mid-load and mid-read → expect **no additional** log lines (criterion #2).
5. Press Back, reopen same unchanged document → expect **1** log line (single re-read is expected; verify via debug toast/log that parse was SKIPPED — e.g., temporary `Log.d("ParseSkip", "cache hit $fingerprint")`) → criterion #3.
6. Enable Developer Options → "Don't keep activities", background + foreground app → VM recreated, 1 stream read, content restored → process-death path of criterion #2.

### Memory diet verification (criterion #4)
Code inspection: grep confirms `content`/`LoadedDocument.content` is never assigned to a VM field or UiState property; only passed as parse argument. Optional: Android Studio Memory Profiler — capture heap after opening 5 MB doc, search for retained `String` of doc length; expect absent.

### Behavior preservation checklist (criterion #5)
Manual pass on a large GFM doc with headings, tables, code fences, mermaid/math placeholders:
- TOC drawer opens, headings listed, jump works (short animate, long instant)
- Search finds matches, next/previous cycles and scrolls
- Light/Dark/Sepia themes switch live
- Scroll away >400 ms, kill app, relaunch, reopen → position restored
- Refresh toolbar button reloads document
- Error path: open a stale/expired SAF URI → friendly error + Retry + Back-to-home

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `viewModelFactory {}` / `initializer {}` / `createSavedStateHandle()` DSL signatures as sketched (lifecycle-viewmodel 2.7.0) | Q1, R7 | Compile error → trivially fixable at execution; fallback is hand-written `ViewModelProvider.Factory` |
| A2 | CommonMark `Parser` instance is thread-safe/reusable after build | Q6, R4 | Corrupted parses under concurrent parse — mitigated: parses sequential in VM; revert path trivial |
| A3 | Preferences DataStore `edit` serializes concurrent writers (single actor) | Q6 | Contention crash — already swallowed by existing try/catch; no regression either way |
| A4 | `collectAsStateWithLifecycle` exists in `lifecycle-runtime-compose:2.7.0` | Standard Stack (optional) | None — item is optional; `collectAsState` fallback already in use |
| A5 | Cursor `SIZE` column usable as capacity hint; may be missing/-1 on some providers | Q2 | Only a perf hint; fallback growth path covers it |

## Open Questions

1. **Fingerprint parity policy (R1)** — accept one-time scroll-key reset for CRLF docs, or hash normalized content for continuity?
   - Recommendation: accept reset (option a); simpler code, self-heals after one session. Needs planner/user sign-off since it touches persisted data.
2. **Where `ReaderViewModel` lives** — new `viewmodel/` package vs `ui/reader/`. Cosmetic; planner's discretion.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| gradlew.bat + JDK 17 + Android SDK | Compile gate | ✓ (Phase 1 compiled clean 2026-08-26 per ROADMAP) | SDK API 34 / build-tools 34 | — |

No missing dependencies. No external services involved.

---

## Sources

### Primary (HIGH confidence)
- Codebase inspection (all file:line references above) — MainActivity.kt, ReaderScreen.kt, SafDocumentRepository.kt, ReaderPreferencesRepository.kt, BlockModel.kt, ArtifactCache.kt, MarkdownBlockParser.kt, build.gradle.kts
- Google Maven `lifecycle-viewmodel-compose` maven-metadata.xml — fetched live 2026-08-26; 2.7.0 stable confirmed
- CONTEXT.md (locked decisions), ROADMAP.md (Phase 2 success criteria), AGENTS.md (conventions)

### Secondary (MEDIUM confidence)
- developer.android.com ViewModel/SavedStateHandle guidance — standard factory patterns `[ASSUMED exact DSL shapes]`

### Tertiary (LOW confidence)
- commonmark-java thread-safety characteristics — training knowledge, tagged `[ASSUMED]`

## Metadata

**Confidence breakdown:**
- Dependency/version: HIGH — verified against Google Maven live
- Architecture: HIGH — direct codebase trace of every read path; refactor is mechanical
- Pitfalls: MEDIUM-HIGH — CRLF fingerprint drift and DSL signatures are the only soft spots, both flagged with fallbacks

**Research date:** 2026-08-26
**Valid until:** ~2026-09-25 (stable stack; AndroidX pinned versions)

## RESEARCH COMPLETE