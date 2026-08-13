# Security & Privacy Guidelines — Inkleaf

## 1. Network Access Policy

- **Strict Offline Posture**: Inkleaf v1.0 performs zero document-originated network requests.
- `android.permission.INTERNET` must **NOT** be added to `AndroidManifest.xml`.
- Remote image URLs (`https://...`) in Markdown documents must not be fetched automatically in v1.0. Display a local placeholder or explicit offline indicator.

## 2. Storage Access Framework (SAF)

- Do not request broad file access permissions (`READ_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE`).
- Use SAF system pickers (`Intent.ACTION_OPEN_DOCUMENT`) and persistent URI permissions (`content://...`).
- Safely handle missing/revoked URI permissions without throwing unhandled exceptions.

## 3. WebView Sandboxing

For WebView instances used as rich rendering fallback islands (Mermaid / KaTeX):
- Disable network: `settings.blockNetworkLoads = true`
- Disable file access where unnecessary: `settings.allowFileAccess = false`
- Disable JavaScript interface binding to arbitrary Java objects.
- Disable WebView debugging in release builds (`WebView.setWebContentsDebuggingEnabled(false)`).

## 4. SVG Sanitization

Before rendering local SVG content:
- Strip `<script>` tags, inline event attributes (`onload`, `onclick`, `onerror`).
- Neutralize external SVG `<image href="http...">` links.
- Enforce maximum SVG node count and decode bounds to prevent memory bombs.
