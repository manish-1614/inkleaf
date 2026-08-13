# Rendering Kernel Guidelines — Inkleaf

## 1. Block Pipeline Architecture

Inkleaf enforces a strictly decoupled block-oriented rendering pipeline:

```text
[Markdown File / Content]
        ↓
  MarkdownParser (Markwon / CommonMark)
        ↓
  Normalized AST
        ↓
  BlockModel Mapping (Sealed Block classes)
        ↓
  RendererSelector (Native vs Plugin vs WebView)
        ↓
  RenderScheduler (Cost-aware Coroutines Worker)
        ↓
  RenderArtifact (Bitmap, Vector, HTML, Text, Error)
        ↓
  ArtifactCache (Disk LRU + In-Memory LRU)
        ↓
  Compose Reader Viewport
```

## 2. Block Model Rules

- **Immutability**: All `BlockModel` items must be immutable `data class` or `sealed interface` types.
- **Source Range Traceability**: Every block must capture `sourceRange` to allow exact line jump and scroll position syncing.
- **No Direct AST Exposure**: Compose UI layers must NEVER import or reference parser-internal AST nodes (e.g. `org.commonmark.node.Node`). They must consume `BlockModel`.

## 3. Render Scheduling Priorities

To guarantee smooth reading performance:
1. **P0 (Highest)**: Native prose (paragraphs, headings, lists, blockquotes). Visualized instantly.
2. **P1 (Medium)**: Local SVG images, code syntax highlighting, native callout decorations.
3. **P2 (Deferred)**: Heavy rich blocks (Mermaid diagrams, KaTeX math rendering, WebView islands).

Off-screen blocks outside the current viewport threshold should defer high-cost rendering jobs until scrolled into range.

## 4. Artifact Cache Keys

Disk artifact key calculation rule:
`SHA-256(sourceHash + pluginId + pluginVersion + rendererVersion + themeId + typographyProfile + widthBucket + densityBucket)`

Do NOT include `locale` in v1.0 cache keys.
