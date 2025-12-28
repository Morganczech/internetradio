Touch Radio v1.2.0 — UX Refinements & Visual Polish Release

This release focuses on improving visual consistency, user experience,
and production stability. No architectural changes were introduced.

Changes

• Improved category visuals and chip readability
• Favorites category now uses a filled heart icon
• Unified and per-category color themes refined
• Player UI now adapts consistently to the active category
• Initial local station fetch limit reset to 10 for faster startup
• Android Auto Backup re-enabled for favorites persistence
• Verbose debug logs removed from production builds

Stability & Reliability

• Improved DNS resolution with hybrid fallback strategy
• Cleaned up logging to reduce runtime noise and improve security
• Release build reliability improved (Kotlin Unit return fix)

Technical

• Fixed Kotlin compiler issue in EqualizerManager (explicit Unit return in locked blocks)
• Release build now compiles cleanly without conditional expression ambiguity
• Version bumped to 1.2.0 (code 4)

APK

• Signed release APK attached to GitHub Release v1.2.0
• SHA-256:
  6a0671c47f47f7665f510f3a230711d197700ec6f9527196402db8cdc447df3e

The project remains stable and actively maintained.
