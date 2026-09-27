# WAR Map — provenance

The map rendered by `frontend/src/components/game/BoardSvg.tsx` is an
original, stylized representation of the classic Brazilian "War" board game.

- 42 territories across 6 continents — names match `EClassicCountries`
  (Manual §1).
- Polygons are schematic octagonal cells generated mechanically from the
  centroids defined in `src/components/game/map-data.ts`. They preserve
  relative continent grouping but are **not** geographically accurate.
- Continent tint colors are part of the jWar design system, defined in
  `tailwind.config.ts` under `colors.continent.*`.

This artwork is original to the jWar project (MIT licensed). No third-party
imagery is embedded; the favicon and decorative SVGs were authored in-repo.
