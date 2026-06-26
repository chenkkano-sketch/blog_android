package cc.kkano.blog.ui.common

import android.content.Context
import android.graphics.Color
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

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

fun TextView.applyTitleStyle(sizeSp: Float = 20f) {
    textSize = sizeSp
    setTextColor(Color.parseColor("#15171C"))
    typeface = android.graphics.Typeface.DEFAULT_BOLD
}

fun TextView.applyBodyStyle(sizeSp: Float = 14f) {
    textSize = sizeSp
    setTextColor(Color.parseColor("#69707C"))
}

fun TextView.setHtmlText(value: String?) {
    text = Html.fromHtml(value.orEmpty(), Html.FROM_HTML_MODE_COMPACT)
}

fun View.roundPadding(value: Int) {
    setPadding(context.dp(value))
}

fun Context.color(@ColorRes resId: Int): Int = ContextCompat.getColor(this, resId)
