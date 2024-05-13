package com.olillin.xaeromapviewer

interface Region {
    fun toRegionReader(): RegionReader

    companion object {
        const val SEGMENTS_ACROSS_REGION: Int = 8
        const val REGION_IMAGE_SIZE = RegionSegment.SEGMENT_IMAGE_SIZE * SEGMENTS_ACROSS_REGION
    }
}