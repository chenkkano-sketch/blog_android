package cc.kkano.blog.ui.common

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

object EmojiTextRenderer {
    private const val OBJECT_REPLACEMENT = "\uFFFC"

    fun render(textView: TextView, raw: String?, emojiMap: Map<String, String>, sizeDp: Int = 34) {
        val source = Html.fromHtml(raw.orEmpty(), Html.FROM_HTML_MODE_COMPACT).toString()
        textView.tag = source
        if (emojiMap.isEmpty()) {
            textView.text = source
            return
        }

        val keys = emojiMap.keys.sortedByDescending { it.length }
        val spans = mutableListOf<EmojiSpanRequest>()
        val builder = StringBuilder()
        var index = 0
        while (index < source.length) {
            val key = keys.firstOrNull { source.startsWith(it, index) }
            if (key == null) {
                builder.append(source[index])
                index++
            } else {
                val start = builder.length
                builder.append(OBJECT_REPLACEMENT)
                spans.add(EmojiSpanRequest(start, start + 1, emojiMap.getValue(key)))
                index += key.length
            }
        }

        val spannable = SpannableStringBuilder(builder.toString())
        textView.text = spannable
        val targetSize = textView.context.dp(sizeDp)
        spans.forEach { request ->
            Glide.with(textView)
                .asBitmap()
                .load(request.url)
                .into(object : CustomTarget<Bitmap>(targetSize, targetSize) {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        if (textView.tag != source) return
                        val drawable = BitmapDrawable(textView.resources, resource).apply {
                            setBounds(0, 0, targetSize, targetSize)
                        }
                        spannable.setSpan(
                            ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM),
                            request.start,
                            request.end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                        textView.text = spannable
                    }

                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) = Unit
                })
        }
    }

    private data class EmojiSpanRequest(
        val start: Int,
        val end: Int,
        val url: String,
    )
}
