# Project Reference: Inkleaf

**Core Value:** A viewer-first, plugin-extensible native Markdown reader for Android with zero layout shift and smooth reading of large documents.

**Current Focus:** Large-document stability & performance (Phases 1-4)

## Key Facts
- Kotlin + Jetpack Compose, minSdk 26, targetSdk 34, JDK 17
- SAF for document access, DataStore preferences, CommonMark parser
- Zero-network rule from PRD (user granted temporary exception for Coil remote images)
- Spec: `Inkleaf_PRD_TAD_v2.1_Elite_Build_Signoff.md`; constraints: `GEMINI.md`, `AGENTS.md`

## Environment
- Build: `cmd /c "gradlew.bat compileDebugKotlin --console=plain"` (PowerShell direct invocation fails)
- JAVA_HOME: Eclipse Adoptium JDK 17; ANDROID_HOME set

## Active Decisions
- Full architectural refactor approved (ViewModel layer) — user decision 2026-08-26
- Mermaid/KaTeX: virtualize + reuse WebViews via pooling — user decision 2026-08-26
- Remote images via Coil remain until a later round — user decision 2026-08-26
