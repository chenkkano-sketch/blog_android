package cc.kkano.blog.ui.article

import android.os.Bundle
import android.view.Gravity
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
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.setHtmlText
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ArticleDetailActivity : AppCompatActivity() {
    private lateinit var root: LinearLayout
    private lateinit var title: TextView
    private lateinit var meta: TextView
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

    private fun buildContent(): ScrollView {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.kk_background))
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(28))
        }
        scroll.addView(root)

        val back = MaterialButton(this).apply {
            text = "返回"
            setTextColor(getColor(R.color.kk_text))
            setBackgroundColor(getColor(R.color.kk_surface))
            layoutParams = LinearLayout.LayoutParams(dp(92), dp(44))
            setOnClickListener { finish() }
        }
        root.addView(back)

        title = TextView(this).apply {
            text = "加载中..."
            applyTitleStyle(25f)
            setPadding(0, dp(18), 0, dp(8))
        }
        root.addView(title)

        meta = TextView(this).apply {
            applyBodyStyle(13f)
        }
        root.addView(meta)

        cover = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(210),
            ).apply {
                topMargin = dp(18)
                bottomMargin = dp(16)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.bg_image_placeholder)
        }
        root.addView(cover)

        content = TextView(this).apply {
            applyBodyStyle(16f)
            setTextColor(getColor(R.color.kk_text))
            setLineSpacing(dp(4).toFloat(), 1f)
            gravity = Gravity.START
        }
        root.addView(content)
        return scroll
    }

    private fun loadArticle(id: Long) {
        lifecycleScope.launch {
            runCatching { AppGraph.repository.article(id) }
                .onSuccess { render(it) }
                .onFailure {
                    Snackbar.make(root, it.message ?: "文章加载失败", Snackbar.LENGTH_SHORT).show()
                }
        }
    }

    private fun render(article: Article) {
        title.text = article.title
        meta.text = "${article.typeName.orEmpty().ifBlank { "文章" }} · ${article.displayTime()} · ${article.views()} 次阅读"
        content.setHtmlText(article.content)

        val coverUrl = AppGraph.repository.absoluteUrl(article.coverPath())
        if (coverUrl.isNotBlank()) {
            Glide.with(cover)
                .load(coverUrl)
                .placeholder(R.drawable.bg_image_placeholder)
                .centerCrop()
                .into(cover)
        } else {
            root.removeView(cover)
        }
    }

    companion object {
        const val EXTRA_ARTICLE_ID = "extra_article_id"
    }
}
