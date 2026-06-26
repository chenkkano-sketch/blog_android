package cc.kkano.blog.ui.home

import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.data.model.Article
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyInnerCard
import cc.kkano.blog.ui.common.applyPillStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.margin
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView

class ArticleAdapter(
    private val onArticleClick: (Article) -> Unit,
) : RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder>() {
    private val items = mutableListOf<Article>()
    private val repository = AppGraph.repository

    fun submitList(list: List<Article>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val context = parent.context
        val card = MaterialCardView(context).apply {
            applyInnerCard(13)
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(context.dp(10), context.dp(5), context.dp(10), context.dp(9))
            }
            isClickable = true
            isFocusable = true
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = context.dp(94)
            setPadding(context.dp(9), context.dp(9), context.dp(9), context.dp(9))
        }
        card.addView(row)

        val cover = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dp(103), context.dp(76))
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.bg_image_placeholder)
        }
        row.addView(cover)

        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            margin(left = 10)
        }
        row.addView(textColumn)

        val title = TextView(context).apply {
            applyTitleStyle(15.5f)
            maxLines = 2
            setLineSpacing(context.dp(2).toFloat(), 1f)
        }
        textColumn.addView(title)

        val summary = TextView(context).apply {
            applyBodyStyle(12.5f)
            maxLines = 2
            setLineSpacing(context.dp(2).toFloat(), 1f)
            margin(top = 5)
        }
        textColumn.addView(summary)

        val metaRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            margin(top = 8)
        }
        textColumn.addView(metaRow)

        val tag = TextView(context).apply {
            applyPillStyle()
            maxLines = 1
        }
        metaRow.addView(tag)

        val time = TextView(context).apply {
            applyBodyStyle(11f)
            setTextColor(KkColors.softMuted)
            margin(left = 8)
            maxLines = 1
        }
        metaRow.addView(time)

        val count = TextView(context).apply {
            applyBodyStyle(11f)
            setTextColor(KkColors.softMuted)
            margin(left = 8)
            maxLines = 1
        }
        metaRow.addView(count)

        return ArticleViewHolder(card, cover, textColumn, title, summary, tag, time, count)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.summary.text = item.summary?.takeIf { it.isNotBlank() } ?: "暂无摘要"
        holder.tag.text = item.typeName?.takeIf { it.isNotBlank() } ?: "文章"
        holder.time.text = item.displayTime().ifBlank { "刚刚" }
        holder.count.text = "${item.views()} 浏览"

        val coverUrl = repository.absoluteUrl(item.coverPath())
        holder.cover.isVisible = coverUrl.isNotBlank()
        (holder.textColumn.layoutParams as LinearLayout.LayoutParams).leftMargin =
            holder.itemView.context.dp(if (coverUrl.isNotBlank()) 10 else 0)
        if (coverUrl.isNotBlank()) {
            Glide.with(holder.cover)
                .load(coverUrl)
                .placeholder(R.drawable.bg_image_placeholder)
                .centerCrop()
                .into(holder.cover)
        } else {
            Glide.with(holder.cover).clear(holder.cover)
        }

        holder.itemView.setOnClickListener { onArticleClick(item) }
    }

    class ArticleViewHolder(
        itemView: MaterialCardView,
        val cover: ImageView,
        val textColumn: LinearLayout,
        val title: TextView,
        val summary: TextView,
        val tag: TextView,
        val time: TextView,
        val count: TextView,
    ) : RecyclerView.ViewHolder(itemView)
}
