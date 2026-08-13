# GEMINI.md — Agent Working Rules for Inkleaf

This file is read by Antigravity/Gemini before generating or editing code in this repository. It is a **constraint file, not a feature spec** — the PRD/TAD (`Inkleaf_PRD_TAD_v2.1_Elite_Build_Signoff.md`) defines *what* to build; this file defines *how* to build it consistently across every session. Read the PRD/TAD before starting any phase of work.

If any instruction below conflicts with a request in a prompt, follow this file unless the human explicitly overrides it in that prompt.

---

## 0. Source of Truth

- **PRD/TAD v2.1** is the single source of truth for product behavior and technical architecture. Do not re-derive closed architecture decisions (e.g., do not add a database server/SQLite/Room, do not add an editor component, do not add document-originated network access, do not add telemetry SDKs).
- **Two-Tier Release Execution**:
  - **Weeks 1–12**: v1.0 Product GA (Gates 0–5). Feature-complete core viewer, block kernel, plugins, UX.
  - **Weeks 13–16**: Public Release Hardening (Gates 6–8). Benchmarks, adversarial fuzzing, store packaging.
- Build strictly in phase order (TAD Section B21). Do not jump to Phase 5 UX before Gate 0/1 architecture & core reader gates pass.
- If a request conflicts with an explicit constraint in PRD/TAD v2.1, stop and flag the conflict rather than silently choosing a conflicting implementation.

---

## 1. Project Structure

```text
inkleaf/
├── GEMINI.md                    → Agent working rules (this file)
├── AGENTS.md                    → Workspace sitemap, roles, and agent entry points
├── Inkleaf_PRD_TAD_v2.1_*.md    → Product & Technical Specification
├── .agents/
│   ├── rules/                   → Scoped guidelines (rendering, security, performance)
│   └── skills/                  → Automated workflow skills & cheatsheets
├── scripts/
│   ├── setup-environment.ps1    → Automated JDK 17 & Android SDK environment installer
│   └── setup-environment.cmd    → CMD runner for environment setup
├── app/
│   ├── build.gradle.kts         → Module build configuration
│   └── src/main/
│       ├── AndroidManifest.xml  → Manifest (no INTERNET permission)
│       ├── java/com/inkleaf/app/
│       │   ├── domain/
│       │   │   ├── model/       → BlockModel, RenderArtifact, ReaderState
│       │   │   ├── parser/      → CommonMark / GFM AST parser & Block mapper
│       │   │   └── plugin/      → RenderPlugin contract, PluginRegistry, RenderScheduler
│       │   ├── data/
│       │   │   ├── cache/       → Memory & Bounded Disk LRU Artifact Cache
│       │   │   ├── preferences/ → Jetpack DataStore (recent files, reading position, theme)
│       │   │   └── saf/         → Storage Access Framework document repository
│       │   └── ui/
│       │       ├── theme/       → Light, Dark, Sepia/Paper Compose typography & colors
│       │       ├── home/        → Open Markdown, Recents, Favorites screen
│       │       └── reader/      → Block-oriented Compose reader, TOC, Search, Progress
│       └── res/                 → Vector drawables, raw assets (KaTeX, Mermaid bundle)
├── build.gradle.kts             → Root build script
├── settings.gradle.kts          → Project settings
└── gradle.properties            → JVM & AndroidX properties
```

---

## 2. Non-Negotiable Rules

1. **Zero Database Server / Local-Only Persistence**:
   - No DB server (Postgres, MySQL, Firebase, Supabase).
   - No SQLite/Room database in v1.0 unless explicitly approved.
   - All persistence uses Jetpack DataStore for preferences/reading position and a bounded Disk LRU cache for rendered diagram/math block artifacts.
2. **Block-Oriented Hybrid Rendering Kernel**:
   - No single renderer owns the document. Markdown source → Parser → AST → `BlockModel` → Renderer Selection → `RenderArtifact` → Compose Presenter.
   - Native prose (paragraphs, headings, lists) paints first. Rich blocks (Mermaid, KaTeX, SVG) render asynchronously without blocking text readability.
3. **Strict Network Isolation (Privacy by Architecture)**:
   - `android.permission.INTERNET` must **NOT** be declared in `AndroidManifest.xml`.
   - WebView instances must have network access disabled (`settings.blockNetworkLoads = true`).
   - SVG renderer must sanitize external links, `<script>` tags, and remote image references.
4. **Failure Isolation**:
   - A plugin crash, render timeout, or malformed diagram must **NEVER** crash the app or blank out the document.
   - Render failures fall back gracefully to `ErrorArtifact` or formatted raw Markdown code source.
5. **Three Core Reader Themes**:
   - Light, Dark, and **Sepia / Paper** are first-class theme tokens. Theme changes must invalidate/update rendered artifacts cleanly without memory leaks.

---

## 3. Terminal & Windows Environment Conventions

- Developer OS: **Windows 11 running Command Prompt (cmd.exe)** or **PowerShell 7+**.
- Always specify explicit, executable commands (`cmd.exe /c ...` or `powershell -ExecutionPolicy Bypass -File ...`).
- When referencing file paths on Windows, use clean forward slashes in Markdown links (`file:///C:/Luminary/Projects/inkleaf/...`) or standard backslashes in Windows CLI scripts.
- Use Gradle Wrapper (`gradlew.bat` or `./gradlew`) for all build and test tasks.

---

## 4. What NOT to Do

- **Don't add Markdown editing features.** Inkleaf is strictly a viewer-first reading engine in v1.0.
- **Don't create a database schema.** DataStore + Disk LRU handles all state.
- **Don't introduce telemetry, analytics, or remote logging SDKs.**
- **Don't bypass the BlockModel.** Compose UI components must consume structured `BlockModel` objects, not raw AST nodes or unparsed raw strings.
- **Don't include locale in v1.0 artifact cache keys.** Cache key formula: `SHA-256(sourceHash + pluginId + pluginVersion + rendererVersion + themeId + typographyProfile + widthBucket + densityBucket)`.

---

## 5. Phase 0 Acceptance & Verification

- Before committing major feature logic, verify Phase 0 Gate 0:
  - `BlockModel` sealed hierarchy compiled.
  - `RenderArtifact` types compiled.
  - `RenderPlugin` interface contract compiled.
  - Native Mermaid feasibility spike script ready.
