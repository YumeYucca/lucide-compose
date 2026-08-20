import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object LucideMetadataGenerator {

    fun iconNameToKotlinName(iconName: String): String =
        iconName
            .trimStart('-', '_')
            .removePrefix("ic_")
            .removePrefix("ic-")
            .replace(Regex("[^a-zA-Z0-9\\-_ ]"), "_")
            .split(Regex("(?<!^)(?=[A-Z])|(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)"))
            .joinToString("") { it.replaceFirstChar(Char::uppercase) }
            .split("_", " ", "-")
            .joinToString("") { it.replaceFirstChar(Char::uppercase) }

    fun generate(
        assetsDir: File,
        srcDir: File,
        basePackage: String,
        chunkSize: Int = 200,
    ) {
        require(assetsDir.isDirectory) { "assetsDir must be a directory: $assetsDir" }
        val packageDir = basePackage.replace('.', '/')
        val outputDir = File(srcDir, packageDir).apply { mkdirs() }
        outputDir.listFiles { file ->
            file.isFile && file.name.endsWith(".generated.kt")
        }?.forEach { it.delete() }

        val entries = mutableListOf<String>()
        val iconNames = mutableListOf<String>()

        assetsDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .sortedBy { it.relativeTo(assetsDir).path }
            .forEach { jsonFile ->
                val iconName = jsonFile.nameWithoutExtension
                val kotlinName = iconNameToKotlinName(iconName)

                val jsonText = jsonFile.readText()
                val json = JSONObject(jsonText)

                val tags = json.optJSONArray("tags")?.toStringList() ?: emptyList()
                val categories = json.optJSONArray("categories")?.toStringList() ?: emptyList()

                fun List<String>.toCodeList(): String =
                    joinToString(", ") { "\"${it.replace("\"", "\\\"")}\"" }

                val entry = buildString {
                    append(
                        """
                        IconMetadata(name = "$kotlinName", tags = listOf(${tags.toCodeList()}), categories = listOf(${categories.toCodeList()}))
                        """.trimIndent()
                    )
                }

                entries += entry
            }

        assetsDir.walkTopDown()
            .filter { it.isFile && it.extension == "svg" }
            .sortedBy { it.relativeTo(assetsDir).path }
            .forEach { svgFile ->
                iconNames += iconNameToKotlinName(svgFile.nameWithoutExtension)
            }

        writeMainMetadataFile(basePackage, outputDir, entries.size, chunkSize, iconNames)
        writeChunkFiles(basePackage, outputDir, entries, chunkSize)
    }

    private fun writeMainMetadataFile(
        basePackage: String,
        outputDir: File,
        totalCount: Int,
        chunkSize: Int,
        iconNames: List<String>,
    ) {
        val chunkCount = (totalCount + chunkSize - 1) / chunkSize
        val mainFile = File(outputDir, "LucideMetadata.generated.kt")

        val imports = buildString {
            for (i in 0 until chunkCount) {
                appendLine("import $basePackage.LucideMetadataChunk$i.chunk as chunk$i")
            }
        }

        val chunksJoin = (0 until chunkCount).joinToString(", ") { "chunk$it" }

        val content = """
            |package $basePackage
            |
            |$imports
            |
            |import androidx.compose.ui.graphics.vector.ImageVector
            |
            |val Lucide.AllIcons: List<ImageVector> by lazy(LazyThreadSafetyMode.NONE) {
            |    listOf(${iconNames.joinToString(", ") { "Lucide.$it" }})
            |}
            |
            |internal val allMetadata: List<IconMetadata> = listOf($chunksJoin).flatten()
            |
        """.trimMargin()

        mainFile.writeText(content)
    }

    private fun writeChunkFiles(
        basePackage: String,
        outputDir: File,
        entries: List<String>,
        chunkSize: Int,
    ) {
        var chunkIndex = 0
        var from = 0

        while (from < entries.size) {
            val to = (from + chunkSize).coerceAtMost(entries.size)
            val chunkEntries = entries.subList(from, to)

            val file = File(outputDir, "LucideMetadataChunk$chunkIndex.generated.kt")

            val body = chunkEntries.joinToString(", ") { it }

            val content = """
                |package $basePackage
                |
                |object LucideMetadataChunk$chunkIndex {
                |    val chunk: List<IconMetadata> = listOf($body)
                |}
                |
            """.trimMargin()

            file.writeText(content)

            from = to
            chunkIndex++
        }
    }

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }
}
