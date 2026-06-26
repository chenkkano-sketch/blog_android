package cc.kkano.blog.ui.article

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.data.model.Article
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyDataBox
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dataBox
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.setHtmlText
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ArticleDetailActivity : AppCompatActivity() {
    private lateinit var contentRoot: LinearLayout
    private lateinit var title: TextView
    private lateinit var meta: TextView
    private lateinit var coverCard: MaterialCardView
    private lateinit var cover: ImageView
    private lateinit var content: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        val articleId = intent.getLongExtra(EXTRA_ARTICLE_ID, 0L)
        if (articleId <= 0L) {
            finish()
            return
        }
        loadArticle(articleId)
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = "文章详情",
                leftIcon = R.drawable.ic_back,
                onLeftClick = { finish() },
            ),
        )

        val scroll = ScrollView(this).apply {
            setBackgroundColor(KkColors.background)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        }
        root.addView(scroll)

        contentRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(20))
        }
        scroll.addView(contentRoot)

        coverCard = MaterialCardView(this).apply {
            radius = dp(17).toFloat()
            cardElevation = dp(9).toFloat()
            setCardBackgroundColor(KkColors.surface)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(210),
            ).apply {
                setMargins(dp(11), 0, dp(11), dp(12))
            }
        }
        cover = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.bg_image_placeholder)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        coverCard.addView(cover)
        contentRoot.addView(coverCard)

        val box = dataBox(marginTop = 0).apply { applyDataBox(14) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(16), dp(15), dp(18))
        }
        box.addView(column)
        title = TextView(this).apply {
            text = "加载中..."
            applyTitleStyle(24f)
            setLineSpacing(dp(5).toFloat(), 1f)
        }
        column.addView(title)
        meta = TextView(this).apply {
            applyBodyStyle(12.5f)
            setTextColor(KkColors.softMuted)
            setPadding(0, dp(10), 0, dp(16))
        }
        column.addView(meta)
        content = TextView(this).apply {
            applyBodyStyle(16f)
            setTextColor(KkColors.text)
            setLineSpacing(dp(6).toFloat(), 1f)
            gravity = Gravity.START
        }
        column.addView(content)
        contentRoot.addView(box)
        return root
    }

    private fun loadArticle(id: Long) {
        lifecycleScope.launch {
            runCatching { AppGraph.repository.article(id) }
                .onSuccess { render(it) }
                .onFailure {
                    Snackbar.make(contentRoot, it.message ?: "文章加载失败", Snackbar.LENGTH_SHORT).show()
                }
        }
    }

    private fun render(article: Article) {
        title.text = article.title
        meta.text = "${article.typeName.orEmpty().ifBlank { "文章" }} · ${article.displayTime()} · ${article.views()} 次阅读"
        content.setHtmlText(article.content)

        val coverUrl = AppGraph.repository.absoluteUrl(article.coverPath())
        coverCard.visibility = if (coverUrl.isNotBlank()) View.VISIBLE else View.GONE
        if (coverUrl.isNotBlank()) {
            Glide.with(cover)
                .load(coverUrl)
                .placeholder(R.drawable.bg_image_placeholder)
                .centerCrop()
                .into(cover)
        } else {
            Glide.with(cover).clear(cover)
        }
    }

    companion object {
        const val EXTRA_ARTICLE_ID = "extra_article_id"
    }
}
