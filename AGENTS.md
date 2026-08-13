# AGENTS.md — Workspace Sitemap & Subagent Guide for Inkleaf

Welcome to the **Inkleaf** codebase! This document provides context, sitemap navigation, and role definitions for agents working on this Native Markdown Reader for Android.

---

## 1. Project Overview

**Inkleaf** is a viewer-first, plugin-extensible Markdown reader for Android built with Kotlin and Jetpack Compose.
- **Primary Spec**: [Inkleaf PRD/TAD v2.1](file:///C:/Luminary/Projects/inkleaf/Inkleaf_PRD_TAD_v2.1_Elite_Build_Signoff.md)
- **Constraint Rules**: [GEMINI.md](file:///C:/Luminary/Projects/inkleaf/GEMINI.md)

---

## 2. Directory Sitemap

```text
C:/Luminary/Projects/inkleaf/
├── Inkleaf_PRD_TAD_v2.1_Elite_Build_Signoff.md  → Single Source of Truth specification
├── GEMINI.md                                    → Agent non-negotiable rules & constraints
├── AGENTS.md                                    → Workspace sitemap & role guide (this file)
├── .agents/
│   ├── rules/
│   │   ├── rendering_kernel.md                  → Block parsing, scheduler, and cache rules
│   │   └── security_and_privacy.md              → Zero-network, SVG & WebView sandbox rules
│   └── skills/
│       ├── inkleaf-env-setup/                   → Tooling & SDK setup skill
│       └── inkleaf-phase0-spike/                → Mermaid feasibility & block AST spike skill
├── scripts/
│   ├── setup-environment.ps1                    → PowerShell script for JDK 17 & Android SDK
│   └── setup-environment.cmd                    → CMD launcher script
└── app/
    ├── build.gradle.kts                         → App dependencies & Compose config
    └── src/main/java/com/inkleaf/app/
        ├── domain/                              → Core reader business logic & contracts
        ├── data/                                → SAF, DataStore preferences, LRU artifact cache
        └── ui/                                  → Jetpack Compose theme, screens, and components
```

---

## 3. Subagent Roles & Personas

When delegating tasks or spawning subagents, match work to these specialized roles:

### 1. `Android System & Environment Architect`
- **Focus**: Gradle build scripts, Android SDK target parameters (API 34), JDK 17 setup, manifest permissions, dependencies.
- **Key Task**: Execute and verify `scripts/setup-environment.ps1` to ensure `ANDROID_HOME`, `JAVA_HOME`, and `gradlew` compile cleanly.

### 2. `Block Kernel & Plugin Engineer`
- **Focus**: CommonMark/GFM AST parser, `BlockModel` sealed class definitions, `RenderPlugin` interface implementation, render scheduling, and artifact LRU cache key hashing.
- **Key Task**: Implement Phase 0 Gate 0 contracts (`BlockModel`, `RenderArtifact`, `RenderPlugin`, `ArtifactCache`).

### 3. `Compose Reader UX & Theme Designer`
- **Focus**: Jetpack Compose UI, typography scale, Light / Dark / **Sepia (Paper)** theme tokens, reading progress bar, Table of Contents drawer, and search highlighting.
- **Key Task**: Ensure native reading ergonomics with zero layout shift and smooth scroll performance.

### 4. `Security & Sandbox Specialist`
- **Focus**: Local-only SAF document access, strict offline WebView configuration for Mermaid/KaTeX, SVG script sanitization, zero-network enforcement.
- **Key Task**: Audit `AndroidManifest.xml` and WebView settings to guarantee zero document-originated network calls.

---

## 4. Key Workflows & Execution Gates

- **Phase 0 (Weeks 1–2)**: Gate 0 — Architecture & Mermaid feasibility spike.
- **Phase 1 (Weeks 3–5)**: Gate 1 — Core Reader (SAF, open-with, native Markdown, TOC, search).
- **Phase 2 (Weeks 6–7)**: Gate 2 — SVG & Plugin Kernel (Cache, scheduler, SVG sanitization).
- **Phase 3 (Week 8)**: Gate 3 — Mermaid (Native / WebView strategy implementation).
- **Phase 4 (Week 9)**: Gate 4 — KaTeX (Offline Math rendering).
- **Phase 5 (Weeks 10–11)**: Gate 5 — Elite UX (Sepia theme, reading progress, recents, favorites).
- **Phase 6 (Week 12)**: Gate 5 Sign-off → **v1.0 Product GA Candidate**.
