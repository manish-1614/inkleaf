---
name: inkleaf-phase0-spike
description: Instructions for running Phase 0 Gate 0 architecture spike and AST block model verification.
---

# Inkleaf Phase 0 Feasibility Spike Skill

This skill details how to perform Gate 0 checks defined in PRD/TAD Section B2 & B3.

## 1. Block AST Verification

Verify sealed data classes in `com.inkleaf.app.domain.model.BlockModel`:
- `ParagraphBlock`
- `HeadingBlock` (level 1-6)
- `CodeBlock` (language tag, code text)
- `TableBlock` (header, rows, alignments)
- `CalloutBlock` (type, title, content)
- `MathBlock` (latex formula, inline/block flag)
- `SvgBlock` (raw SVG XML)
- `ImageBlock` (url, altText, title)
- `RawFallbackBlock` (fallback text)

