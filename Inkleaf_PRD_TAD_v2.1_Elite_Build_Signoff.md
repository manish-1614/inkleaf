# INKLEAF
## A Native, Plugin-Extensible Markdown Reader for Android

**PRODUCT REQUIREMENTS DOCUMENT + TECHNICAL ARCHITECTURE DOCUMENT**

**PRD v2.1 · TAD v2.1 · ELITE BUILD-SIGNOFF SPECIFICATION**  
Prepared for: Manish Prajapati  
Date: 11 August 2026  
Status: Revised Build-Signoff Candidate

> **North Star:** Inkleaf is the best way to read serious Markdown on Android: fast first paint, beautiful native reading, smooth long-document navigation, reliable diagrams/math/media, graceful failure, accessibility, and privacy by architecture.

---

# 0. Document Control

| Item | Value |
|---|---|
| Document | Inkleaf PRD/TAD v2.1 |
| Supersedes | Inkleaf PRD/TAD v2.0 |
| Primary platform | Android |
| Primary UI | Kotlin + Jetpack Compose |
| Rendering model | Block-oriented hybrid renderer: native first, plugin renderer second, isolated WebView when justified |
| Data model | Local-only; SAF URIs + DataStore + bounded local caches |
| Network posture | No document-originated network access in v1.0 |
| Release model | **v1.0 Product GA → Public Release Hardening → Public Listing** |
| v1.0 Product GA target | 12-week feature-complete solo-build target |
| Public hardening target | Additional 4 weeks, subject to actual benchmark/security findings |
| Status | Build-signoff candidate |

## 0.1 Version History

| Version | Change |
|---|---|
| v1.0 | Founding PRD/TAD: product thesis, feature set, hybrid rendering, plugin concept, roadmap. |
| v2.0 | Expanded into build-signoff specification with four pillars, measurable UX/architecture requirements, quality gates, resource governance, security, accessibility, and testing. |
| **v2.1** | **Execution correction:** separates Product GA from Public Release Hardening; removes premature locale cache dimension; adds sepia/paper theme and in-document reading progress; moves Mermaid feasibility validation to the front of Phase 0; makes renderer strategy conditional on measured Mermaid coverage; reduces GA scope to a realistic solo-build boundary. |

## 0.2 Four Pillars

| Pillar | Build-signoff principle | Outcome |
|---|---|---|
| **I — Elite Reader UX** | Reading must feel native, calm, predictable, and effortless. | Fast navigation, comfortable typography, progress awareness, search, TOC, selection, zoom, sharing, resume reading, themes, and accessible interaction. |
| **II — Block Rendering Kernel** | No single renderer owns the document. | Parse once, represent stable blocks, render each block with the most appropriate engine. |
| **III — Plugin & Artifact Architecture** | Plugins are isolated rendering capabilities, not arbitrary UI owners. | Mermaid, KaTeX, SVG and future renderers are replaceable, cacheable, testable, and failure-isolated. |
| **IV — Quantified Quality** | Quality is measured progressively rather than allowed to become an undefined release tail. | GA has practical acceptance gates; public hardening has deeper benchmark/security/store gates. |

## 0.3 Critical Execution Decision

**The project is no longer defined as “12 weeks to satisfy every release-readiness requirement.”**

Instead:

```text
Weeks 1–12
    ↓
v1.0 Product GA
    Feature-complete, stable core reader
    Gates 0–5

Weeks 13–16
    ↓
Public Release Hardening
    Deep performance, adversarial security, compatibility,
    store/privacy/licensing and final release evidence
    Gates 6–8

After hardening
    ↓
Public Play Store listing
```

This is deliberate scope control, not a reduction in product ambition.

---

# PART A — PRODUCT REQUIREMENTS DOCUMENT

## A1. Product Vision

Inkleaf is a viewer-first Android application for reading local Markdown documents with a premium, native-feeling experience. It is not an editor, note-taking suite, cloud-sync product, or general-purpose converter.

The differentiator is the combination of:

- native reading ergonomics
- fast first paint
- stable layout
- long-document performance
- rich technical rendering
- graceful failure
- accessibility
- privacy
- Android-native interaction

## A2. Product Principles

