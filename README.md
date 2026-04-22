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

# Example app

The app implements a `LazyRow` with elements behaving like a pager with a dismiss action whenever the user scrolls past the edge.

https://github.com/user-attachments/assets/68df46f0-df54-4b90-ba2d-900a663de364

# Usage

The library is an extension over the Compose's `OverscrollEffect` that has an ability to modify the rendering of the view which is being overscrolled as well as invoking a callback whenever the threshold has been met.

## Simple usage

If you only need a callback when the overscroll threshold is met, you can use the simplest version of the effect.

```kotlin
val hapticFeedback = LocalHapticFeedback.current
LazyRow(
    overscrollEffect = rememberHorizonalOverscrolledEffect(
        threshold = 100f,
        onOverscrolled = { finished ->
            if (finished) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                onDismissRequest()
            }
        }
    ),
) {
    // ...
}
```

## Custom overscroll effect

For more control over how the content looks while being overscrolled, you can provide a custom `effectNode`. You can also define asymmetrical thresholds for the start and end sides.

```kotlin
val startThreshold = 100f
val endThreshold = 200f

LazyRow(
    overscrollEffect = rememberHorizonalOverscrolledEffect(
        startThreshold = startThreshold,
        endThreshold = endThreshold,
        onOverscrolled = { finished -> 
            /* ... */ 
        },
        effectNode = remember(startThreshold, endThreshold) {
            object : OverscrolledEffectNode {
                override fun node(currentOffset: () -> Offset): Modifier.Node {
                    return object : Modifier.Node(), LayoutModifierNode {
                        override fun MeasureScope.measure(
                            measurable: Measurable,
                            constraints: Constraints,
                        ): MeasureResult {
                            val placeable = measurable.measure(constraints)
                            return layout(placeable.width, placeable.height) {
                                val offsetValue = currentOffset()
                                val threshold =
                                    if (offsetValue.x < 0) endThreshold else startThreshold
                                val progress = offsetValue.x / threshold
                                placeable.placeWithLayer(
                                    offsetValue.x.roundToInt(),
                                    offsetValue.y.roundToInt(),
                                    layerBlock = {
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
                                )
                            }
                        }
                    }
                }
            }
        }
    ),
) {
    // ...
}
```
