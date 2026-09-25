# iOS Keyboard Theme & Styling Specifications

This document outlines the precise color codes, dimensional ratios, and visual states used in the iOS Keyboard clone.

---

## 1. Color Palette Matrix

| UI Component | iOS Light Mode Hex | iOS Dark Mode Hex | Description |
|---|---|---|---|
| **Canvas Background** | `#D1D5DB` | `#1C1C1E` | Main underlying keyboard chassis background |
| **Character Key Surface** | `#FFFFFF` | `#2C2C2E` | Normal letter/digit key face |
| **Key Surface (Pressed)** | `#E5E5EA` | `#48484A` | Depressed key face state |
| **Key Bottom Drop Shadow** | `#898A8D` | `#121213` | 1.2dp physical elevation shadow |
| **Modifier Key Surface** | `#AFB4BD` | `#3A3A3C` | Shift, Backspace, 123, Symbol keys |
| **Modifier Key (Pressed)** | `#FFFFFF` | `#636366` | Pressed state for modifier keys |
| **Primary Key Text** | `#000000` | `#FFFFFF` | Glyph/label color |
| **Secondary Key Text** | `#6C6C70` | `#8E8E93` | Spacebar sub-labels and alternates |
| **Accent Action Blue** | `#007AFF` | `#0A84FF` | iOS primary accent blue |
| **Action Return Key** | `#007AFF` | `#0A84FF` | Search/Go/Send highlighted return key |
| **Return Key Text** | `#FFFFFF` | `#FFFFFF` | White label on blue return key |
| **Suggestion Strip Bg** | `#ECEFF2` | `#252528` | Background for 3-column suggestion bar |
| **Popup Bubble Surface** | `#FFFFFF` | `#2C2C2E` | Key magnifier popup balloon |
| **Popup Drop Shadow** | `#55000000` | `#88000000` | Soft elevation drop shadow for popups |
| **Gesture Trail Path** | `#66007AFF` | `#660A84FF` | 40% translucent iOS blue swipe curve |

---

## 2. Geometry & Metrics

```
+-------------------------------------------------------+
|  [Left: Literal]  |  [Center: "Autocorrect"]  | [Right] |  <- Suggestion Strip (42dp)
+-------------------------------------------------------+
|   [ Q ]   [ W ]   [ E ]   [ R ]   [ T ]   [ Y ] ...    |  <- Row 1 (Key height ~42dp)
|     [ A ]   [ S ]   [ D ]   [ F ]   [ G ]   [ H ] ...  |  <- Row 2
| [⇧]   [ Z ]   [ X ]   [ C ]   [ V ]   [ B ] ...   [⌫]  |  <- Row 3
| [123]   [🌐]    [🎤]    [      space      ]    [return]|  <- Row 4
+-------------------------------------------------------+
|                    [ Home Bar Inset ]                 |  <- Bottom padding (4-8dp)
+-------------------------------------------------------+
```

- **Key Corner Radius:** `5dp`
- **Horizontal Key Spacing:** `6dp`
- **Vertical Row Spacing:** `10dp`
- **Outer Margin:** `4dp`
- **Long-Press Popover Item Width:** `38dp`
- **Magnifier Balloon Scale:** `135% width`, `130% height` relative to key size

---

## 3. Dynamic Auto-Switching

The keyboard observes Android system configuration flags:
```kotlin
val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
    ThemeColors.Dark
} else {
    ThemeColors.Light
}
```
Users can manually lock the theme to **Always Light** or **Always Dark** in the iOS Settings screen.