| ID | Principle | Implication |
|---|---|---|
| P-01 | Reading comes first | No editor chrome or authoring workflow in v1.0. |
| P-02 | Content never waits unnecessarily | Native prose paints first; rich blocks enhance progressively. |
| P-03 | Failure never destroys readability | Every renderer has a visible fallback. |
| P-04 | Native whenever it materially improves UX | WebView is used selectively, not dogmatically avoided. |
| P-05 | Privacy is architectural | No silent network, telemetry SDK, or broad storage permission. |
| P-06 | State follows the reader | Recent, favorite, reading position, and progress persist locally. |
| P-07 | Android-native interaction | Back, selection, accessibility, lifecycle, sharing, and gestures follow platform expectations. |
| P-08 | Scope must be releasable | Feature scope and quality gates are explicitly separated. |

## A3. Goals

1. Deliver a premium Markdown reading experience on Android.
2. Support CommonMark + GFM through an explicit compatibility contract.
3. Render Mermaid, KaTeX/LaTeX, and SVG entirely on-device.
4. Keep ordinary prose native and rich rendering isolated.
5. Provide search, TOC, internal navigation, selection, copy, zoom, sharing, favorites, recents, resume reading, themes, and reading progress.
6. Operate without broad storage permission and without mandatory network access.
7. Make failure recovery, accessibility, and low-memory behavior part of the product.
8. Keep v1.0 implementable by a solo developer without turning hardening into an endless blocker.

## A4. Non-Goals

- Markdown editing/authoring.
- Accounts/cloud sync/collaboration.
- Plugin marketplace or arbitrary third-party plugin execution.
- PDF editor/converter in v1.0.
- General-purpose document management suite.
- Automatic remote resource loading.
- Full Obsidian replacement.

## A5. MVP / v1.0 Product GA Scope

### MUST — Product GA

- SAF document picker for `.md` / `.markdown`.
- Android open-with/share intent handling.
- User-selected folder browsing where supported by SAF.
- Recent documents.
- Favorites.
- Resume reading / last position.
- Visible in-document reading progress.
- CommonMark + defined GFM subset.
- Headings, paragraphs, emphasis, links, nested lists, task lists, tables, blockquotes, fenced code, syntax highlighting, footnotes, images, horizontal rules.
- Callouts/admonitions with documented syntax.
- Table of contents and heading navigation.
- In-document search with count, next/previous, highlight, and scroll-to-match.
- Light theme.
- Dark theme.
- **Sepia/Paper reading theme.**
- System theme selection.
- Dynamic font scaling.
- Code copy and horizontal scrolling.
- Image/diagram zoom and pan.
- Internal anchors.
- Relative local Markdown links when SAF scope permits.
- Native/sanitized SVG rendering.
- Mermaid plugin using the renderer strategy selected by Phase 0 validation.
- KaTeX/LaTeX plugin using offline bundled assets.
- Graceful renderer failure and source/text fallback.
- No document-originated network access.
- Core TalkBack semantics for headings, links, controls, images, and rich-block fallbacks.

### HARDENING — Not required to declare Product GA

- Full seven-corpus benchmark sweep across all five device tiers.
- Adversarial security corpus expansion and final security sign-off.
- Exhaustive SAF provider matrix.
- Final Play Store privacy/licensing/store evidence.
- Deep performance regression analysis and optimization beyond GA thresholds.
- Full accessibility audit beyond the core reader flow.
- Extended compatibility and malformed-input corpus.
- Closed-beta feedback fixes discovered after the GA candidate.

### v1.x / Post-GA

- Tabs / multi-document sessions.
- Custom theme packs.
- Share rendered diagrams.
- PDF export.
- Obsidian-flavored compatibility layer.
- Cross-document browsing across a user-authorized directory.
- PlantUML/Graphviz if justified.
- TTS/read-aloud.
- Home-screen widget.
- Explicit future network mode.
- External plugins after a separate security model.

---

# A6. Elite Reader UX

## A6.1 Home

- Primary CTA: **Open Markdown**.
- Recent documents after first use.
- Favorites as a dedicated section.
- Missing files are visibly marked and removable.
- No account or cloud requirement.

## A6.2 Document Screen

- Content remains the dominant surface.
- Toolbar can collapse while reading and return on upward scroll/tap.
- Title follows a deterministic filename/first-heading rule.
- TOC, Search, Appearance, Share and More remain reachable.
- Back closes transient UI before leaving the document.

## A6.3 Typography & Layout

