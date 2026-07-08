# Project: EngFlow Frontend UI/UX Redesign (Playful Geometric Design)

## Architecture
The application is a Vue 3 single-page application powered by Vite, Pinia, and Tailwind CSS.
It follows a clean layout:
- `src/assets/`: Stylesheet layers.
- `src/components/`: Reusable UI widgets and component blocks.
- `src/views/`: Routed pages (User-facing and Admin panel).
- `src/router/`: Application navigation mapping.
- `src/store/`: Reactive store modules (Pinia).
- `src/services/`: REST API service layer.

## Code Layout
- Global Styles: `frontend/src/assets/bauhaus.css`
- Tailwind Config: `frontend/tailwind.config.js`
- Core Components: `frontend/src/components/bauhaus/`
  - `BauhausButton.vue` (and `BaseButton.vue` for compliance)
  - `BauhausCard.vue` (and `BaseCard.vue` for compliance)
  - `FlashcardFlip.vue`
  - `StreakCalendar.vue`
  - `GameResult.vue`
  - `AudioButton.vue`
- User Views: `frontend/src/views/`
- Admin Views: `frontend/src/views/admin/`
- Router Config: `frontend/src/router/index.js`

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|---|---|---|---|
| 1 | Design System Setup | Update `tailwind.config.js` and `index.html` to configure fonts, vibrant color palette, `shadow-pop`, `blob` radius, and set `preflight: false`. | None | DONE |
| 2 | Core Components Redesign | Re-implement core components (`BauhausButton`, `BauhausCard`, etc.) and create `BaseButton.vue` & `BaseCard.vue` with Playful Geometric styles. | M1 | DONE |
| 3 | User-Facing Views Redesign | Update at least 80% of user-facing views to use the new Playful Geometric Design classes. | M2 | DONE |
| 4 | Admin Dashboard Views Redesign | Update admin panel views and layout shell to conform to the new design system. | M2 | DONE |
| 5 | Verification & Bugfixes | Fix the missing `/lessons/:id/preview` admin builder router bug, verify `npm run build` passes, and use `chrome-devtools` to capture and verify the rendered UI. | M3, M4 | IN_PROGRESS |

## Interface Contracts
### Components ↔ Views
- `BauhausButton` / `BaseButton`:
  - Props: `variant` (primary, secondary, yellow, outline, ghost), `shape` (square, pill, blob), `size` (sm, md, lg).
- `BauhausCard` / `BaseCard`:
  - Props: `hoverEffect` (boolean), `decoration` (boolean), `decorationShape` (circle, square, triangle, blob), `decorationColor` (red, blue, yellow, green, purple).
