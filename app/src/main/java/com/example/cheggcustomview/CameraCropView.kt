package com.example.cheggcustomview

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

class CameraCropView(
    context: Context,
    private val attrs: AttributeSet,
) : View(
    context,
    attrs
) {
    var cropInCenterVertically = false

    private val halfLayoutWidth get() = width / 2
    private val halfLayoutHeight get() = height / 2

    private val cornerBitmapStartTop: Bitmap =
        getBitmapAttr("startTopIcon", R.drawable.corner_start_top)
    private val cornerBitmapEndTop: Bitmap = getBitmapAttr("endTopIcon", R.drawable.corner_end_top)
    private val cornerBitmapStartBottom: Bitmap =
        getBitmapAttr("startBottomIcon", R.drawable.corner_start_bottom)
    private val cornerBitmapEndBottom: Bitmap =
        getBitmapAttr("endBottomIcon", R.drawable.corner_end_bottom)

    private var cropWidth = getCropDimension("initialWidthCrop", 200f)
    private var cropHeight = getCropDimension("initialHeightCrop", 200f)

    private val minWidth = getCropDimension("minWidthCrop", 200f)
    private val minHeight = getCropDimension("minHeightCrop", 200f)

    private var cropMarginTop = getCropMarginTop()

    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        //Transparent
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    var startTouchPointX = 0F
    var startTouchPointY = 0F

    var lastX: Float = 0F
    var lastY: Float = 0F

    init {
        if (cropMarginTop == -1f) cropInCenterVertically = true
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.apply {
            //Camera rect:
            drawRect(
                (width - cropWidth) / 2,
                if(cropInCenterVertically) (height - cropHeight) / 2 else cropMarginTop,
                cropWidth + (width - cropWidth) / 2,
                if (cropInCenterVertically) (height - cropHeight) / 2 + cropHeight else cropHeight + cropMarginTop,
                cropPaint
            )
            //4 corner bitmaps:
            drawBitmap(
                cornerBitmapStartTop,
                (width - cropWidth) / 2 - cornerBitmapStartTop.width / 7,
                (if (cropInCenterVertically) (height - cropHeight) / 2 else cropMarginTop) - cornerBitmapStartTop.height / 7,
                cornerPaint
            )
            drawBitmap(
                cornerBitmapEndTop,
                cropWidth + (width - cropWidth) / 2 - cornerBitmapEndTop.width / 8f * 7,
                (if (cropInCenterVertically) (height - cropHeight) / 2 else cropMarginTop) - cornerBitmapEndTop.height / 7,
                cornerPaint
            )
            drawBitmap(
                cornerBitmapStartBottom,
                (width - cropWidth) / 2 - cornerBitmapStartBottom.width / 7,
                (if (cropInCenterVertically) (height - cropHeight) / 2 + cropHeight else cropHeight + cropMarginTop) - cornerBitmapStartBottom.height / 8f * 7,
                cornerPaint
            )
            drawBitmap(
                cornerBitmapEndBottom,
                cropWidth + (width - cropWidth) / 2 - cornerBitmapEndBottom.width / 8f * 7,
                (if (cropInCenterVertically) (height - cropHeight) / 2 + cropHeight else cropHeight + cropMarginTop) - cornerBitmapEndBottom.height / 8f * 7,
                cornerPaint
            )

        }
    }

    private var resizing = 0f

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return detector.onTouchEvent(event).let { result ->
            if (!result) {
                when (event?.action) {
                    MotionEvent.ACTION_MOVE -> {
                        //Check of lastX and lastY is for prevent resizing immediately after the touch - but only after touch and move.
                        //For this lastX and lastY are reset immediately after each start of touch - and receive values after moving started.
                        if (lastX != 0F && lastY != 0F) {
                            //Check in which half of the screen the user started the movement. Each half perform resizing in its direction.

                            //Width resizing:
                            resizing = 2 *
                                if (startTouchPointX >= halfLayoutWidth) event.x - lastX
                                else lastX - event.x

                            //Check if the resizing going to over the min/max size: yes - set to the size the min/max size. no - do the resizing.
                            if (cropWidth + resizing < minWidth) cropWidth = minWidth
                            else if (cropWidth + resizing < width) cropWidth += resizing


                            //Height resizing:
                            resizing = 2 *
                                    if (cropInCenterVertically){
                                        if (startTouchPointY >= halfLayoutHeight) event.y - lastY
                                        else lastY - event.y
                                    } else {
                                        if (startTouchPointY >= cropMarginTop + cropHeight / 2) event.y - lastY
                                        else lastY - event.y
                                    }


                            if (cropHeight + resizing < minHeight) {
                                cropMarginTop += (cropHeight - minHeight) / 2
                                cropHeight = minHeight
                            } else if ((!cropInCenterVertically) && (cropMarginTop - resizing / 2 > 0  && resizing / 2 + cropMarginTop + cropHeight < height) || ((cropInCenterVertically && cropHeight + resizing < height)) ) {
                                cropHeight += resizing
                                cropMarginTop -= resizing / 2
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

    private fun getCropDimension(attr: String, defValue: Float): Float {
        val userInput = attrs.getAttributeValue("http://schemas.android.com/apk/res-auto", attr)
        return (userInput?.substring(0, userInput.indexOf("."))?.toFloat() ?: defValue).dp()
    }

    private fun getCropMarginTop(): Float {
        val userInput = attrs.getAttributeValue("http://schemas.android.com/apk/res-auto", "cropMarginTop")
            ?: return -1f
        return (userInput.substring(0, userInput.indexOf(".")).toFloat()).dp()
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

    private fun Float.dp() = (this * resources.displayMetrics.density)
}