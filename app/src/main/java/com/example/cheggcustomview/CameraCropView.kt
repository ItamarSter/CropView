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
    private val halfScreenWidth = Resources.getSystem().displayMetrics.widthPixels / 2
    private val halfScreenHeight = Resources.getSystem().displayMetrics.heightPixels / 2

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

    private var cropMarginTop = getCropDimension("cropMarginTop", 200f)

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
                cropMarginTop.toFloat(),
                cropWidth + (right - cropWidth.toFloat()) / 2,
                cropHeight + cropMarginTop.toFloat(),
                cropPaint
            )
            //4 corner bitmaps:
            drawBitmap(
                cornerBitmapStartTop,
                (right - cropWidth.toFloat()) / 2 - cornerBitmapStartTop.width / 7,
                cropMarginTop.toFloat() - cornerBitmapStartTop.height / 7,
                cornerPaint
            )
            drawBitmap(
                cornerBitmapEndTop,
                cropWidth + (right - cropWidth.toFloat()) / 2 - cornerBitmapEndTop.width / 8f * 7,
                cropMarginTop.toFloat() - cornerBitmapEndTop.height / 7,
                cornerPaint
            )
            drawBitmap(
                cornerBitmapStartBottom,
                (right - cropWidth.toFloat()) / 2 - cornerBitmapStartBottom.width / 7,
                cropHeight + cropMarginTop - cornerBitmapStartBottom.height / 8f * 7,
                cornerPaint
            )
            drawBitmap(
                cornerBitmapEndBottom,
                cropWidth + (right - cropWidth.toFloat()) / 2 - cornerBitmapEndBottom.width / 8f * 7,
                cropHeight + cropMarginTop - cornerBitmapEndBottom.height / 8f * 7,
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
                            resizing =
                                if (startTouchPointX >= halfScreenWidth) event.x - lastX
                                else lastX - event.x

                            //Check if the resizing going to over the min/max size: yes - set to the size the min/max size. no - do the resizing.
                            if (cropWidth + resizing < minWidth) cropWidth = minWidth
                            else if (cropWidth + resizing < right) cropWidth += resizing


                            //Height resizing:
                            resizing = (event.y - lastY)
                            if (startTouchPointY >= cropMarginTop + cropHeight / 2) {
                                when {
                                    //Reaching the min:
                                    cropHeight + resizing < minHeight -> {
                                        cropMarginTop += (cropHeight - minHeight) / 2
                                        cropHeight = minHeight
                                    }
                                    //Reaching the top of screen:
                                    cropMarginTop - resizing / 2 < 0 -> {
//                                        cropHeight += cropMarginTop * 2
//                                        cropMarginTop = 0f
                                    }
                                    //Usual resizing:
                                    else -> {
                                        cropHeight += resizing
                                        cropMarginTop -= resizing / 2
                                    }
                                }
                            } else {
                                when {
                                    cropHeight - resizing < minHeight -> {
                                        cropMarginTop += (cropHeight - minHeight) / 2
                                        cropHeight = minHeight
                                    }
                                    cropMarginTop + resizing / 2 < 0 -> {
//                                        cropHeight += cropMarginTop * 2
//                                        cropMarginTop = 0f
                                    }
                                    else -> {
                                        cropHeight -= resizing
                                        cropMarginTop += resizing / 2
                                    }
                                }
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