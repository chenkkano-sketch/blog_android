package cc.kkano.blog.ui.dynamics

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.data.model.Dynamic
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyDataBox
import cc.kkano.blog.ui.common.applyPillStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.margin
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.common.setHtmlText
import cc.kkano.blog.ui.common.setRoundedBackground
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
            applyDataBox(15)
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(context.dp(11), context.dp(6), context.dp(11), context.dp(12))
            }
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(14), context.dp(14), context.dp(14), context.dp(14))
        }
        card.addView(root)

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        root.addView(header)

        val avatar = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dp(39), context.dp(39))
            setBackgroundResource(R.drawable.bg_avatar)
            scaleType = ImageView.ScaleType.CENTER_CROP
            elevation = context.dp(4).toFloat()
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

        val metaRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            margin(top = 5)
        }
        userColumn.addView(metaRow)

        val role = TextView(context).apply {
            text = "用户"
            applyPillStyle(KkColors.black, Color.WHITE)
        }
        metaRow.addView(role)

        val time = TextView(context).apply {
            applyBodyStyle(12f)
            setTextColor(Color.parseColor("#8A909B"))
            margin(left = 8)
        }
        metaRow.addView(time)

        val more = TextView(context).apply {
            text = "⋯"
            textSize = 22f
            gravity = Gravity.CENTER
            includeFontPadding = false
            setTextColor(KkColors.muted)
            setRoundedBackground(Color.parseColor("#F6F7F9"), 9)
            layoutParams = LinearLayout.LayoutParams(context.dp(42), context.dp(34))
        }
        header.addView(more)

        val title = TextView(context).apply {
            applyTitleStyle(16.5f)
            maxLines = 2
            setLineSpacing(context.dp(3).toFloat(), 1f)
            margin(top = 14)
        }
        root.addView(title)

        val content = TextView(context).apply {
            applyBodyStyle(14.5f)
            setTextColor(Color.parseColor("#3B414C"))
            setLineSpacing(context.dp(4).toFloat(), 1f)
            margin(top = 9)
        }
        root.addView(content)

        val imageGrid = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            margin(top = 12)
        }
        root.addView(imageGrid)

        val footer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, context.dp(13), 0, 0)
            margin(top = 8)
        }
        root.addView(footer)
        val views = actionPill(context, "0 浏览")
        val likes = actionPill(context, "0 赞")
        val comments = actionPill(context, "0 评论")
        footer.addView(views)
        footer.addView(likes)
        footer.addView(comments)

        val likeSummary = TextView(context).apply {
            applyBodyStyle(12f)
            setTextColor(KkColors.muted)
            setPadding(context.dp(9), context.dp(7), context.dp(9), context.dp(7))
            setRoundedBackground(KkColors.soft, 10)
            margin(top = 10)
            visibility = android.view.View.GONE
        }
        root.addView(likeSummary)

        return DynamicViewHolder(
            card,
            avatar,
            nickname,
            role,
            time,
            title,
            content,
            imageGrid,
            views,
            likes,
            comments,
            likeSummary,
        )
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: DynamicViewHolder, position: Int) {
        val item = items[position]
        val user = item.user
        holder.nickname.text = user?.nickname?.takeIf { it.isNotBlank() }
            ?: user?.username?.takeIf { it.isNotBlank() }
            ?: "匿名用户"
        holder.time.text = item.createdAt.orEmpty().ifBlank { "刚刚" }
        holder.title.text = item.title.orEmpty()
        holder.title.isVisible = !item.title.isNullOrBlank()
        holder.content.setHtmlText(item.content)
        holder.views.text = "${item.views ?: 0} 浏览"
        holder.likes.text = "${item.likes ?: 0} 赞"
        holder.comments.text = "${item.commentsCount ?: 0} 评论"
        holder.likes.setRoundedBackground(
            if (item.isLiked == true) Color.parseColor("#FFF1F3") else KkColors.soft,
            999,
        )
        holder.likes.setTextColor(if (item.isLiked == true) KkColors.danger else KkColors.muted)

        val summary = item.likeSummary
        holder.likeSummary.isVisible = summary != null && summary.count > 0
        holder.likeSummary.text = summary?.let {
            val preview = it.preview.take(3).joinToString("、") { userInfo ->
                userInfo.nickname?.takeIf { name -> name.isNotBlank() }
                    ?: userInfo.username.orEmpty()
            }.ifBlank { "${it.count} 人" }
            if (it.adminLiked) "♥ $preview 赞过 · 站长赞过" else "♥ $preview 赞过"
        }.orEmpty()
        if (summary?.adminLiked == true) {
            holder.likeSummary.setRoundedBackground(Color.parseColor("#FFF8E6"), 10)
            holder.likeSummary.setTextColor(Color.parseColor("#7C5B00"))
        } else {
            holder.likeSummary.setRoundedBackground(KkColors.soft, 10)
            holder.likeSummary.setTextColor(KkColors.muted)
        }

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

        bindImages(holder.imageGrid, item.imagePaths())
    }

    private fun actionPill(context: android.content.Context, textValue: String): TextView {
        return TextView(context).apply {
            text = textValue
            applyBodyStyle(12f)
            setTextColor(KkColors.muted)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setRoundedBackground(KkColors.soft, 999)
            layoutParams = LinearLayout.LayoutParams(0, context.dp(34), 1f).apply {
                setMargins(context.dp(3), 0, context.dp(3), 0)
            }
        }
    }

    private fun bindImages(container: LinearLayout, paths: List<String>) {
        val context = container.context
        container.removeAllViews()
        container.isVisible = paths.isNotEmpty()
        if (paths.isEmpty()) return

        val visiblePaths = paths.take(3)
        container.orientation = LinearLayout.HORIZONTAL
        val height = if (visiblePaths.size == 1) context.dp(210) else context.dp(119)
        visiblePaths.forEach { path ->
            val card = MaterialCardView(context).apply {
                radius = context.dp(11).toFloat()
                cardElevation = 0f
                setCardBackgroundColor(Color.parseColor("#EDF0F4"))
                layoutParams = LinearLayout.LayoutParams(0, height, 1f).apply {
                    setMargins(context.dp(2), 0, context.dp(2), 0)
                }
            }
            val image = ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundResource(R.drawable.bg_image_placeholder)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
            card.addView(image)
            val url = repository.absoluteUrl(path)
            if (url.isNotBlank()) {
                Glide.with(image)
                    .load(url)
                    .placeholder(R.drawable.bg_image_placeholder)
                    .centerCrop()
                    .into(image)
            }
            container.addView(card)
        }
    }

    class DynamicViewHolder(
        itemView: MaterialCardView,
        val avatar: ImageView,
        val nickname: TextView,
        val role: TextView,
        val time: TextView,
        val title: TextView,
        val content: TextView,
        val imageGrid: LinearLayout,
        val views: TextView,
        val likes: TextView,
        val comments: TextView,
        val likeSummary: TextView,
    ) : RecyclerView.ViewHolder(itemView)
}
