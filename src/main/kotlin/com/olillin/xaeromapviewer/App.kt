package com.olillin.xaeromapviewer

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import java.awt.image.BufferedImage
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.notExists


class App : Application() {

    private val selectedFileLabel = Label("Selected file")
    private val selectedFileTextField = TextField().apply {
        prefWidth = 400.0
    }

    private val skipBytesLabel = Label("Skip bytes")
    private val skipBytesTextField = TextField("0")
    private val widthLabel = Label("Width")
    private val widthTextField = TextField("64")
    private val heightLabel = Label("Height")
    private val heightTextField = TextField("64")

    private val generateButton = Button("Generate").apply {
        setOnAction {
            refreshImage()
        }
    }

    private val saveButton = Button("Save image").apply {
        setOnAction {
            if (image != null) {
                saveImage(image!!, Path.of("out"))
            }
        }
    }

    private val generateAllButton = Button("GENERATE ALL").apply {
        setOnAction {
            val selectedFile = Path.of(selectedFileTextField.text)
            val width = widthTextField.text.toInt()
            val height = heightTextField.text.toInt()
            generateAll(selectedFile, width, height)
        }
    }

    private val nextByteButton = Button(">").apply {
        setOnAction {
            changeOffset(1)
        }
    }
    private val nextPixelButton = Button(">>").apply {
        setOnAction {
            changeOffset(PIXEL_SIZE)
        }
    }
    private val nextRowButton = Button("vv").apply {
        setOnAction {
            val imageWidth = widthTextField.text.toInt()
            changeOffset(PIXEL_SIZE * imageWidth)
        }
    }
    private val nextSlideButton = Button("2v").apply {
        setOnAction {
            changeOffset(imageSize)
        }
    }
    private val previousByteButton = Button("<").apply {
        setOnAction {
            changeOffset(-1)
        }
    }
    private val previousPixelButton = Button("<<").apply {
        setOnAction {
            changeOffset(-PIXEL_SIZE)
        }
    }
    private val previousRowButton = Button("^^").apply {
        setOnAction {
            val imageWidth = widthTextField.text.toInt()
            changeOffset(-PIXEL_SIZE * imageWidth)
        }
    }
    private val previousSlideButton = Button("2^").apply {
        setOnAction {
            changeOffset(-imageSize)
        }
    }

    private var image: BufferedImage? = null
    private val displayImage = ImageView().apply {
        fitWidth = 512.0
        isSmooth = false
        isPreserveRatio = true
    }

    private val imageSize: Long = PIXEL_SIZE * widthTextField.text.toInt() * heightTextField.text.toInt()

    override fun start(stage: Stage) {
        stage.title = "XaeroMapViewer"

        val layout = VBox().apply {
            children.addAll(
                HBox().apply {
                    children.addAll(
                        selectedFileLabel,
                        selectedFileTextField,
                    )
                },
                HBox().apply {
                    children.addAll(
                        VBox().apply {
                            children.addAll(
                                widthLabel,
                                widthTextField,
                            )
                        },
                        VBox().apply {
                            children.addAll(
                                heightLabel,
                                heightTextField,
                            )
                        },
                        VBox().apply {
                            children.addAll(
                                skipBytesLabel,
                                skipBytesTextField,
                            )
                        },
                    )
                },
                HBox().apply {
                    children.addAll(
                        generateButton,
                        saveButton,
                        generateAllButton,
                    )
                },
                HBox().apply {
                    children.addAll(
                        previousByteButton,
                        nextByteButton,
                        previousPixelButton,
                        nextPixelButton,
                        previousRowButton,
                        nextRowButton,
                        previousSlideButton,
                        nextSlideButton,
                    )
                },
                displayImage,
            )
        }
        stage.scene = Scene(layout, 550.0, 700.0)
        stage.show()
    }

    private fun refreshImage() {
        val selectedFile = Path.of(selectedFileTextField.text)
        val skipBytes = skipBytesTextField.text.toLong()
        val width = widthTextField.text.toInt()
        val height = heightTextField.text.toInt()

        image = generateImage(
            selectedFile,
            skipBytes,
            width,
            height,
        )

        displayImage.image = scaleImage(image!!, 512.0 / image!!.width).toFX()
    }