- Coherent type scale for body, H1–H6, captions, code, tables, callouts.
- Android font/display scaling respected.
- Comfortable maximum content width on wide screens.
- Wide tables and code may escape into horizontally scrollable regions.
- Avoid unnecessary layout shifts after first paint.

## A6.4 Reading Themes

v1.0 Product GA includes three explicit reader surfaces:

| Theme | Intent |
|---|---|
| **Light** | Clean daytime/general reading. |
| **Dark** | Low-light and system-dark environments. |
| **Sepia / Paper** | Long-form reading comfort; warm, paper-like presentation. |

Theme requirements:

- Theme changes must not corrupt cached artifacts.
- Code blocks and diagrams receive theme-aware styling where supported.
- Contrast remains readable.
- Theme choice persists locally.
- System theme can automatically choose Light/Dark; Sepia remains an explicit reader choice.

## A6.5 In-Document Reading Progress

The document screen must provide a **visible reading-progress indicator** without consuming significant content space.

Default behavior:

- Thin progress indicator associated with the document viewport.
- Progress is based on the reader's current logical scroll position.
- It should update smoothly without causing layout work.
- The indicator is not the sole representation of reading state; the app may also show a percentage or current heading in an optional compact reader affordance.
- Progress must restore consistently when the reader position is restored.

Progress must not be implemented as a continuously recomputed full-document layout operation.

## A6.6 Reader Interaction Contract

| Action | Required behavior |
|---|---|
| Tap link | Internal anchor/local document or supported external destination according to policy. |
| Long press text | Native selection/copy/share behavior. |
| Tap TOC heading | Navigate to heading and close navigator. |
| Search next/previous | Scroll to and expose the selected match. |
| Pinch image/diagram | Zoom around focal point. |
| Double tap media | Sensible zoom/reset behavior. |
| Tap code copy | Copy exact source, excluding UI chrome. |
| Back from search/TOC/media | Close transient layer. |
| Back from document | Return Home. |

## A6.7 Reader State

Persist:

- favorite
- recent-open timestamp
- document fingerprint
- last reading position
- last-read heading where resolvable
- selected reader theme
- font scale/preferences

If content changes materially, stale offsets must not be blindly restored.

---

# A7. Accessibility

### Product GA

Core reader must provide:

- TalkBack semantic headings.
- Accessible links and controls.
- Image alt-text handling.
- Accessible rich-block fallback text where possible.
- Touch targets appropriate for Android interaction.
- No meaning conveyed only by color.
- Readable light/dark/sepia themes.
- Large-font behavior without clipping.
- Reduced-motion respect.

### Public Hardening

The hardening pass expands this into:

- broader TalkBack navigation audit
- table semantics
- complex diagram accessibility review
- additional font/display scale testing
- accessibility regression matrix across device classes

---

# A8. Markdown Compatibility Contract

| Feature | Product GA | Rule |
|---|---|---|
| CommonMark core | MUST | Required corpus passes without crash and preserves semantics. |
| GFM tables | MUST | Wide tables remain readable; horizontal scroll permitted. |
| GFM task lists | MUST | Read-only checked/unchecked representation. |
| Fenced code | MUST | Exact source preserved for copy. |
| Footnotes | MUST | Reference ↔ footnote navigation. |
| Images | MUST | Local relative assets + explicit broken state. |
| Raw HTML | MUST DEFINE | Safe supported subset only; unsupported/unsafe content degrades safely. |
| Callouts | MUST | Documented syntax and native styling. |
| Unknown fenced language | MUST | Plain readable code fallback. |
| Mermaid | MUST | Plugin route selected by Phase 0 renderer validation. |
| KaTeX | MUST | Offline plugin route. |
| SVG | MUST | Sanitized plugin route. |
| YAML frontmatter | v1.x | Deferred until after GA unless required by real corpus. |
| Wiki links/embeds | v1.x | Deferred compatibility layer. |

---

# A9. Privacy & Network Policy

- No document-originated network access in v1.0.
- No analytics SDK.
- No advertising SDK.
- No automatic upload.
- SAF instead of broad storage permissions.
- WebView islands cannot access network.
- Remote resources remain a future explicit mode.

---

# A10. Success Metrics

## Product GA thresholds

| Metric | GA target |
|---|---|
| Cold-start | P50 target < 400 ms on reference midrange |
| First usable content | P50 < 500 ms; P95 < 1,000 ms |
| Core scroll | No sustained jank on normal corpus |
| Crash-free sessions | ≥ 99.5% once stabilized |
| App size | < 25 MB target; documented exception allowed |
| Core accessibility | No known P0/P1 issue |
| Core compatibility | Required GA corpus passes |

