package cc.kkano.blog.ui.dynamics

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.data.model.Dynamic
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.margin
import cc.kkano.blog.ui.common.setHtmlText
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView

class DynamicAdapter : RecyclerView.Adapter<DynamicAdapter.DynamicViewHolder>() {
    private val items = mutableListOf<Dynamic>()
    private val repository = AppGraph.repository

    fun submitList(list: List<Dynamic>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DynamicViewHolder {
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
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(14), context.dp(14), context.dp(14), context.dp(14))
        }
        card.addView(root)

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        root.addView(header)

        val avatar = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dp(42), context.dp(42))
            setBackgroundResource(R.drawable.bg_avatar)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        header.addView(avatar)

        val userColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            margin(left = 10)
        }
        header.addView(userColumn)

        val nickname = TextView(context).apply {
            applyTitleStyle(15f)
            maxLines = 1
        }
        userColumn.addView(nickname)

        val time = TextView(context).apply {
            applyBodyStyle(12f)
            margin(top = 2)
        }
        userColumn.addView(time)

        val title = TextView(context).apply {
            applyTitleStyle(17f)
            maxLines = 2
            margin(top = 14)
        }
        root.addView(title)

        val content = TextView(context).apply {
            applyBodyStyle(14f)
            margin(top = 8)
        }
        root.addView(content)

        val preview = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                context.dp(184),
            ).apply {
                topMargin = context.dp(12)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.bg_image_placeholder)
        }
        root.addView(preview)

        val stats = TextView(context).apply {
            applyBodyStyle(12f)
            margin(top = 12)
        }
        root.addView(stats)

        return DynamicViewHolder(card, avatar, nickname, time, title, content, preview, stats)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: DynamicViewHolder, position: Int) {
        val item = items[position]
        val user = item.user
        holder.nickname.text = user?.nickname?.takeIf { it.isNotBlank() }
            ?: user?.username?.takeIf { it.isNotBlank() }
            ?: "匿名用户"
        holder.time.text = item.createdAt.orEmpty()
        holder.title.text = item.title.orEmpty()
        holder.title.isVisible = !item.title.isNullOrBlank()
        holder.content.setHtmlText(item.content)
        holder.stats.text = "${item.views ?: 0} 浏览 · ${item.likes ?: 0} 赞 · ${item.commentsCount ?: 0} 评论"

        val avatarUrl = repository.absoluteUrl(user?.avatar)
        if (avatarUrl.isNotBlank()) {
            Glide.with(holder.avatar)
                .load(avatarUrl)
                .placeholder(R.drawable.bg_avatar)
                .centerCrop()
                .into(holder.avatar)
        } else {
            Glide.with(holder.avatar).clear(holder.avatar)
        }

        val firstImage = item.imagePaths().firstOrNull()
        val imageUrl = repository.absoluteUrl(firstImage)
        holder.preview.isVisible = imageUrl.isNotBlank()
        if (imageUrl.isNotBlank()) {
            Glide.with(holder.preview)
                .load(imageUrl)
                .placeholder(R.drawable.bg_image_placeholder)
                .centerCrop()
                .into(holder.preview)
        } else {
            Glide.with(holder.preview).clear(holder.preview)
        }
    }

    class DynamicViewHolder(
        itemView: MaterialCardView,
        val avatar: ImageView,
        val nickname: TextView,
        val time: TextView,
        val title: TextView,
        val content: TextView,
        val preview: ImageView,
        val stats: TextView,
    ) : RecyclerView.ViewHolder(itemView)
}
