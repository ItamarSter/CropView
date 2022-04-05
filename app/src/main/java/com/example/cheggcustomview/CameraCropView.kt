package com.example.cheggcustomview

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.annotation.StringRes

class CameraCropView(
    context: Context,
    private val attrs: AttributeSet,
) : View(
    context,
    attrs
) {
    private val halfScreenWidth = Resources.getSystem().displayMetrics.widthPixels / 2
    private val halfScreenHeight = Resources.getSystem().displayMetrics.heightPixels / 2

    private val cornerBitmapStartTop: Bitmap =
        getBitmapAttr("startTopIcon", R.drawable.corner_start_top)
    private val cornerBitmapEndTop: Bitmap = getBitmapAttr("endTopIcon", R.drawable.corner_end_top)
    private val cornerBitmapStartBottom: Bitmap =
        getBitmapAttr("startBottomIcon", R.drawable.corner_start_bottom)
    private val cornerBitmapEndBottom: Bitmap =
        getBitmapAttr("endBottomIcon", R.drawable.corner_end_bottom)

    private var cropWidth = getCropDimension("initialWidthCrop")
    private var cropHeight = getCropDimension("initialHeightCrop")

    private val minWidth = getCropDimension("minWidthCrop")
    private val minHeight = getCropDimension("minHeightCrop")

    private val cropMarginTop = getCropDimension("cropMarginTop")

    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        //Transparent
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    var startTouchPointX = 0F
    var startTouchPointY = 0F

    var lastX: Float = 0F
    var lastY: Float = 0F

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.apply {
            //Camera rect:
            drawRect(
                (right - cropWidth.toFloat()) / 2,
                (bottom - cropHeight.toFloat()) / 2,
                cropWidth + (right - cropWidth.toFloat()) / 2,
                cropHeight + (bottom - cropHeight.toFloat()) / 2,
                cropPaint
            )
            //4 corner bitmaps:
            drawBitmap(
                cornerBitmapStartTop,
                (right - cropWidth.toFloat()) / 2 - cornerBitmapStartTop.width / 7,
                (bottom - cropHeight.toFloat()) / 2 - cornerBitmapStartTop.height / 7,
                cornerPaint
            )
            drawBitmap(
                cornerBitmapEndTop,
                cropWidth + (right - cropWidth.toFloat()) / 2 - cornerBitmapEndTop.width / 8f * 7,
                (bottom - cropHeight.toFloat()) / 2 - cornerBitmapEndTop.height / 7,
                cornerPaint
            )
            drawBitmap(
                cornerBitmapStartBottom,
                (right - cropWidth.toFloat()) / 2 - cornerBitmapStartBottom.width / 7,
                cropHeight + (bottom - cropHeight.toFloat()) / 2 - cornerBitmapStartBottom.height / 8f * 7,
                cornerPaint
            )
            drawBitmap(
                cornerBitmapEndBottom,
                cropWidth + (right - cropWidth.toFloat()) / 2 - cornerBitmapEndBottom.width / 8f * 7,
                cropHeight + (bottom - cropHeight.toFloat()) / 2 - cornerBitmapEndBottom.height / 8f * 7,
                cornerPaint
            )

        }
    }

    private var growth = 0

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return detector.onTouchEvent(event).let { result ->
            if (!result) {
                when (event?.action) {
                    MotionEvent.ACTION_MOVE -> {
                        //Check of lastX and lastY is for prevent growth immediately after the touch - but only after touch and move.
                        //For this lastX and lastY are reset immediately after each start of touch - and receive values after moving started.
                        if (lastX != 0F && lastY != 0F) {
                            //Check in which quarter of the screen the user started the movement. Each quarter perform growth in its direction.
                            if (startTouchPointX >= halfScreenWidth && startTouchPointY >= halfScreenHeight) {
                                //Width growth:
                                growth = (event.x - lastX).toInt()
                                //Check if the growth going to over the min/max size: yes - set to the size the min/max size. no - do the growth.
                                if (cropWidth + growth < minWidth) cropWidth = minWidth
                                if (cropWidth + growth > right) cropWidth = right
                                else cropWidth += growth

                                //Height growth:
                                growth = (event.y - lastY).toInt()
                                if (cropHeight + growth < minHeight) cropHeight = minHeight
                                if (cropHeight + growth > bottom) cropHeight = bottom
                                else cropHeight += growth

                            } else if (startTouchPointX < halfScreenWidth && startTouchPointY < halfScreenHeight) {
                                growth = (event.x - lastX).toInt()
                                if (cropWidth - growth < minWidth) cropWidth = minWidth
                                if (cropWidth - growth > right) cropWidth = right
                                else cropWidth -= growth

                                growth = (event.y - lastY).toInt()
                                if (cropHeight - growth < minHeight) cropHeight = minHeight
                                if (cropHeight - growth > bottom) cropHeight = bottom
                                else cropHeight -= growth

                            } else if (startTouchPointX < halfScreenWidth && startTouchPointY >= halfScreenHeight) {
                                growth = (event.x - lastX).toInt()
                                if (cropWidth - growth < minWidth) cropWidth = minWidth
                                if (cropWidth - growth > right) cropWidth = right
                                else cropWidth -= growth

                                growth = (event.y - lastY).toInt()
                                if (cropHeight + growth < minHeight) cropHeight = minHeight
                                if (cropHeight + growth > bottom) cropHeight = bottom
                                else cropHeight += growth

                            } else {
                                growth = (event.x - lastX).toInt()
                                if (cropWidth + growth < minWidth) cropWidth = minWidth
                                if (cropWidth + growth > right) cropWidth = right
                                else cropWidth += growth

                                growth = (event.y - lastY).toInt()
                                if (cropHeight - growth < minHeight) cropHeight = minHeight
                                if (cropHeight - growth > bottom) cropHeight = bottom
                                else cropHeight -= growth
                            }
                            invalidate()
                        }
                        lastX = event.x
                        lastY = event.y
                        true
                    }
                    else -> {
                        true
                    }
                }
            } else true
        }
    }

    private val touchListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            startTouchPointX = e.x
            startTouchPointY = e.y
            lastX = 0F
            lastY = 0F
            return true
        }
    }

    private val detector: GestureDetector = GestureDetector(context, touchListener)

    private fun getCropDimension(attr: String): Int {
        val userInput = attrs.getAttributeValue("http://schemas.android.com/apk/res-auto", attr)
        return (userInput?.substring(0, userInput.indexOf("."))?.toInt() ?: 200).dp()
    }

    private fun getBitmapAttr(attr: String, defBitmap: Int): Bitmap {
        val userInput = attrs.getAttributeResourceValue(
            "http://schemas.android.com/apk/res-auto",
            attr,
            defBitmap
        )

        return try {
            BitmapFactory.decodeResource(resources, userInput)
        } catch (e: Throwable) {
            BitmapFactory.decodeResource(resources, defBitmap)
        }
    }

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()
}