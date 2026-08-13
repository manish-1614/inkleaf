---
name: inkleaf-phase0-spike
description: Instructions for running Phase 0 Gate 0 feasibility spike, native Mermaid parse validation, and AST block model verification.
---

# Inkleaf Phase 0 Feasibility Spike Skill

This skill details how to perform Gate 0 feasibility checks defined in PRD/TAD Section B2 & B3.

## 1. Native Mermaid Feasibility Decision Gate

Evaluate candidate native Mermaid rendering capabilities against the test corpus (`C3` diagram heavy corpus):
1. **Parse Success Rate Target**: ≥ 90% across flowcharts, sequence diagrams, class diagrams, state diagrams.
2. **Performance Target**: Rendering time < 250ms on reference midrange device.
3. **Decision Outcome**:
   - If target met → Native renderer preferred, WebView fallback.
   - If target missed → WebView primary renderer for Mermaid, Native fallback for basic flowcharts.

## 2. Block AST Verification

Verify sealed data classes in `com.inkleaf.app.domain.model.BlockModel`:
- `ParagraphBlock`
- `HeadingBlock` (level 1-6)
- `CodeBlock` (language tag, code text)
- `TableBlock` (header, rows, alignments)
- `CalloutBlock` (type, title, content)
- `MermaidBlock` (diagram source)
- `MathBlock` (latex formula, inline/block flag)
- `SvgBlock` (raw SVG XML)
- `RawFallbackBlock` (fallback text)