## Public hardening thresholds

The deeper benchmark suite from Part B is required before public listing, not before the 12-week Product GA milestone.

---

# PART B — TECHNICAL ARCHITECTURE DOCUMENT

## B1. Architecture Decision

Inkleaf uses a block-oriented rendering kernel:

```text
Markdown Source
      ↓
Parser
      ↓
Normalized AST
      ↓
Stable Block Model
      ↓
Renderer Selection
      ↓
Native / Plugin / WebView
      ↓
Render Artifact
      ↓
Cache
      ↓
Compose Reader
```

No renderer owns the entire document.

## B2. Renderer Strategy — Phase 0 Decision Gate

### Mandatory Phase 0 Mermaid validation spike

Before downstream Mermaid architecture is considered locked, evaluate the candidate native Mermaid renderer against a representative corpus.

The spike must measure:

1. Diagram type coverage.
2. Parse success rate.
3. Visual fidelity.
4. Rendering time.
5. memory behavior.
6. theme support.
7. zoom/pan behavior.
8. accessibility/fallback capability.

### Decision rule

```text
Native Mermaid passes agreed coverage/fidelity/performance threshold
        ↓
Native = preferred renderer
WebView = fallback

Native Mermaid does not meet threshold
        ↓
WebView = primary Mermaid renderer
Native = optional renderer for verified subsets
```

**Important:** the architecture does not assume that native Mermaid is always the common path.

This removes the risk of building scheduling, caching, and UX assumptions around an unverified renderer.

## B3. Mermaid Validation Acceptance Threshold

For the Phase 0 decision, the native candidate must meet all of the following:

- ≥ 90% parse success across the project target corpus.
- No critical diagram category completely unsupported.
- Visual output acceptable against predefined golden references.
- No unacceptable memory/ANR behavior on the reference midrange device.
- Renderer can be cancelled or safely abandoned.
- Failure routes cleanly to the WebView/source fallback.

If the threshold is missed, the architecture automatically adopts WebView as the common Mermaid path rather than treating that as an architectural failure.

## B4. Logical Architecture

```text
Presentation
 ├── Home
 ├── Document
 ├── Reader Toolbar
 ├── Search
 ├── TOC
 ├── Media Viewer
 └── Settings

Document Domain
 ├── DocumentController
 ├── ReaderStateStore
 ├── LinkResolver
 └── DocumentFingerprint

Rendering Kernel
 ├── MarkdownParser
 ├── NormalizedAst
 ├── BlockModel
 ├── RendererSelector
 ├── NativeBlockRenderer
 ├── PluginRegistry
 ├── RenderScheduler
 ├── RenderArtifact
 └── ArtifactPresenter

Plugins
 ├── SVG
 ├── Mermaid
 └── KaTeX

Data
 ├── SAF Repository
 ├── Artifact Cache
 ├── DataStore
 └── Diagnostics

Platform
 ├── Intents
 ├── SAF
 ├── Isolated WebView
 └── Lifecycle / Accessibility
```

## B5. Renderer Selection

Renderer selection is based on:

- block type
- renderer availability
- renderer capability
- content complexity
- theme
- width constraints
- resource pressure
- cached artifact availability

Example:

```text
Block
  ↓
Can native renderer handle it?
  ├─ yes → native
  └─ no
       ↓
Can registered plugin handle it?
  ├─ yes → plugin
  └─ no
       ↓
Is WebView renderer required/allowed?
  ├─ yes → isolated WebView
  └─ no → raw source fallback
```

## B6. Block Model

```kotlin
sealed interface BlockModel {
    val id: BlockId
    val sourceRange: SourceRange
}

data class ParagraphBlock(...)
data class HeadingBlock(...)
data class CodeBlock(...)
data class TableBlock(...)
data class ImageBlock(...)
data class BlockQuoteBlock(...)
data class CalloutBlock(...)
data class MermaidBlock(...)
data class MathBlock(...)
data class SvgBlock(...)
data class HorizontalRuleBlock(...)
data class RawFallbackBlock(...)
```

UI must not depend directly on parser-specific AST classes.

## B7. Render Artifact Model

