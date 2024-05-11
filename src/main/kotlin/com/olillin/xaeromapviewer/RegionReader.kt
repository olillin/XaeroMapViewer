package com.olillin.xaeromapviewer

import java.awt.image.BufferedImage

interface RegionReader {
    /** Get all segments. */
    fun getSegments(): List<RegionSegment>

    /** Get a compiled image of all segments. */
    fun getFullImage(): BufferedImage
}