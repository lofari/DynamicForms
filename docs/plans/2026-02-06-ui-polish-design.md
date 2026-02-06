# UI Polish Design

## 1. Form Elements — Section-based Cards

Group form elements into cards based on `SectionHeaderElement` boundaries.

**Grouping logic** in `FormScreen`:
- Walk through the page's elements and split into groups at each `SectionHeaderElement`
- Each group becomes an `ElevatedCard`
- The `SectionHeaderElement`'s title becomes the card header (`titleMedium`), subtitle below it (`bodySmall`, `onSurfaceVariant`)
- Elements before the first `SectionHeader` (if any) go into a headerless card
- `SectionHeaderElement` is consumed by the card header — not rendered as a standalone element inside the card

**Card styling:**
- `CardDefaults.elevatedCardElevation()` (default subtle shadow)
- 12dp rounded corners
- 16dp padding inside
- 12dp vertical spacing between child elements
- Section title: 4dp bottom padding before first element

**What stays the same:** Individual form element composables are unchanged. The card is a layout-level change in `FormScreen` only.

## 2. Success Screen

Replace plain centered text with a structured confirmation card.

**Layout (centered on screen):**
```
ElevatedCard (maxWidth 360dp, 24dp padding) {
    Circle (64dp, primaryContainer background) {
        Check icon (32dp, onPrimaryContainer)
    }
    16dp spacer
    "Success!" — headlineMedium, centered
    8dp spacer
    Dynamic message — bodyLarge, onSurfaceVariant, centered
    24dp spacer
    "Back to Forms" — filled Button
}
```

No animations. Static layout.

## 3. Form List Screen — Richer Cards

### 3a. Model change

Add `pageCount` and `fieldCount` to `FormSummary`:

```kotlin
data class FormSummary(
    val formId: String,
    val title: String,
    val description: String = "",
    val pageCount: Int = 0,
    val fieldCount: Int = 0
)
```

Update mock JSON to include the counts.

### 3b. Card layout

```
Card {
    Row {
        Column(weight 1) {
            title (titleMedium)
            description (bodyMedium, onSurfaceVariant)
            Row(8dp spacing) {
                chip: "N steps"
                chip: "N fields"
                chip: "Resume" (if draft exists)
            }
        }
        ChevronRight icon (onSurfaceVariant)
    }
}
```

**Metadata chips:** `bodySmall`, `surfaceVariant` background, rounded (50%) corners, 4dp vertical / 8dp horizontal padding. Not `AssistChip` — just styled `Surface` composables to keep them lightweight and non-interactive.

**Draft "Resume" chip:** Same style but uses `tertiaryContainer` / `onTertiaryContainer` colors to stand out.

## Files to modify

1. `FormScreen.kt` — Add section grouping logic with cards
2. `SuccessScreen.kt` — Rebuild layout with card + icon
3. `FormListScreen.kt` — Richer card layout with metadata and chevron
4. `FormSummary` in `Form.kt` — Add `pageCount`, `fieldCount`
5. `MockInterceptor.kt` — Add counts to mock JSON
6. `FormElementRenderer.kt` — No changes (section header rendering may need a conditional skip if consumed by card)
