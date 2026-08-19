# Lucide Compose

Kotlin Multiplatform Compose bindings for [Lucide Icons](https://lucide.dev/).
The library supports Android, Desktop/JVM, JavaScript, WebAssembly, iOS, and
macOS targets.

## Install

The package is published to GitHub Packages with the fixed snapshot version
`1.0.0-SNAPSHOT`.

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/YumeYucca/lucide-compose")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("io.github.YumeYucca:lucide-compose:1.0.0-SNAPSHOT")
}
```

For a public package, GitHub Packages still requires authentication when
resolving Maven artifacts. Use a GitHub personal access token with
`read:packages`, supplied as `gpr.key` or `GITHUB_TOKEN`.

## Use icons

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.LucideRegistry

@Composable
fun IconExamples() {
    Image(
        painter = rememberVectorPainter(Lucide.Info),
        contentDescription = "Information",
    )

    LucideRegistry.searchIcon("building").forEach { icon ->
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = null,
        )
    }
}
```

The icon vectors are standard Compose `ImageVector` values, so they work with
`Image` and any custom Compose UI component.
