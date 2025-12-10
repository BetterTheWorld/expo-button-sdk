package expo.modules.buttonsdk.promotion

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

object PromotionUIFactory {
    
    fun createHeaderPromotionButton(
        context: Context,
        count: Int,
        badgeLabel: String,
        badgeFontSize: Float,
        onClickListener: () -> Unit
    ): View? {
        val button = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
            
            val pillBackground = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#F9F9FB"))
                cornerRadius = dpToPx(13).toFloat()
            }
            
            val rippleDrawable = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#20000000")),
                pillBackground,
                null
            )
            background = rippleDrawable
            
            isClickable = true
            isFocusable = true
            
            setOnClickListener { onClickListener() }
        }
        
        val iconView = createTagIconView(context, badgeFontSize)
        if (iconView != null) {
            button.addView(iconView)
        }
        
        val labelView = TextView(context).apply {
            text = badgeLabel
            textSize = badgeFontSize
            setTextColor(Color.parseColor("#0B72AC"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(4)
                rightMargin = dpToPx(4)
                gravity = Gravity.CENTER_VERTICAL
            }
            layoutParams = params
        }
        button.addView(labelView)        
        
        return button
    }
    
    private fun createTagIconView(context: Context, badgeFontSize: Float): View? {
        val scaleFactor = badgeFontSize / 11f
        val scaledIconSize = dpToPx((14 * scaleFactor).toInt())
        
        val iconView = ImageView(context).apply {
            setImageDrawable(createTagIconDrawable(badgeFontSize))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            
            val params = LinearLayout.LayoutParams(
                scaledIconSize,
                scaledIconSize
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            layoutParams = params
        }
        return iconView
    }
    
    private fun createTagIconDrawable(badgeFontSize: Float): Drawable {
        return object : Drawable() {
            private val scaleFactor = badgeFontSize / 11f
            
            private val paint = Paint().apply {
                color = Color.parseColor("#0B72AC")
                style = Paint.Style.STROKE
                strokeWidth = dpToPx(1).toFloat() * scaleFactor
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }
            
            override fun draw(canvas: Canvas) {
                val bounds = getBounds()
                val scale = minOf(bounds.width(), bounds.height()) / 24f
                
                canvas.save()
                canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
                canvas.scale(scale, scale)
                
                val path = Path().apply {
                    moveTo(7f, 3f)
                    lineTo(12f, 3f)
                    lineTo(12.586f, 3.586f)
                    lineTo(19.586f, 10.586f)
                    lineTo(19.586f, 13.414f)
                    lineTo(12.586f, 20.414f)
                    lineTo(9.758f, 20.414f)
                    lineTo(2.758f, 13.414f)
                    lineTo(3f, 12f)
                    lineTo(3f, 7f)
                    lineTo(7f, 3f)
                    close()
                }
                
                canvas.drawPath(path, paint)
                
                val holePaint = Paint().apply {
                    color = Color.parseColor("#0B72AC")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawCircle(7f, 7f, 1f, holePaint)
                
                canvas.restore()
            }
            
            override fun setAlpha(alpha: Int) {
                paint.alpha = alpha
            }
            
            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
                paint.colorFilter = colorFilter
            }
            
            override fun getOpacity(): Int {
                return android.graphics.PixelFormat.TRANSLUCENT
            }
        }
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }
}