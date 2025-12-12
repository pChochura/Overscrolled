# Overscrolled

An implementation of the `OverscrollEffect` that allows the view to be dismissed with a callback for Compose.

![badge-android](https://img.shields.io/badge/android-6EDB8D.svg)
![badge-ios](http://img.shields.io/badge/ios-CDCDCD.svg)
![badge-jvm](http://img.shields.io/badge/jvm-DB413D.svg)
![badge-js](http://img.shields.io/badge/js-F8DB5D.svg)

# Installation

![badge-multiplatform](https://img.shields.io/badge/multiplatform-313131.svg)
[![Sonatype Central](https://maven-badges.sml.io/sonatype-central/io.github.pchochura/overscrolled/badge.svg)](https://maven-badges.sml.io/sonatype-central/io.github.pchochura/overscrolled/)

settings.gradle.kts:
```kotlin
pluginManagement {
  repositories {
    mavenCentral()
  }
}
```

gradle.kts:
```kotlin
dependencies {
  implementation("io.github.pchochura:overscrolled:{LATEST_VERSION}")
}
```

# Usage

The library is an extension over the Compose's `OverscrollEffect` that has an ability to modify the rendering of the view which is being overscrolled as well as invoking a callback whenever the threshold has been met.

```kotlin
// That's just for the user to know whether they reached the threshold
val hapticFeedback = LocalHapticFeedback.current
val state = rememberLazyListState()
LazyRow(
    state = state,
    flingBehavior = rememberSnapFlingBehavior(state),
    overscrollEffect = rememberOverscrolledEffect(
        orientation = Orientation.Horizontal,
        threshold = 100f,
        layerBlock = { progress ->
            // This callback is invoked on every frame with the context of `GraphicsLayerScope`
            // which can be altered depending on the progress of the overscroll.

            alpha = (1 - progress.absoluteValue).coerceAtLeast(0.3f)
            scaleX = 1 - (0.05f * progress.absoluteValue)
            scaleY = 1 - (0.05f * progress.absoluteValue)
            transformOrigin = TransformOrigin(
                pivotFractionX = if (progress > 0) 1f else 0f,
                pivotFractionY = 0.5f,
            )
        },
        onOverscrolled = { finished ->
            // This callback is invoked when the threshold is met. The finished parameter
            // indicates whether the user lifted the finger finishing the overscroll.
            
            if (finished) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                onDismissRequest()
            } else {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            }
        }
    ),
) {
    items(listOf(1, 2, 3)) {
        Text(
            modifier = Modifier.fillParentMaxSize(),
            text = "$it",
        )
    }
}
```
