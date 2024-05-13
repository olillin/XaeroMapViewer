package com.olillin.xaeromapviewer

import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

class FolderRegionsReader(val folderPath: Path) {
    val regions: List<FileRegion> =
        folderPath.listDirectoryEntries().filter {
            it.name.matches(Regex("""-?\d+_-?\d+\.xwmc(\.outdated)?"""))
        }.map {
            FileRegion(it.toFile())
        }

    val segments: List<RegionSegment> = regions.flatMap { it.toRegionReader().segments }

    val fullImage: BufferedImage
        get() {
            // Calculate dimensions
            val minX: Int = regions.minOf { it.x }
            val maxX: Int = regions.maxOf { it.x }
            val width = maxX - minX + 1

            val minY: Int = regions.minOf { it.y }
            val maxY: Int = regions.maxOf { it.y }
            val height = maxY - minY + 1


            val imageWidth = width * Region.REGION_IMAGE_SIZE
            val imageHeight = height * Region.REGION_IMAGE_SIZE

            // Create image
            val out = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
            val graphics = out.graphics
            graphics.color = Color.BLACK
            graphics.fillRect(0, 0, imageWidth, imageHeight)

            // Fill regions
            val offsetX = width - maxX
            val offsetY = height - maxY

            regions.forEach { region ->
                val reader = region.toRegionReader()
                val imageX = (region.x + offsetX - 1) * Region.REGION_IMAGE_SIZE
                val imageY = (region.y + offsetY - 1) * Region.REGION_IMAGE_SIZE

                graphics.drawImage(
                    reader.fullImage,
                    imageX,
                    imageY,
                    null,
                )
            }

            return out
        }
}