```kotlin
sealed interface RenderArtifact {
    val rendererId: String
    val rendererVersion: String
    val intrinsicWidth: Int?
    val intrinsicHeight: Int?
}

data class BitmapArtifact(...)
data class VectorArtifact(...)
data class HtmlArtifact(...)
data class TextArtifact(...)
data class ErrorArtifact(...)
```

> Plugins produce artifacts. Android Views are presenters, not cache entries.

## B8. Plugin Contract

```kotlin
interface RenderPlugin {
    val id: String
    val version: String
    val languageTags: Set<String>
    val capabilities: Set<PluginCapability>

    suspend fun detect(input: PluginInput): DetectionResult

    suspend fun render(
        input: PluginInput,
        context: RenderContext
    ): RenderArtifact
}
```

Rules:

- deterministic
- cancellable
- timeout-bound
- theme-aware where applicable
- width-aware where applicable
- failure-isolated
- cacheable where safe
- no global UI mutation

## B9. WebView Island Contract

WebView is an implementation mechanism, not a document architecture.

Requirements:

- lazy creation
- bundled assets only
- network disabled
- minimal JS bridge
- no unrestricted JavaScript interface
- debugging disabled in release
- hard render timeout
- cancellation
- teardown/recycling after use
- failure becomes `ErrorArtifact` or source fallback

## B10. SVG Security

- Sanitize scripts and event handlers.
- Reject/neutralize external resource references.
- Reject unsafe embedded/foreign content.
- Bound node count.
- Bound dimensions and decoded memory.
- Malformed SVG must never crash the reader.

## B11. Cache Architecture

### Cache key — Product GA

```text
SHA-256(
    sourceHash
  + pluginId
  + pluginVersion
  + rendererVersion
  + themeId
  + typographyProfile
  + widthBucket
  + densityBucket
)
```

**Locale is intentionally excluded from v1.0.**

Localization is not a v1.0 product requirement, and introducing locale into the artifact key would add invalidation and disk-churn complexity without a current product benefit.

If localization later affects rendered artifacts, `locale` can be added as a versioned cache-key dimension in the localization milestone.

### Cache requirements

- bounded in-memory LRU
- bounded disk LRU
- atomic writes
- versioned namespace
- corrupted artifacts deleted/recomputed
- renderer/theme/typography changes invalidate incompatible artifacts

## B12. Render Scheduling

Scheduling is renderer-cost aware.

```text
LOW COST
native prose / headings / lists
        ↓
MEDIUM COST
images / SVG / code highlighting
        ↓
HIGH COST
Mermaid / KaTeX / WebView
```

Rules:

- Native content gets first priority.
- Plugin work is viewport-aware.
- Initial concurrency target: 2–3 jobs, subject to benchmark adjustment.
- High-cost WebView jobs receive stricter concurrency limits.
- Off-screen artifacts can be evicted from memory.
- Cancellation propagates when work is no longer useful.
- Scheduling policy must not assume native Mermaid is always the common case.

## B13. Resource Governance

| Resource | GA policy |
|---|---|
| Markdown size | Tested safe range; controlled warning above range |
| Parser nesting | Bounded |
| SVG nodes | Bounded |
| Image dimensions | Bounded/downsampled |
| Plugin source | Bounded per block |
| Concurrent plugin jobs | Globally bounded |
| WebView instances | Strictly bounded |
| Memory cache | Bounded LRU |
| Disk cache | Bounded LRU |
| Render timeout | Plugin-specific + global ceiling |

## B14. Low-Memory Mode

When memory pressure is detected:

- prioritize native prose
- reduce rich-block concurrency
- render rich blocks near viewport only
- evict bitmap artifacts aggressively
- minimize WebView retention
- allow rich rendering to be disabled for the document
- never make the underlying Markdown unreadable

---

# B15. Testing Strategy — Two-Tier Release Model

## Product GA test suite

The GA suite is intentionally smaller:

1. Core CommonMark/GFM corpus.
2. Core plugin corpus.
3. Core renderer golden tests.
4. Core lifecycle tests.
5. Core accessibility flow.
6. Core SAF flow.
7. Basic memory/resource tests.
8. Basic network-isolation verification.

## Public Hardening suite

The full v2.0 quality suite is required here:

- seven benchmark corpora
- five device tiers
- adversarial Markdown
- malicious SVG
- malicious/complex Mermaid
- malformed math
- cache corruption
- SAF provider matrix
- Macrobenchmark regression analysis
- full accessibility audit
- final network/security verification

