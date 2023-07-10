package com.satverse.drawing

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var mDrawPath: CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Float = 0f
    private var color = Color.BLACK
    private var canvas: Canvas? = null
    private val mPaths = ArrayList<CustomPath>()
    private val mUndoPaths = ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }

    fun onClickUndo() {
        if (mPaths.size > 0) {
            mUndoPaths.add(mPaths.removeAt(mPaths.size - 1))
            invalidate()
        }
    }

    fun onClickRedo() {
        if (mUndoPaths.size > 0) {
            mPaths.add(mUndoPaths.removeAt(mUndoPaths.size - 1))
            invalidate()
        }
    }

    private fun setUpDrawing() {
        mDrawPath = CustomPath(color, mBrushSize)

        mDrawPaint = Paint()
        mDrawPaint!!.apply {
            isAntiAlias = true
            color = this@DrawingView.color
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        mCanvasPaint = Paint(Paint.DITHER_FLAG)
        mBrushSize = 20f
    }

    fun setBackgroundImage(imageUri: Uri?) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri!!)
            val backgroundImage = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val scaledBitmap = scaleBitmapToViewSize(backgroundImage)
            mCanvasBitmap = scaledBitmap.copy(Bitmap.Config.ARGB_8888, true)
            canvas = Canvas(mCanvasBitmap!!)

            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scaleBitmapToViewSize(bitmap: Bitmap): Bitmap {
        val viewWidth = width
        val viewHeight = height

        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val scaledWidth: Int
        val scaledHeight: Int

        if (viewWidth > viewHeight) {
            scaledWidth = viewWidth
            scaledHeight = (viewWidth / aspectRatio).toInt()
        } else {
            scaledWidth = (viewHeight * aspectRatio).toInt()
            scaledHeight = viewHeight
        }

        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    }

    fun clearDrawing() {
        mPaths.clear()
        mUndoPaths.clear()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)

        for (path in mPaths) {
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            canvas.drawPath(path, mDrawPaint!!)
        }

        if (!mDrawPath!!.isEmpty) {
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize

                mDrawPath!!.reset()
                if (touchX != null && touchY != null) {
                    mDrawPath!!.moveTo(touchX, touchY)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchX != null && touchY != null) {
                    mDrawPath!!.lineTo(touchX, touchY)
                }
            }
            MotionEvent.ACTION_UP -> {
                mPaths.add(mDrawPath!!)
                mDrawPath = CustomPath(color, mBrushSize)
            }
            else -> return false
        }
        invalidate()

        return true
    }

    fun setSizeForBrush(newSize: Float) {
        mBrushSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            newSize,
            resources.displayMetrics
        )
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    fun setColor(newColor: String) {
        color = Color.parseColor(newColor)
        mDrawPaint!!.color = color
        invalidate()
    }

    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path() {}
}