    private fun generateImage(filePath: Path, skipBytes: Long, width: Int, height: Int): BufferedImage {
        val dataInput = DataInputStream(FileInputStream(filePath.toFile()))
        dataInput.skipNBytes(skipBytes)

        val image = generateImage(dataInput, width, height)

        return image
    }

    private fun generateImage(dataInput: DataInputStream, width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        try {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val color: Int = dataInput.readInt()
                    image.setRGB(x, y, color)
                }
            }
        } catch (_: EOFException) {}
        return image
    }

    private fun scaleImage(img: BufferedImage, scaleFactor: Double): java.awt.Image =
        img.getScaledInstance(
            (img.width * scaleFactor).toInt(),
            (img.height * scaleFactor).toInt(),
            java.awt.Image.SCALE_DEFAULT
        )

    private fun saveImage(image: BufferedImage, outputDir: Path) {
        if (!outputDir.exists())
            outputDir.createDirectories()
        val index: Int = (0..256).first {
            outputDir.resolve("$it.png").notExists()
        }
        val outputFile: File = outputDir.resolve("$index.png").toFile()
        ImageIO.write(image, "png", outputFile)
        println("Saved to ${outputFile.path}")
    }

    private fun generateAll(filePath: Path, width: Int, height: Int) {
        val dataInputStream = DataInputStream(FileInputStream(filePath.toFile()))
        val headIdentifier = byteArrayOf(0x00, 0x00, 0x00, -128, 0x58, 0x00, 0x00, 0x40, 0x00)
        val lastBytes = ByteArray(headIdentifier.size)

        var position: Long = 0
        dataInputStream.use { dis ->
            while (true) {
                val nextByte = dis.read()
                position++

                // If EOF is reached, break out of the loop
                if (nextByte == -1) {
                    println("EOF reached.")
                    break
                }

                // Slide the window by one byte
                for (i in 0 until lastBytes.size - 1) {
                    lastBytes[i] = lastBytes[i + 1]
                }

                // Put next byte into the last position of the buffer
                lastBytes[lastBytes.size - 1] = nextByte.toByte()

                // Check if buffer matches headIdentifier
                if (lastBytes.contentEquals(headIdentifier)) {
                    println("Found image at $position")
                    // Generate image
                    val image = generateImage(dis, width, height)
                    position += imageSize
                    saveImage(image, Path.of("out/all"))
                }
            }
        }
    }

    private fun changeOffset(by: Long, refresh: Boolean = true) {
        val currentOffset: Long = skipBytesTextField.text.toLong()
        val fileSize = Path.of(selectedFileTextField.text).fileSize()
        val newOffset: Long = (currentOffset + by).coerceIn(0, fileSize - imageSize)
        skipBytesTextField.text = newOffset.toString()

        if (refresh)
            refreshImage()
    }

    companion object {
        const val PIXEL_SIZE: Long = 4
    }
}

fun main() {
    Application.launch(App::class.java)
}

fun java.awt.Image.toFX(): Image {
    val width = getWidth(null)
    val height = getHeight(null)
    // Convert to BufferedImage
    val buffered = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val graphics = buffered.createGraphics()
    graphics.drawImage(this, 0, 0, null)
    graphics.dispose()
    // Copy pixels
    val out = WritableImage(width, height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel: Int = buffered.getRGB(x, y)
            val bytes: ByteArray = pixel.toByteArray()
            val r: Int = bytes[1].toUByte().toInt().coerceIn(0, 255)
            val g: Int = bytes[2].toUByte().toInt().coerceIn(0, 255)
            val b: Int = bytes[3].toUByte().toInt().coerceIn(0, 255)
            val color: Color = Color.rgb(r, g, b)
            out.pixelWriter.setColor(x, y, color)
        }
    }
    return out
}

private fun Int.toByteArray(): ByteArray =
    ByteBuffer.allocate(Int.SIZE_BYTES).putInt(this).array()