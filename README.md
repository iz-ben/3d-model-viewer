# 3D Model Viewer

View and inspect 3D models directly in your IntelliJ-based IDE.

## Features

- View 3D models directly in the editor
- Wireframe mode toggle via status bar
- Animation playback controls (play/pause)
- Animation selector for models with multiple animations
- glTF JSON view alongside the 3D preview (Markdown-style editor / split / preview toggle)
- Highlight materials in the model by moving the caret or selecting inside the glTF JSON
  (works for `materials`, `meshes`, and `nodes` entries)
- **Model Explorer** tool window: browse a `.glb` / `.gltf` file's internal structure
  (asset, scenes, nodes, meshes, materials, accessors, buffer views, images, samplers,
  animations, …). Double-click a node to jump to it in the glTF JSON, or select a material,
  mesh or node to highlight it in the 3D preview

## Supported File Formats

- **GLB** — Built-in support, works out of the box
- **GLTF** — Built-in support, automatically bundles referenced assets (textures, binary files)
- **three.js JSON** — Built-in support for three.js model JSON (BufferGeometry / Object / Scene). Detected by content, so ordinary `.json` files are unaffected and keep their normal JSON editor plus an Editor/Preview toggle
  
- **OBJ** — Optional support, enable in **Settings → Tools → 3D Model Viewer**

> [!WARNING]
> OBJ support is opt-in because other plugins may also register the `.obj` file extension.

## Installation

### From JetBrains Marketplace

1. Open **Settings** → **Plugins** → **Marketplace**
2. Search for "3D-Model-Viewer"
3. Click **Install**
4. Restart IDE if prompted

### From GitHub Releases

1. Download the latest `.zip` file from [GitHub Releases](https://github.com/iz-ben/3d-model-viewer/releases)
2. Open **Settings** → **Plugins**
3. Click the ⚙️ icon → **Install Plugin from Disk...**
4. Select the downloaded zip file
5. Restart IDE if prompted

## Usage

1. Open any supported 3D model file (`.glb`, `.gltf`, or `.obj`)
2. The model will render in the editor tab
3. Use the status bar widgets at the bottom to:
   - Toggle **Wireframe** mode
   - **Play/Pause** animations
   - Select animations
4. For `.glb` / `.gltf` files, open the **Model Explorer** tool window (right stripe, shown while
   a glTF file is focused) to browse the model's internal structure — double-click a node to jump
   to it in the glTF JSON, or select a material/mesh/node to highlight it in the preview

## Screenshots & Videos

## Demo Videos

[![Damaged Helmet Demo Video](assets/videos/damaged_helmet.gif)](assets/videos/damaged_helmet.mp4)
[![Littlest Tokyo Demo Video](assets/videos/littlest_tokyo.gif)](assets/videos/littlest_tokyo.mp4)
[![Robot Expressive Demo Video](assets/videos/robot_expressive.gif)](assets/videos/robot_expressive.mp4)

## Screenshots
<div class="slider">
  <img src="assets/images/damaged_helmet.png" alt="Damaged Helmet Wireframe"/>
  <img src="assets/images/damaged_helmet_wireframe.png" alt="Damaged Helmet"/>
  <img src="assets/images/littlest_tokyo.png" alt="Littlest Tokyo"/>
  <img src="assets/images/littlest_tokyo_wireframe.png" alt="Littlest Tokyo Wireframe"/>
  <img src="assets/images/robot_expressive.png" alt="Robot Expressive"/>
  <img src="assets/images/robot_expressive_wireframe.png" alt="Robot Expressive Wireframe"/>
</div>

## Contributing

This project uses [Conventional Commits](https://www.conventionalcommits.org/) to automatically determine version bumps and generate changelogs.

### Commit Message Format

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Commit Types

| Type | Description | Version Bump |
|------|-------------|--------------|
| `feat:` | A new feature | Minor |
| `fix:` | A bug fix | Patch |
| `docs:` | Documentation only changes | None |
| `style:` | Code style changes (formatting, etc.) | None |
| `refactor:` | Code refactoring | None |
| `perf:` | Performance improvements | None |
| `test:` | Adding or updating tests | None |
| `chore:` | Maintenance tasks | None |

### Breaking Changes

Add `BREAKING CHANGE:` in the commit footer or `!` after the type to trigger a **major** version bump:

```
feat!: remove deprecated API endpoints

BREAKING CHANGE: The v1 API has been removed in favor of v2.
```

### Examples

```bash
# Patch release (1.0.0 → 1.0.1)
git commit -m "fix: resolve memory leak in 3D renderer"

# Minor release (1.0.0 → 1.1.0)
git commit -m "feat: add support for FBX file format"

# Major release (1.0.0 → 2.0.0)
git commit -m "feat!: redesign plugin settings UI"
```

## Support

If you find this plugin useful, consider supporting its development:

☕ [Buy Me a Coffee](https://buymeacoffee.com/coterieke)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Links

- [GitHub Repository](https://github.com/iz-ben/3d-model-viewer)
- [Report Issues](https://github.com/iz-ben/3d-model-viewer/issues)
