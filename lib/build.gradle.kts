import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "io.github.pchochura.overscrolled"
version = "1.0.0"

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xwhen-guards")
    }

    jvm()
    androidLibrary {
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        namespace = group.toString()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64()

    js {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("io.github.pchochura", "overscrolled", version.toString())

    pom {
        name = "Overscrolled"
        description = "OverscrollEffect with a callback"
        inceptionYear = "2025"
        url = "https://github.com/pChochura/Overscrolled"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/license/mit"
                distribution = "https://opensource.org/license/mit"
            }
        }
        developers {
            developer {
                id = "pChochura"
                name = "Paweł Chochura"
                url = "https://github.com/pChochura"
            }
        }
        scm {
            url = "https://github.com/pChochura/Overscrolled"
            connection = "scm:git:git://github.com/pChochura/Overscrolled.git"
            developerConnection = "scm:git:ssh://git@github.com/pChochura/Overscrolled.git"
        }
    }
}
