package com.lewis.imageselect.image

import android.graphics.Point
import android.graphics.PointF

data class ImageCropInfo(val ratio: PointF = PointF(16f, 9f), val maxSize: Point = Point()) {
    fun hasSizeLimit(): Boolean {
        return maxSize.x > 0 && maxSize.y > 0
    }

    fun hasRatioLimit(): Boolean {
        return maxSize.x > 0 && maxSize.y > 0
    }
}

class ImageCompressionInfo() {

}