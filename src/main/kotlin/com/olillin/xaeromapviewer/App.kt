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
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.notExists


class App : Application() {

    private val selectedFileLabel = Label("Selected file")
    private val selectedFileTextField = TextField().apply {
        prefWidth = 400.0
    }

    private val selectedSegmentLabel = Label("Segment")
    private val selectedSegmentTextField = TextField("0")
    private val previousSegmentButton = Button("<").apply {
        setOnAction {
            changeSelectedSegment(-1)
        }
    }
    private val nextSegmentButton = Button(">").apply {
        setOnAction {
            changeSelectedSegment(1)
        }
    }

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

    private val generateAllButton = Button("Generate all").apply {
        setOnAction {
            val selectedFile = Path.of(selectedFileTextField.text)
            generateAll(selectedFile)
        }
    }

    private val generateFullButton = Button("Generate full").apply {
        setOnAction {
            val selectedFile = Path.of(selectedFileTextField.text)
            generateFull(selectedFile)
        }
    }

    private var image: BufferedImage? = null
    private val displayImage = ImageView().apply {
        fitWidth = 512.0
        isSmooth = false
        isPreserveRatio = true
    }

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
                        selectedSegmentLabel,
                        previousSegmentButton,
                        selectedSegmentTextField,
                        nextSegmentButton,
                    )
                },
                HBox().apply {
                    children.addAll(
                        generateButton,
                        saveButton,
                        generateAllButton,
                        generateFullButton,
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
        val selectedSegment = selectedSegmentTextField.text.toInt()

        val file = selectedFile.toFile()
        val reader = FileRegionReader(file)
        val segments = reader.getSegments()

        if (selectedSegment >= segments.size) {
            changeSelectedSegment(segments.size - selectedSegment - 1)
            return
        }

        image = segments[selectedSegment].image

        displayImage.image = scaleImage(image!!, 512.0 / image!!.width).toFX()
    }

    private fun scaleImage(img: BufferedImage, scaleFactor: Double): java.awt.Image = img.getScaledInstance(
        (img.width * scaleFactor).toInt(), (img.height * scaleFactor).toInt(), java.awt.Image.SCALE_DEFAULT
    )

    private fun saveImage(image: BufferedImage, outputDir: Path) {
        if (!outputDir.exists()) outputDir.createDirectories()
        val index: Int = (0..256).first {
            outputDir.resolve("$it.png").notExists()
        }
        val outputFile: File = outputDir.resolve("$index.png").toFile()
        ImageIO.write(image, "png", outputFile)
        println("Saved to ${outputFile.path}")
    }

    private fun generateAll(filePath: Path) {
        val selectedFile = filePath.toFile()
        val reader = FileRegionReader(selectedFile)
        val segments = reader.getSegments()
        segments.forEach { segment ->
            saveImage(segment.image, Path.of("out/all"))
        }
    }

    private fun generateFull(filePath: Path) {
        val selectedFile = filePath.toFile()
        val reader = FileRegionReader(selectedFile)
        image = reader.getFullImage()

        displayImage.image = scaleImage(image!!, 512.0 / image!!.width).toFX()
    }

    private fun changeSelectedSegment(by: Int, refresh: Boolean = true) {
        val currentSegment: Int = selectedSegmentTextField.text.toInt()
        val newSegment: Int = (currentSegment + by).coerceAtLeast(0)

        // Do nothing if the segment hasn't changed
        if (newSegment == currentSegment) return

        selectedSegmentTextField.text = newSegment.toString()

        if (refresh) {
            refreshImage()
        }
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

private fun Int.toByteArray(): ByteArray = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(this).array()