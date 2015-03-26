package org.jetbrains.protocolReader

import gnu.trove.THashSet
import gnu.trove.TObjectProcedure
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * Records a list of files in the root directory and deletes files that were not re-generated.
 */
class FileSet(private val rootDir: Path) {
  SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private val unusedFiles: THashSet<Path>

  init {
    unusedFiles = THashSet<Path>()
    Files.walkFileTree(rootDir, object : SimpleFileVisitor<Path>() {
      override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
        return if (Files.isHidden(dir)) FileVisitResult.SKIP_SUBTREE else FileVisitResult.CONTINUE
      }

      override fun visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (!Files.isHidden(path)) {
          unusedFiles.add(path)
        }
        return FileVisitResult.CONTINUE
      }
    })
  }

  fun createFileUpdater(filePath: String): FileUpdater {
    val file = rootDir.resolve(filePath)
    unusedFiles.remove(file)
    return FileUpdater(file)
  }

  fun deleteOtherFiles() {
    unusedFiles.forEach(object : TObjectProcedure<Path> {
      override fun execute(path: Path): Boolean {
        if (Files.deleteIfExists(path)) {
          val parent = path.getParent()
          Files.newDirectoryStream(parent).use { stream ->
            if (!stream.iterator().hasNext()) {
              Files.delete(parent)
            }
          }
        }
        return true
      }
    })
  }
}
