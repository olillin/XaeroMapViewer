package com.olillin.xaeromapviewer

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.zip.ZipFile

/** Read data from a `.xaero` [file] */
class FileRegionReader(private val file: File) : RegionReader {
    override fun getSegments(): List<RegionSegment> {
        val segments: MutableList<RegionSegment> = mutableListOf()
        val fc = getUnzipped(file).inputStream().channel

        // Skip to first segment
        readUntilNextSegment(fc)

        try {
            while (true) {
                val segmentFile = File.createTempFile("xwmv", ".segment")
                val segmentBuffer = segmentFile.outputStream()

                // Read segment data
                readUntilNextSegment(fc, segmentBuffer)

                // Create segment
                segmentBuffer.flush()
                val fis = segmentFile.inputStream()
                val segment = RegionSegment(fis.readAllBytes())
                fis.close()
                segments.add(segment)
            }
        } catch (_: EOFException) {

        } finally {
            fc.close()
        }

        return segments
    }

    override fun getFullImage(): BufferedImage {
        val imageWidth = SEGMENTS_ACROSS_REGION * RegionSegment.IMAGE_SIZE
        val out = BufferedImage(imageWidth, imageWidth, BufferedImage.TYPE_INT_ARGB)
        val graphics = out.graphics
        graphics.color = Color.BLACK
        graphics.fillRect(0, 0, out.width, out.height)

        val segments = getSegments()
        segments.forEach { segment ->
            println("x: ${segment.x}, y: ${segment.y}")
            graphics.drawImage(
                segment.image,
                segment.x * RegionSegment.IMAGE_SIZE,
                segment.y * RegionSegment.IMAGE_SIZE,
                null,
            )
        }

        return out
    }

    companion object {
        const val SEGMENTS_ACROSS_REGION: Int = 16

        /**
         * Progress [fileChannel] to the beginning of the next segment.
         * Read bytes are written to [outputStream].
         */
        @JvmStatic
        fun readUntilNextSegment(fileChannel: FileChannel, outputStream: OutputStream? = null) {
            // Skip first byte to avoid returning same segment as already on
            val nextByte = ByteBuffer.allocate(1)
            fileChannel.read(nextByte)
            outputStream?.write(nextByte[0].toInt())

            // Fill lastBytes with the first bytes from fileChannel
            val lastBytes = ByteBuffer.allocate(RegionSegment.HEADER_IDENTIFIER.size)
            fileChannel.read(lastBytes)

            while (true) {
                nextByte.clear()
                val bytesRead = fileChannel.read(nextByte)

                // If EOF is reached, break out of the loop
                if (bytesRead == -1) {
                    throw EOFException("Reached end of file before finding next segment.")
                }

                // Add read byte to end of buffer and shift other bytes to fit
                outputStream?.write(lastBytes[0].toInt())
                for (i in 0 until lastBytes.capacity() - 1) {
                    lastBytes.put(i, lastBytes[i + 1])
                }
                lastBytes.put(lastBytes.capacity() - 1, nextByte[0])


                // Check if buffer matches headIdentifier
                val potentialIdentifier = lastBytes.slice(1, RegionSegment.HEADER_IDENTIFIER.size - 1)
                if (potentialIdentifier.array().contentEquals(RegionSegment.HEADER_IDENTIFIER)) {
                    // Go back to start of segment header
                    fileChannel.position(fileChannel.position() - RegionSegment.HEADER_LENGTH)

                    return
                }
            }
        }

        @JvmStatic
        fun getUnzipped(file: File): File {
            // Get zipped data
            val zip = ZipFile(file)
            val entry = zip.entries().nextElement()
            val inputStream = zip.getInputStream(entry)
            // Create new file
            val unzippedFile = File.createTempFile("xwmv", ".xaero")
            val outputStream = unzippedFile.outputStream()
            inputStream.copyTo(outputStream)
            outputStream.flush()

            return unzippedFile
        }
    }
}