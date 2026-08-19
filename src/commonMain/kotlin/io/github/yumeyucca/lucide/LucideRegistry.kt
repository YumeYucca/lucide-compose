package io.github.yumeyucca.lucide

import androidx.compose.ui.graphics.vector.ImageVector
import io.github.yumeyucca.lucide.lucide.SquareSlash

data class IconMetadata(
    val name: String,
    val tags: List<String>,
    val categories: List<String>,
) {
    val icon: ImageVector by lazy {
        Lucide.AllIcons.find { it.name == name } ?: Lucide.SquareSlash
    }
}

object LucideRegistry {
    val allMeta: List<IconMetadata> by lazy { allMetadata }

    val metaByName: Map<String, IconMetadata> by lazy {
        allMeta.associateBy(IconMetadata::name)
    }

    fun metaByTag(query: String): List<IconMetadata> =
        allMeta.filter { metadata ->
            metadata.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }

    fun metaByCategory(query: String): List<IconMetadata> =
        allMeta.filter { metadata ->
            metadata.categories.any { category -> category.contains(query, ignoreCase = true) }
        }

    fun metaByName(query: String): List<IconMetadata> =
        allMeta.filter { metadata -> metadata.name.contains(query, ignoreCase = true) }

    fun searchMeta(query: String): List<IconMetadata> =
        allMeta.filter { metadata ->
            metadata.name.contains(query, ignoreCase = true) ||
                metadata.tags.any { tag -> tag.contains(query, ignoreCase = true) } ||
                metadata.categories.any { category -> category.contains(query, ignoreCase = true) }
        }

    fun getMetadata(name: String): IconMetadata? = metaByName[name]

    fun iconByTag(query: String): List<ImageVector> = metaByTag(query).map(IconMetadata::icon)

    fun iconByCategory(query: String): List<ImageVector> =
        metaByCategory(query).map(IconMetadata::icon)

    fun iconByName(query: String): List<ImageVector> =
        metaByName(query).map(IconMetadata::icon)

    fun searchIcon(query: String): List<ImageVector> = searchMeta(query).map(IconMetadata::icon)
}
