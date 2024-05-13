package com.olillin.xaeromapviewer

import java.awt.image.BufferedImage

interface RegionReader {
    /** The segments in the region. */
    val segments: List<RegionSegment>

    /** A compiled image of all segments. */
    val fullImage: BufferedImage
}