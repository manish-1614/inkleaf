# Markdown Reader Rendering Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix table alignment, fast-scroll rendering stability, heading navigation, and misleading code-block labels in the Inkleaf Android Markdown reader.

**Architecture:** Keep the existing native Compose `LazyColumn` reader and block model. Normalize table data before rendering, give each table one shared set of column widths inside a horizontal scroll container, give list items stable identity/content types, navigate by heading block id, and display code labels only for recognized languages.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX LazyColumn/LazyListState, existing Markdown parser and block model, Gradle tests.

## Global Constraints

- Preserve the viewer-first, local-only Android reader behavior.
- Use the existing `BlockModel` and Compose rendering architecture; do not introduce a second Markdown renderer.
- Preserve the existing Copy action for code blocks.
- Keep tables horizontally scrollable when their shared columns cannot fit the viewport.
- Show a code-block language label only for a recognized language; hide it for empty, plain-text, diagram, Mermaid-like, or unknown syntax.

---

### Task 1: Shared-width Markdown table rendering

**Files:**
- Modify: `app/src/main/java/com/inkleaf/app/ui/reader/BlockItemPresenter.kt`
- Test: existing parser/UI test locations if available; otherwise add focused pure Kotlin tests beside the domain tests.

- [x] Write a failing test or pure helper assertion proving that all rows of a table use the same width for each column, including rows with missing cells.
- [x] Run the focused test and confirm it fails for the current independently measured row layout.
- [x] Normalize header/body row lengths to the maximum column count.
- [x] Compute one width per column using all header and body cell content, with sensible minimum and maximum bounds.
- [x] Render header and body rows inside one horizontally scrollable container and apply the same width to each column position.
- [x] Preserve wrapping, borders, theme colors, and existing typography.
- [x] Run the focused test and the relevant Android test task.

### Task 2: Stable and non-white fast scrolling

**Files:**
- Modify: `app/src/main/java/com/inkleaf/app/ui/reader/ReaderScreen.kt`
- Modify: `app/src/main/java/com/inkleaf/app/ui/reader/MermaidWebViewPresenter.kt`
- Modify: `app/src/main/java/com/inkleaf/app/ui/reader/KatexWebViewPresenter.kt`
- Modify: `app/src/main/java/com/inkleaf/app/ui/reader/BlockItemPresenter.kt` if shared block presentation changes are needed.

- [x] Add a regression check for stable block identity/content type in the lazy list.
- [x] Run the check and confirm the current list lacks the required stable identity behavior.
- [x] Supply stable keys from `BlockModel.id` and stable content types for block categories.
- [x] Avoid recreating expensive rendering state during ordinary list scrolling.
- [x] Give asynchronous WebView-backed blocks an opaque themed background and stable placeholder dimensions while content loads.
- [x] Run unit/build checks and inspect the affected Compose code for layout-shift or white-fallback paths.

### Task 3: Direct TOC navigation by heading identity

**Files:**
- Modify: `app/src/main/java/com/inkleaf/app/ui/reader/TocDrawer.kt`
- Modify: `app/src/main/java/com/inkleaf/app/ui/reader/ReaderScreen.kt`
- Modify: `app/src/main/java/com/inkleaf/app/domain/model/BlockModel.kt` only if a stable heading identity is missing.

- [x] Add a regression test for selecting the second heading at a given level and navigating to its exact block index, including an off-screen heading.
- [x] Run the test and confirm the current level-based lookup fails or targets the wrong heading.
- [x] Pass the heading id through the drawer callback rather than only the heading level.
- [x] Build a heading-id-to-lazy-list-index map and scroll directly to the selected item.
- [x] Close the drawer after a successful selection while preserving the current list state.
- [x] Run the focused test and relevant Android checks.

### Task 4: Recognized-only code-block labels

**Files:**
- Modify: `app/src/main/java/com/inkleaf/app/domain/parser/MarkdownBlockParser.kt` if info-string normalization belongs at parse time.
- Modify: `app/src/main/java/com/inkleaf/app/ui/reader/BlockItemPresenter.kt`.
- Test: parser/domain or UI test location matching existing project conventions.

- [x] Add failing tests for recognized labels such as `bash`, and hidden labels for empty, plain-text, Mermaid-like, diagram-like, and unknown info strings.
- [x] Run the tests and confirm the current fallback label behavior fails them.
- [x] Normalize fenced-code info strings and define a small recognized-language allowlist.
- [x] Render the uppercase language badge only when the normalized language is recognized; otherwise omit the badge while retaining code and Copy.
- [x] Run focused tests and the Android build/test task.

### Final verification

- [x] Re-read this plan and verify all four reported behaviors have an implementation and regression check.
- [x] Run the full available Gradle test/build verification.
- [x] Review the changed files for unrelated changes, unstable keys, misleading labels, and theme regressions.
- [x] Report exact commands and results, including any environment limitations.
