# Changelog

All notable changes to this project will be documented in this file.

## [1.2.1] - 2026-04-23

### ⚠️ Breaking Changes
- **onOverscrolled**: The `onOverscrolled` method now receives `finished: Boolean, direction: Direction` instead of `finished: Boolean`. 

### 🛠 Bug Fixes
- **Direction mix-up**: `Direction` enum is correctly calculated for the `OverscrolledEffectNode`

## [1.2.0] - 2026-04-23

### ⚠️ Breaking Changes
- **OverscrolledEffectNode**: The `node` method now receives `currentProgress: () -> OverscrolledProgress` instead of `currentOffset: () -> Offset`.
- **OverscrolledProgress**: Introduced a new data class that holds `absoluteOffset`, `progress` (0..1), and `direction` (`FromStart` or `FromEnd`).
- **createOverscrolledEffectNode**: Updated to accept a lambda with `currentProgress: () -> OverscrolledProgress`.

### 🚀 Features
- **onProgressChanged**: Added a new optional callback to `rememberHorizonalOverscrolledEffect` and `rememberVerticalOverscrolledEffect` to observe overscroll progress changes.

## [1.1.0] - 2026-04-22

### 🚀 Features
- **Asymmetrical Thresholds**: Added support for separate start and end overscroll thresholds in `rememberHorizonalOverscrolledEffect` and `rememberVerticalOverscrolledEffect`.
- **Custom Effect Nodes**: Introduced `OverscrolledEffectNode` interface, providing a way to define custom transformations for the overscrolled content.
- **Orientation Helpers**: Added `rememberHorizonalOverscrolledEffect` and `rememberVerticalOverscrolledEffect` convenience methods.

### 🛠 Refactoring & Improvements
- **Improved API**: Deprecated the generic `rememberOverscrolledEffect` to encourage the use of orientation-specific helpers.
- **Documentation**: Added comprehensive KDoc documentation for all public APIs and interfaces.
- **Internal Cleanup**: Refactored internal overscroll logic for better maintainability and performance.

### ⚠️ Deprecations
- `rememberOverscrolledEffect` has been deprecated. Use `rememberHorizonalOverscrolledEffect` or `rememberVerticalOverscrolledEffect` instead.