This preserves the quality bar while preventing hardening work from blocking the core product build.

---

# B16. Benchmark Corpus

The full public-release corpus remains:

| Corpus | Representative load |
|---|---|
| C1 — Normal Markdown | 1–5k lines |
| C2 — Developer Documentation | 10k+ lines, code/tables/anchors |
| C3 — Diagram Heavy | 50–100 Mermaid blocks |
| C4 — Math Heavy | Hundreds of equations |
| C5 — Asset Heavy | Many local SVG/images |
| C6 — Adversarial | Malformed Markdown, nesting, SVG, plugin inputs |
| C7 — Obsidian-adjacent | Frontmatter, callouts, wiki links, embeds |

These are **hardening evidence**, not prerequisites for starting core development.

---

# B17. Device Matrix

Public hardening target:

- Low-end / ~4 GB RAM
- Reference midrange
- Modern flagship
- Tablet / large width
- Landscape / foldable

Product GA requires:

- reference midrange
- one lower-memory device
- one modern flagship/emulator baseline

The remaining device classes are completed during hardening.

---

# B18. Security & Threat Model

| Threat | GA expectation | Hardening expectation |
|---|---|---|
| Malicious Markdown | Basic parser/resource limits | Fuzz/adversarial corpus |
| Malicious SVG | Sanitization | Expanded malicious SVG suite |
| Mermaid/KaTeX abuse | Timeout + network isolation | Adversarial renderer corpus |
| Tracking image | Network disabled | Instrumented verification |
| Hostile SAF URI | Safe abstraction | Provider matrix |
| Corrupt cache | Recompute safely | Corruption injection |
| Huge document | Resource limits | Stress/low-memory corpus |

---

# B19. Build & Release Gates

## v1.0 Product GA — Gates 0–5

### Gate 0 — Architecture & Feasibility

Must pass before feature implementation is considered architecturally locked:

- block model defined
- artifact model defined
- plugin contract defined
- cache contract defined
- threat model baseline defined
- **Mermaid feasibility spike completed**
- renderer strategy selected based on evidence

### Gate 1 — Core Reader

- SAF open
- parser → block model
- native rendering
- search
- TOC
- anchors
- links
- code
- tables
- images
- back behavior
- reader state

### Gate 2 — SVG & Plugin Kernel

- plugin registry
- artifact cache
- scheduler
- SVG sanitization
- placeholders
- failure isolation
- zoom/pan

### Gate 3 — Mermaid

- selected Mermaid strategy implemented
- representative diagram corpus passes
- WebView isolation works where required
- source fallback works
- no document-level failure

### Gate 4 — KaTeX

- offline math
- visual rendering
- malformed-input fallback
- timeout/cancellation
- theme behavior

### Gate 5 — Elite Reader UX

- Light/Dark/Sepia
- visible reading progress
- recents
- favorites
- resume reading
- immersive reader behavior
- core TalkBack semantics
- dynamic font scaling
- adaptive layout baseline
- lifecycle/process restoration

**Gate 5 is the v1.0 Product GA feature-complete gate.**

---

# B20. Public Release Hardening — Gates 6–8

### Gate 6 — Performance & Compatibility Hardening

- full seven-corpus benchmark suite
- five-device matrix
- Macrobenchmark regression analysis
- large-document stress testing
- low-memory testing
- final app-size review

### Gate 7 — Security & Reliability Hardening

- adversarial Markdown corpus
- malicious SVG corpus
- Mermaid/KaTeX adversarial cases
- cache corruption tests
- SAF provider matrix
- final network isolation
- crash/ANR review
- expanded accessibility audit

### Gate 8 — Public Release

- dependency/license register complete
- privacy declarations complete
- store listing complete
- screenshots/store copy complete
- closed-beta feedback triaged
- release build reproducibility checked
- no open P0/P1 crash, ANR, security, or data-loss defect
- final acceptance traceability complete

---

# B21. Revised 16-Week Execution Roadmap

## Phase 0 — Feasibility & Architecture
**Weeks 1–2**

Deliver:

- repository/scaffold
- Markdown parser spike
- block model spike
- artifact model
- plugin contract
- cache prototype
- benchmark seed corpus
- **native Mermaid validation spike**
- Mermaid renderer strategy decision

**Exit:** Gate 0.

## Phase 1 — Core Reader
**Weeks 3–5**

Deliver:

