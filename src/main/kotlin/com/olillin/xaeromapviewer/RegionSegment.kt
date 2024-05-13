package com.olillin.xaeromapviewer


import java.awt.image.BufferedImage
import java.io.DataInputStream
import java.io.EOFException
import kotlin.experimental.and
import kotlin.math.pow

data class RegionSegment(private val bytes: ByteArray) {
    /** Bytes before the [imageBytes]. Always 10 bytes long. */
    public val headerBytes: ByteArray = bytes.inputStream().readNBytes(HEADER_LENGTH)

    /** Always 16,384 bytes long. */
    public val imageBytes: ByteArray = bytes.inputStream().let {
        it.skipNBytes(HEADER_LENGTH.toLong())
        it.readNBytes(IMAGE_LENGTH)
    }

    /** Remaining bytes after the [imageBytes]. */
    public val extraBytes: ByteArray = bytes.inputStream().let {
        it.skipNBytes((HEADER_LENGTH + IMAGE_LENGTH).toLong())
        it.readAllBytes()
    }

    public val x: Int = headerBytes.let {
        it[0].and(0b11110000.toByte()).div(2.0.pow(4.0)).toInt()
    }
    public val y: Int = headerBytes.let {
        it[0].and(0b00001111).toInt()
    }

    /** Generated image from [imageBytes]. */
    public val image: BufferedImage = DataInputStream(imageBytes.inputStream()).let { dis ->
        val out = BufferedImage(SEGMENT_IMAGE_SIZE, SEGMENT_IMAGE_SIZE, BufferedImage.TYPE_INT_RGB)
        try {
            for (y in 0 until SEGMENT_IMAGE_SIZE) {
                for (x in 0 until SEGMENT_IMAGE_SIZE) {
                    val color: Int = dis.readInt()
                    out.setRGB(x, y, color)
                }
            }
        } catch (_: EOFException) {
            println("WARNING: Reached end of stream while reading image data.")
        }
        out
    }

    companion object {
        const val SEGMENT_IMAGE_SIZE: Int = 64
        const val IMAGE_LENGTH: Int = SEGMENT_IMAGE_SIZE * SEGMENT_IMAGE_SIZE * 4
        const val HEADER_LENGTH: Int = 10
        val HEADER_IDENTIFIER = byteArrayOf(0x00, 0x00, 0x00, -128, 0x58, 0x00, 0x00, 0x40, 0x00)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RegionSegment

        if (!bytes.contentEquals(other.bytes)) return false
        if (image != other.image) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + image.hashCode()
        return result
    }
}
