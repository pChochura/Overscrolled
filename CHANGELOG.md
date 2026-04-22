# Changelog

All notable changes to this project will be documented in this file.

## [1.1.0] - 2023-10-27

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
