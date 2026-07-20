# Changelog

All notable changes to this project will be documented in this file. See [standard-version](https://github.com/conventional-changelog/standard-version) for commit guidelines.

## [1.4.0](https://github.com/iz-ben/3d-model-viewer/compare/v1.3.1...v1.4.0) (2026-07-20)


### Features

* add Model Explorer tool window for glTF/GLB structure ([#49](https://github.com/iz-ben/3d-model-viewer/issues/49)) ([e60abb2](https://github.com/iz-ben/3d-model-viewer/commit/e60abb2b9e26c2a0e9c90237c19c8708147a48f6)), closes [#48](https://github.com/iz-ben/3d-model-viewer/issues/48)
* **plugin:** register STL file type ([#37](https://github.com/iz-ben/3d-model-viewer/issues/37)) ([04287a2](https://github.com/iz-ben/3d-model-viewer/commit/04287a2538c4c2c04b949c48b845a1f33686fe2a)), closes [#23](https://github.com/iz-ben/3d-model-viewer/issues/23) [#7](https://github.com/iz-ben/3d-model-viewer/issues/7) [#27](https://github.com/iz-ben/3d-model-viewer/issues/27)


### Bug Fixes

* **editor:** avoid Structure view disposing a throwaway 3D editor ([#50](https://github.com/iz-ben/3d-model-viewer/issues/50)) ([e863c8d](https://github.com/iz-ben/3d-model-viewer/commit/e863c8de04f95210813fde69d32edb4423285328))
* enable viewer controls for three.js JSON models and cache sniff ([#47](https://github.com/iz-ben/3d-model-viewer/issues/47)) ([5c56d5f](https://github.com/iz-ben/3d-model-viewer/commit/5c56d5f200e955d6c41c306062fc7311468b4760))
* **model-explorer:** guard structure refresh against stale overlapping parses ([#51](https://github.com/iz-ben/3d-model-viewer/issues/51)) ([fe847e9](https://github.com/iz-ben/3d-model-viewer/commit/fe847e95e0744487efc3277dc25a05f523531bd1))
* **status-bar:** hide animation widgets for models without animations ([#54](https://github.com/iz-ben/3d-model-viewer/issues/54)) ([1804643](https://github.com/iz-ben/3d-model-viewer/commit/180464337c9922e2da01acbddaa2f4174c55b9ba))
* **viewer:** center 3D model when Model Explorer pane opens ([#53](https://github.com/iz-ben/3d-model-viewer/issues/53)) ([70ca625](https://github.com/iz-ben/3d-model-viewer/commit/70ca6251105b0e3003b05b4cdc98ce84a9a238cc))
* **viewer:** resolve model assets within the project root ([#44](https://github.com/iz-ben/3d-model-viewer/issues/44)) ([c3444b3](https://github.com/iz-ben/3d-model-viewer/commit/c3444b3adcc3790f2cdd8b9db2e4b101a3c79a8c))

### [1.3.1](https://github.com/iz-ben/3d-model-viewer/compare/v1.3.0...v1.3.1) (2026-07-17)

## [1.3.0](https://github.com/iz-ben/3d-model-viewer/compare/v1.2.0...v1.3.0) (2026-07-17)


### Features

* auto-inject marketplace change notes from CHANGELOG.md ([#35](https://github.com/iz-ben/3d-model-viewer/issues/35)) ([6237384](https://github.com/iz-ben/3d-model-viewer/commit/62373842f5d25ffe00a7067f3a2e2fc6faf31161))


### Bug Fixes

* Fix marketplace compat and gate merges on plugin verification ([#34](https://github.com/iz-ben/3d-model-viewer/issues/34)) ([45f0ef0](https://github.com/iz-ben/3d-model-viewer/commit/45f0ef0ed9c1020f7f5fe7dd7cf75a555f7b9a69))

## [1.2.0](https://github.com/iz-ben/3d-model-viewer/compare/v1.1.0...v1.2.0) (2026-07-16)


### Features

* serverless Threlte viewer with auto-rotate and spec-gloss support ([#22](https://github.com/iz-ben/3d-model-viewer/issues/22)) ([db431ff](https://github.com/iz-ben/3d-model-viewer/commit/db431fffbb68b6146044381f1458b97e2005335c))

## [1.1.0](https://github.com/iz-ben/3d-model-viewer/compare/v1.0.3...v1.1.0) (2026-07-09)


### Features

* highlight glTF materials from the JSON caret/selection ([#21](https://github.com/iz-ben/3d-model-viewer/issues/21)) ([c37a9fa](https://github.com/iz-ben/3d-model-viewer/commit/c37a9fa0cf48d4d88b96862ad23f69a1b6891f40))
* show glTF JSON alongside the 3D model preview ([#20](https://github.com/iz-ben/3d-model-viewer/issues/20)) ([18a1e14](https://github.com/iz-ben/3d-model-viewer/commit/18a1e14faaec6a637b39d0c56d915df3fbacd7b2)), closes [#13](https://github.com/iz-ben/3d-model-viewer/issues/13)

### [1.0.3](https://github.com/iz-ben/3d-model-viewer/compare/v1.0.2...v1.0.3) (2026-07-08)


### Features

* focus panes and animation sync ([#18](https://github.com/iz-ben/3d-model-viewer/issues/18)) ([821154f](https://github.com/iz-ben/3d-model-viewer/commit/821154fccfe14eeec2ca0e9785c0cf33da50e870))


### Bug Fixes

* mac viewer server jvm ([#19](https://github.com/iz-ben/3d-model-viewer/issues/19)) ([93d2621](https://github.com/iz-ben/3d-model-viewer/commit/93d2621fcb56577caed9a0cd06db73e2af79587b)), closes [#16](https://github.com/iz-ben/3d-model-viewer/issues/16)

### [1.0.2](https://github.com/iz-ben/3d-model-viewer/compare/v1.0.1...v1.0.2) (2026-01-26)


### Bug Fixes

* explicitly inlude okhttp as a dependency ([#12](https://github.com/iz-ben/3d-model-viewer/issues/12)) ([35ef319](https://github.com/iz-ben/3d-model-viewer/commit/35ef319767efcf9e503f056060cdac4e465db9b1))

### 1.0.1 (2026-01-26)


### Bug Fixes

* versioning ([#9](https://github.com/iz-ben/3d-model-viewer/issues/9)) ([e3b45e6](https://github.com/iz-ben/3d-model-viewer/commit/e3b45e6ecbfc683bcf6afb62ac4b23edf3f4c322))
* versioning by using standard ([#11](https://github.com/iz-ben/3d-model-viewer/issues/11)) ([6ce2d3b](https://github.com/iz-ben/3d-model-viewer/commit/6ce2d3ba8742088936df4f6ceda808aabb9f32b3))

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

This changelog is automatically generated by the release workflow based on [Conventional Commits](https://www.conventionalcommits.org/).
