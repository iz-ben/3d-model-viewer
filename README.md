# 3D Model Viewer

View and inspect 3D models directly in your IntelliJ-based IDE.

## Features

- View 3D models directly in the editor
- Wireframe mode toggle via status bar
- Animation playback controls (play/pause)
- Animation selector for models with multiple animations

## Supported File Formats

- **GLB** — Built-in support, works out of the box
- **GLTF** — Built-in support, automatically bundles referenced assets (textures, binary files)
  > ⚠️ GLTF support has limited handling for non-self-contained files; external textures may fail to show.
- **OBJ** — Optional support, enable in **Settings → Tools → 3D Model Viewer**
  > ⚠️ OBJ support is opt-in because other plugins may also register the `.obj` file extension. Enabling this feature is left to your discretion to avoid potential conflicts. Requires IDE restart after enabling.

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

1. Open any supported 3D model file (`.glb`, `.gltf`, or `.obj` if enabled)
2. The model will render in the editor tab
3. Use the status bar widgets at the bottom to:
   - Toggle **Wireframe** mode
   - **Play/Pause** animations
   - Select animations

## Screenshots & Videos

<!-- TODO: Add screenshots and video links here -->
<!-- 
![Screenshot 1](URL_TO_SCREENSHOT_1)
![Screenshot 2](URL_TO_SCREENSHOT_2)

[Watch Demo Video](URL_TO_VIDEO)
-->

## Support

If you find this plugin useful, consider supporting its development:

☕ [Buy Me a Coffee](https://buymeacoffee.com/coterieke)

## Links

- [GitHub Repository](https://github.com/iz-ben/3d-model-viewer)
- [Report Issues](https://github.com/iz-ben/3d-model-viewer/issues)