- Home
- SAF
- open-with
- native Markdown
- headings/lists/tables/code/images
- search
- TOC
- anchors
- links
- reader state

**Exit:** Gate 1.

## Phase 2 — Plugin Kernel + SVG
**Weeks 6–7**

Deliver:

- plugin registry
- renderer selector
- artifact cache
- scheduling
- SVG
- sanitization
- failure states

**Exit:** Gate 2.

## Phase 3 — Mermaid
**Week 8**

Implement the strategy selected in Phase 0:

- native-first if threshold passed
- WebView-primary if threshold failed
- zoom/pan
- fallback
- representative corpus

**Exit:** Gate 3.

## Phase 4 — KaTeX
**Week 9**

- offline assets
- WebView island if required
- math styling
- timeout
- fallback

**Exit:** Gate 4.

## Phase 5 — Elite Reader UX
**Weeks 10–11**

- Light/Dark/Sepia
- reading progress
- immersive reader
- favorites
- recents
- resume reading
- accessibility baseline
- adaptive layouts
- lifecycle restoration

## Phase 6 — GA Stabilization
**Week 12**

- fix P0/P1 defects
- stabilize core corpus
- performance baseline
- GA acceptance
- freeze v1.0 Product GA scope

**Exit:** Gate 5 → **v1.0 Product GA candidate.**

---

## Phase 7 — Performance & Compatibility Hardening
**Weeks 13–14**

- full benchmark matrix
- low-memory stress
- large documents
- device matrix
- performance optimization

**Exit:** Gate 6.

## Phase 8 — Security & Reliability Hardening
**Week 15**

- adversarial corpus
- malicious SVG
- plugin stress
- SAF provider matrix
- cache corruption
- accessibility expansion
- network verification

**Exit:** Gate 7.

## Phase 9 — Public Release
**Week 16**

- closed-beta feedback
- store assets
- privacy/license review
- final release build
- Play Store submission package

**Exit:** Gate 8.

---

# B22. Risk Register

| Risk | Impact | v2.1 mitigation |
|---|---|---|
| Native Mermaid incomplete | High | Resolve in Weeks 1–2 before architecture lock. |
| WebView becomes Mermaid primary | Medium/High | Architecture supports either renderer; scheduler is renderer-cost aware. |
| WebView memory pressure | High | Strict instance limits, viewport scheduling, artifact caching, low-memory mode. |
| Solo-dev scope | High | 12-week Product GA + 4-week public hardening; hardening cannot block core development. |
| Visual regressions | Medium | GA golden corpus; expanded matrix during hardening. |
| Security tail grows indefinitely | High | Security Gate 7 is bounded to Week 15 with explicit acceptance criteria. |
| SAF inconsistency | Medium | Basic GA coverage, full provider matrix in hardening. |
| Markdown dialect fragmentation | Medium | Explicit compatibility contract; extensions deferred unless required. |
| Cache complexity | Medium | Remove locale dimension until localization is a real product requirement. |

---

# B23. Cache Invalidation Contract

Artifact invalidation occurs when any artifact-affecting dimension changes:

- source hash
- plugin ID/version
- renderer version
- theme
- typography profile
- width bucket
- density bucket

**Localization is not a v1.0 cache dimension.**

If localization is introduced later and affects artifacts, it becomes a versioned cache-key migration.

---

# B24. Observability Without Telemetry

- Android/Play release health may be reviewed through platform-provided aggregate diagnostics.
- In-app diagnostics contain app version, renderer/plugin versions, device/API and cache state.
- Users may explicitly copy diagnostics.
- Raw document contents are never automatically included.

---

# B25. Dependency & Licensing Register

Exact versions are locked during implementation, not invented in the PRD.

| Dependency | Purpose | Requirement |
|---|---|---|
| Markwon | Markdown foundation | Pin and verify license/transitives. |
| Markdown parser/GFM support | Parsing | Lock compatibility matrix. |
| Mermaid.js | WebView Mermaid path | Vendor pinned version and license. |
| KaTeX | Math | Vendor pinned version and license. |
| SVG library | SVG rendering | Select and verify exact license. |
| Image loader | Local image decoding/loading | Local-only policy in v1.0. |

---

# PART C — TRACEABILITY & ACCEPTANCE

## C1. Requirement Prefixes

