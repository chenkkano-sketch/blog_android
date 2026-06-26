package cc.kkano.blog.ui.common

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Html
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import cc.kkano.blog.R
import com.google.android.material.card.MaterialCardView

object KkColors {
    val black: Int = Color.parseColor("#15171C")
    val orange: Int = Color.parseColor("#FF7A36")
    val background: Int = Color.parseColor("#F4F5F7")
    val surface: Int = Color.WHITE
    val text: Int = Color.parseColor("#252932")
    val muted: Int = Color.parseColor("#69707C")
    val softMuted: Int = Color.parseColor("#858B96")
    val line: Int = Color.parseColor("#E7E9EE")
    val soft: Int = Color.parseColor("#F7F8FA")
    val danger: Int = Color.parseColor("#FF4F5F")
}

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

fun Context.roundedDrawable(
    color: Int,
    radius: Int,
    strokeColor: Int? = null,
    strokeWidth: Int = 1,
): GradientDrawable {
    return GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (strokeColor != null) {
            setStroke(dp(strokeWidth), strokeColor)
        }
    }
}

fun Context.gradientDrawable(
    colors: IntArray,
    radius: Int,
    orientation: GradientDrawable.Orientation = GradientDrawable.Orientation.TOP_BOTTOM,
): GradientDrawable {
    return GradientDrawable(orientation, colors).apply {
        cornerRadius = dp(radius).toFloat()
    }
}

fun View.margin(
    left: Int = 0,
    top: Int = 0,
    right: Int = 0,
    bottom: Int = 0,
) {
    val params = layoutParams as? ViewGroup.MarginLayoutParams ?: return
    params.setMargins(context.dp(left), context.dp(top), context.dp(right), context.dp(bottom))
    layoutParams = params
}

fun View.roundPadding(value: Int) {
    setPadding(context.dp(value))
}

fun View.setRoundedBackground(
    color: Int,
    radius: Int,
    strokeColor: Int? = null,
    strokeWidth: Int = 1,
) {
    background = context.roundedDrawable(color, radius, strokeColor, strokeWidth)
}

fun MaterialCardView.applyDataBox(radius: Int = 14) {
    radius.also { this.radius = context.dp(it).toFloat() }
    cardElevation = context.dp(7).toFloat()
    setCardBackgroundColor(KkColors.surface)
    strokeColor = Color.parseColor("#1012151C")
    strokeWidth = context.dp(1)
    preventCornerOverlap = true
    useCompatPadding = false
}

fun MaterialCardView.applyInnerCard(radius: Int = 13) {
    this.radius = context.dp(radius).toFloat()
    cardElevation = context.dp(0).toFloat()
    setCardBackgroundColor(KkColors.surface)
    strokeColor = Color.parseColor("#1012151C")
    strokeWidth = context.dp(1)
    preventCornerOverlap = true
}

fun TextView.applyTitleStyle(sizeSp: Float = 20f) {
    textSize = sizeSp
    setTextColor(KkColors.black)
    typeface = Typeface.DEFAULT_BOLD
    includeFontPadding = false
}

fun TextView.applyBodyStyle(sizeSp: Float = 14f) {
    textSize = sizeSp
    setTextColor(KkColors.muted)
    includeFontPadding = false
}

fun TextView.applyPillStyle(
    backgroundColor: Int = Color.parseColor("#FFF1E8"),
    textColor: Int = KkColors.orange,
) {
    textSize = 11f
    typeface = Typeface.DEFAULT_BOLD
    includeFontPadding = false
    setTextColor(textColor)
    gravity = Gravity.CENTER
    setPadding(context.dp(7), context.dp(3), context.dp(7), context.dp(3))
    setRoundedBackground(backgroundColor, 999)
}

fun TextView.setHtmlText(value: String?) {
    text = Html.fromHtml(value.orEmpty(), Html.FROM_HTML_MODE_COMPACT)
}

fun Context.color(@ColorRes resId: Int): Int = ContextCompat.getColor(this, resId)

fun Context.kkTopBar(
    title: String,
    @DrawableRes leftIcon: Int? = null,
    @DrawableRes rightIcon: Int? = null,
    onLeftClick: (() -> Unit)? = null,
    onRightClick: (() -> Unit)? = null,
): View {
    val bar = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(8), dp(14), dp(8))
        setBackgroundColor(KkColors.surface)
        elevation = dp(6).toFloat()
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(56),
        )
    }

    bar.addView(topBarAction(leftIcon, onLeftClick))
    bar.addView(TextView(this).apply {
        text = title
        applyTitleStyle(18f)
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
    })
    bar.addView(topBarAction(rightIcon, onRightClick))
    return bar
}

private fun Context.topBarAction(
    @DrawableRes icon: Int?,
    onClick: (() -> Unit)?,
): View {
    val frame = FrameLayout(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(54), ViewGroup.LayoutParams.MATCH_PARENT)
    }
    if (icon == null) return frame
    val button = ImageButton(this).apply {
        setImageResource(icon)
        setColorFilter(KkColors.black)
        background = roundedDrawable(KkColors.surface, 20)
        elevation = dp(4).toFloat()
        scaleType = android.widget.ImageView.ScaleType.CENTER
        setPadding(dp(10))
        layoutParams = FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER)
        setOnClickListener { onClick?.invoke() }
    }
    frame.addView(button)
    return frame
}

fun Context.sectionHeader(
    title: String,
    action: String? = null,
    onActionClick: (() -> Unit)? = null,
): LinearLayout {
    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), 0, dp(12), 0)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(46),
        )
        addView(TextView(this@sectionHeader).apply {
            text = title
            applyTitleStyle(16f)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        if (action != null) {
            addView(TextView(this@sectionHeader).apply {
                text = action
                applyBodyStyle(12f)
                setTextColor(KkColors.softMuted)
                setOnClickListener { onActionClick?.invoke() }
            })
        }
    }
}

fun Context.dataBox(marginTop: Int = 12): MaterialCardView {
    return MaterialCardView(this).apply {
        applyDataBox()
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            setMargins(dp(11), dp(marginTop), dp(11), 0)
        }
    }
}

fun Context.menuRow(
    title: String,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    onClick: (() -> Unit)? = null,
): View {
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), dp(10), dp(12), dp(10))
        setRoundedBackground(KkColors.soft, 11)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(56),
        ).apply {
            setMargins(dp(9), dp(5), dp(9), dp(7))
        }
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick?.invoke() }
    }

    if (icon != null) {
        row.addView(ImageButton(this).apply {
            setImageResource(icon)
            setColorFilter(KkColors.black)
            background = null
            isClickable = false
            setPadding(dp(8))
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        })
    }
    row.addView(LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        addView(TextView(this@menuRow).apply {
            text = title
            applyTitleStyle(14.5f)
        })
        if (!subtitle.isNullOrBlank()) {
            addView(TextView(this@menuRow).apply {
                text = subtitle
                applyBodyStyle(11.5f)
                setPadding(0, dp(4), 0, 0)
            })
        }
    })
    row.addView(TextView(this).apply {
        text = "›"
        textSize = 24f
        setTextColor(Color.parseColor("#BDC1C8"))
        includeFontPadding = false
    })
    return row
}
