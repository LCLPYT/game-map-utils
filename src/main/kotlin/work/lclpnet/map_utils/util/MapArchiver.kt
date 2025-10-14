package work.lclpnet.map_utils.util

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

class MapArchiver(private val excludeGlobs: Set<String> = emptySet()) {

    fun createArchive(inputDir: Path, outputFile: Path) {
        outputFile.parent.createDirectories()

        outputFile.outputStream().use { fos ->
            XZCompressorOutputStream(fos).use { xzOut ->
                TarArchiveOutputStream(xzOut).use { tarOut ->
                    writeTarEntries(inputDir, tarOut)
                }
            }
        }
    }

    private fun writeTarEntries(
        inputDir: Path,
        tarOut: TarArchiveOutputStream
    ) {
        val matchers = excludeGlobs.map { FileSystems.getDefault().getPathMatcher("glob:$it") }

        Files.walkFileTree(inputDir, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (matchers.any { it.matches(inputDir.relativize(file)) }) {
                    return FileVisitResult.CONTINUE
                }

                val entry = TarArchiveEntry(file.toFile(), inputDir.relativize(file).toString())
                tarOut.putArchiveEntry(entry)

                file.inputStream().use { it.copyTo(tarOut) }

                tarOut.closeArchiveEntry()

                return FileVisitResult.CONTINUE
            }

            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (matchers.any { it.matches(inputDir.relativize(dir)) }) {
                    return FileVisitResult.SKIP_SUBTREE
                }

                if (dir != inputDir) {
                    val entry = TarArchiveEntry(dir.toFile(), inputDir.relativize(dir).toString() + "/")
                    tarOut.putArchiveEntry(entry)
                    tarOut.closeArchiveEntry()
                }

                return FileVisitResult.CONTINUE
            }
        })

        tarOut.finish()
    }
}