| Prefix | Meaning |
|---|---|
| FR | Functional |
| UX | User experience |
| REN | Rendering |
| PLG | Plugin |
| PERF | Performance |
| SEC | Security/privacy |
| ACC | Accessibility |
| DATA | Persistence |
| REL | Release |

## C2. Critical Acceptance Requirements

| ID | Acceptance |
|---|---|
| FR-001 | Open a Markdown document through SAF without network access. |
| UX-001 | Useful native content appears before optional rich blocks finish rendering. |
| UX-002 | Back closes transient reader UI before leaving the document. |
| UX-003 | Long press provides native text selection. |
| UX-004 | Search supports count/navigation/highlight. |
| UX-005 | User can select Light, Dark, or Sepia/Paper. |
| UX-006 | Document screen visibly communicates reading progress. |
| DATA-001 | Favorite and reading position survive restart. |
| REN-001 | Renderer failure cannot blank/crash the document. |
| PLG-001 | Plugins return cacheable artifacts rather than owning arbitrary document UI. |
| PLG-002 | Mermaid strategy is evidence-selected during Gate 0. |
| SEC-001 | v1.0 document rendering performs no network access. |
| SEC-002 | SVG unsafe execution/external references are rejected or neutralized. |
| ACC-001 | Core reader is usable with TalkBack. |
| PERF-001 | GA reference benchmark meets startup/first-content thresholds. |
| PERF-002 | Normal long-document scrolling has no sustained jank. |
| REL-001 | No P0/P1 crash, ANR, security, or data-loss issue at public release. |

## C3. Definition of Done

A feature is not done merely because its happy path works.

It is done when:

- implementation exists
- tests exist
- visual golden coverage exists where appropriate
- accessibility is checked
- dark/light/sepia behavior is checked where applicable
- failure path is checked
- resource use is bounded
- performance impact is measured for rendering changes
- documentation is updated
- requirement traceability is maintained

---

# APPENDIX A — GA Markdown Corpus

Minimum Product GA corpus:

- headings
- emphasis
- nested lists
- task lists
- links
- anchors
- tables
- blockquotes
- footnotes
- code
- images
- callouts
- Mermaid
- math
- SVG
- malformed/unknown fenced blocks

The seven-corpus hardening suite remains in Part B16.

# APPENDIX B — UX State Matrix

| State | Required behavior |
|---|---|
| First launch | Clear Open Markdown CTA. |
| Empty recent | No fake content. |
| Loading | Non-blocking progress/skeleton. |
| Plugin rendering | Stable placeholder; reader remains usable. |
| Plugin failure | Inline error + source fallback. |
| Missing file | Clear recovery/removal action. |
| Permission lost | Explain re-authorization. |
| Large document | Progressive rendering; safe mode if required. |
| No network | Core viewer remains functional. |
| Process death | Restore reader state when possible. |
| Theme change | Reader updates without corrupting cache. |
| Search active | Match remains visible while navigating. |

# APPENDIX C — Phase 0 Decisions

Must be locked by the end of Week 2:

1. minSdk
2. exact Markdown parser/Markwon stack
3. native Mermaid candidate and corpus result
4. Mermaid renderer strategy
5. SVG implementation/sanitization boundary
6. KaTeX implementation
7. reference benchmark device
8. GA resource limits
9. cache limits
10. reader typography tokens
11. app naming decision sufficient for development/release planning

# APPENDIX D — Final Sign-Off Checklist

## Product GA

- [ ] Gate 0 — Architecture & Mermaid feasibility
- [ ] Gate 1 — Core Reader
- [ ] Gate 2 — SVG & Plugin Kernel
- [ ] Gate 3 — Mermaid
- [ ] Gate 4 — KaTeX
- [ ] Gate 5 — Elite Reader UX

## Public Release

- [ ] Gate 6 — Performance & Compatibility Hardening
- [ ] Gate 7 — Security & Reliability Hardening
- [ ] Gate 8 — Public Release

# Final Build-Signoff Statement

> **Inkleaf v2.1 is designed to be ambitious without being schedule-fiction.**

The product vision remains elite. The execution contract is now honest:

**12 weeks to build and stabilize the complete viewer-first product experience.  
4 additional weeks to prove it against the deeper public-release quality matrix.**

The hardening phase is not a dumping ground for unfinished core features. It is explicitly reserved for proving, measuring, attacking, and polishing a product that already works.

The goal is not merely to ship a Markdown viewer.

**The goal is to establish an Android-native reading engine that can become exceptionally good.**
