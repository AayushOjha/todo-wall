# TodoWall Design Guide

## 🎨 Brand Identity
**Concept**: "Focus & Clarity". The app should feel calm, professional, and non-intrusive, especially since its primary output is a wallpaper.

### Brand Colors
| Role | Color | Hex | Description |
| :--- | :--- | :--- | :--- |
| **Primary** | Electric Blue | `#3D8BFF` | Action items, primary buttons. |
| **Secondary** | Deep Slate | `#101418` | Backgrounds, dark mode base. |
| **Surface** | Dark Grey | `#1E2329` | Cards, input fields. |
| **Accent** | Soft Mint | `#A7F3D0` | Completion states, success icons. |
| **Error** | Rose | `#FF5F5F` | Delete actions, warnings. |
| **Text (High)** | White | `#FFFFFF` | Primary content. |
| **Text (Med)** | Steel | `#94A3B8` | Subtitles, disabled states. |

## Typography
- **Headlines**: Sans-serif (Inter/Roboto), Bold, tight letter spacing.
- **Body**: Sans-serif, Regular/Medium, 16sp base.
- **Todo Items**: 18sp for clarity on wallpaper.

## 🍱 Component Styles
- **Corners**: 20dp for primary containers, 12dp for items.
- **Elevation**: Minimal. Use border/surface contrast instead of heavy shadows.
- **Spacing**: 16dp standard gutter, 8dp between related elements.

## 🛠 UX Principles
1. **Zero Friction**: Adding a todo should be a one-tap experience.
2. **Invisible Sync**: Wallpaper updates happen automatically without user intervention.
3. **Hierarchy**: The current task should always be the most prominent.
