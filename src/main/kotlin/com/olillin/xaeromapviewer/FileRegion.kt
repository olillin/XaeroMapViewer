package com.olillin.xaeromapviewer

import java.io.File

data class FileRegion(val file: File) : Region {
    val x: Int = file.name.split(".")[0].split("_")[0].toInt()
    val y: Int = file.name.split(".")[0].split("_")[1].toInt()

    override fun toRegionReader() = FileRegionReader(this)
}