# Branding assets

Design exploration archive for the 3D Model Viewer plugin logo and editor
file-type icons. These are working artifacts kept for reference; the assets
actually shipped by the plugin live under `src/main/resources/`.

## Shipped assets (source of truth)

- Plugin logo: `src/main/resources/META-INF/pluginIcon.svg` and `pluginIcon_dark.svg`
- File-type icons: `src/main/resources/icons/{glb,gltf,obj,stl}_{light,dark}.svg`

Changes to these only take effect on the Marketplace / in the IDE on the next
release build.

## Final choices

Plugin logo: a half-solid / half-wireframe monkey (Suzanne) that reads as a 3D
model preview.

- Solid half: amber `#CCCC33`
- Wireframe half: cyan `#47A3D1`
- The two hues are picked from the color wheel for a harmonious, distinct
  solid-vs-wireframe split, and stay legible on both light and dark themes.

File-type icons: one distinct glyph per format so they are easy to tell apart
in the New UI project tree and editor tabs.

- GLB: solid cube
- glTF: wireframe cube (glTF is the text/source form of GLB)
- STL: faceted triangle mesh (STL is a triangle soup)
- OBJ: shaded globe

## Directory guide

- `final/` — the chosen monkey SVGs and the swap helper used during testing.
- `logo-colors/` — full logo color exploration: two-tone pairings, analogous
  (30 degrees) and 60-degree wheel splits, the solid-hue rotation with cyan
  wireframe locked, and the fine amber-to-lime steps.
- `file-icon-shapes/` — the per-format shape glyphs (cube / wireframe cube /
  triangles / globe).
- `obj-variants/` — the OBJ sphere options (A classic globe, B latitude bands,
  C low-poly facets). A was chosen.
- `badges/`, `early-file-icons/` — earlier file-icon studies (lettered chips and
  color badges) that were rejected in favor of the shape glyphs.
- `previews/` — rendered comparison sheets (PNG) used to review each round.
- `explorations/` — standalone shape/logo candidate SVGs from early rounds
  (cube, sphere, gem, torus, teapot, icosahedron, cylinder, monkey, etc.).

## Tooling

Previews were rendered with `rsvg-convert` (librsvg). Comparison sheets are SVGs
that reference rasterized PNGs; open the `previews/*.png` directly to view them.
