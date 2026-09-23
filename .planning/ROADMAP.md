# Roadmap: Inkleaf Large-Document Stability & Performance

## Overview

Inkleaf (Native Markdown Reader for Android, Kotlin + Jetpack Compose) crashes and freezes on large Markdown files. Root-cause analysis identified: full-document reads repeated 4x per open, main-thread parsing, WebView lifecycle/threading races, uncached syntax highlighting in the scroll hot path, and unbounded long-range scroll animations. Phase 1 (stability hotfixes) is complete. This roadmap covers the remaining durable fixes.

## Phases

- [x] **Phase 1: Stability Hotfixes** - WebView lifecycle/threading guards, off-main-thread parsing, safe clamped scrolling
- [ ] **Phase 2: Architecture Refactor — ViewModel & Single-Pass Data Flow** - Introduce ReaderViewModel with SavedStateHandle, eliminate 4x file reads via streaming fingerprint, parse-once cache
- [ ] **Phase 3: Rendering Performance** - Cache syntax highlighting, WebView virtualization + pooling, search debounce
- [ ] **Phase 4: Hygiene & Regression Hardening** - DataStore write debouncing, recents serialization fix, regression checklist

## Phase Details

### Phase 1: Stability Hotfixes (COMPLETE)
**Goal**: Eliminate hard crashes from WebView destroy races, background-thread state writes, main-thread parsing freezes, and unbounded TOC jumps.
**Depends on**: Nothing (first phase)
**Requirements**: PERF-01, PERF-02
**Success Criteria**:
1. Opening a large (1-5 MB) recent document no longer auto-closes the app
2. TOC navigation jump to bottom of large document completes without crash or freeze
3. `gradlew compileDebugKotlin` passes
**Plans**: Complete 2026-08-26 (direct implementation)

### Phase 2: Architecture Refactor — ViewModel & Single-Pass Data Flow
**Goal**: Restructure reader data flow around a ReaderViewModel owning UiState; read each document exactly once (streaming SHA-256 fingerprint during that single read); parse once and cache by fingerprint; survive configuration changes and process death without re-loading.
**Depends on**: Phase 1
**Requirements**: PERF-03, PERF-04, PERF-05
**Success Criteria** (what must be TRUE):
1. Opening any document reads the SAF stream at most once per open action (verified by code inspection / logging)
2. Rotating the device mid-read does not restart document load; UI state survives
3. Reopening an unchanged document with a warm cache skips re-parse
4. No raw markdown string is retained after parsing completes (memory diet)
5. `gradlew compileDebugKotlin` passes and existing behavior (TOC, search, themes, scroll restore) is preserved
**Plans**: TBD

### Phase 3: Rendering Performance
**Goal**: Remove scroll-hot-path work: remember-cached syntax highlighting, Mermaid/KaTeX WebView virtualization + pooling, debounced off-main-thread search.
**Depends on**: Phase 2
**Requirements**: PERF-06, PERF-07, PERF-08
**Success Criteria**:
1. Flinging through code-heavy documents shows no regex rescans per frame (highlight cached per block+query)
2. Rapid fling past many diagrams does not spawn/destroy WebViews per block beyond pool size
3. Search typing does not scan all blocks on the main thread per keystroke
4. `gradlew compileDebugKotlin` passes
**Plans**: TBD

### Phase 4: Hygiene & Regression Hardening
**Goal**: Debounce DataStore scroll-position writes correctly, fix `|` delimiter corruption in recents serialization (JSON encoding), run full regression checklist.
**Depends on**: Phase 3
**Requirements**: PERF-09, PERF-10
**Success Criteria**:
1. Recents entries containing `|` in URIs/names round-trip intact
2. Scroll position persists accurately after reading session ends
3. Full regression checklist passes: open 5 MB cold, TOC jump to bottom, rapid fling past diagrams, rotate mid-read, process-death restore
**Plans**: TBD
