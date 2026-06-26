package cc.kkano.blog.ui.home

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.data.model.Article
import cc.kkano.blog.ui.common.applyBodyStyle
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
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(context.dp(14), context.dp(8), context.dp(14), context.dp(8))
            }
            radius = context.dp(18).toFloat()
            cardElevation = context.dp(1).toFloat()
            setCardBackgroundColor(context.getColor(R.color.kk_surface))
            strokeColor = context.getColor(R.color.kk_line)
            strokeWidth = context.dp(1)
            isClickable = true
            isFocusable = true
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(context.dp(12), context.dp(12), context.dp(12), context.dp(12))
        }
        card.addView(row)

        val cover = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dp(104), context.dp(82))
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.bg_image_placeholder)
        }
        row.addView(cover)

        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            margin(left = 12)
        }
        row.addView(textColumn)

        val title = TextView(context).apply {
            applyTitleStyle(17f)
            maxLines = 2
        }
        textColumn.addView(title)

        val summary = TextView(context).apply {
            applyBodyStyle(13f)
            maxLines = 2
            margin(top = 6)
        }
        textColumn.addView(summary)

        val meta = TextView(context).apply {
            applyBodyStyle(12f)
            margin(top = 8)
        }
        textColumn.addView(meta)

        return ArticleViewHolder(card, cover, title, summary, meta)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.summary.text = item.summary?.takeIf { it.isNotBlank() } ?: "暂无摘要"
        val category = item.typeName?.takeIf { it.isNotBlank() } ?: "文章"
        holder.meta.text = "$category · ${item.displayTime()} · ${item.views()} 次阅读"

        val coverUrl = repository.absoluteUrl(item.coverPath())
        holder.cover.isVisible = coverUrl.isNotBlank()
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
        val title: TextView,
        val summary: TextView,
        val meta: TextView,
    ) : RecyclerView.ViewHolder(itemView)
}
