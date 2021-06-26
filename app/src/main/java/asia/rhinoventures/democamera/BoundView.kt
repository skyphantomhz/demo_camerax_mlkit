package asia.rhinoventures.democamera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View

class BoundView(
    context: Context,
    private val rect: Rect,
    private val label: String
) : View(context) {

    private val boundPaint = Paint().apply {
        strokeWidth = 3F
        color = Color.YELLOW
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint().apply {
        color = Color.RED
        textSize = 40F
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawText(label, rect.exactCenterX(), rect.exactCenterY(), textPaint)
        canvas?.drawRect(rect, boundPaint)
    }
}