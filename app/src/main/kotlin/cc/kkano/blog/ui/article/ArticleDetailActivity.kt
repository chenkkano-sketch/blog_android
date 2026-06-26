package cc.kkano.blog.ui.article

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.data.api.ApiRoutes
import cc.kkano.blog.data.model.Article
import cc.kkano.blog.data.model.Comment
import cc.kkano.blog.navigation.FeatureMode
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyDataBox
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dataBox
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.common.sectionHeader
import cc.kkano.blog.ui.common.setHtmlText
import cc.kkano.blog.ui.common.setRoundedBackground
import cc.kkano.blog.ui.feature.FeatureFormActivity
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ArticleDetailActivity : AppCompatActivity() {
    private lateinit var contentRoot: LinearLayout
    private lateinit var title: TextView
    private lateinit var meta: TextView
    private lateinit var authorAvatar: ImageView
    private lateinit var authorName: TextView
    private lateinit var authorIntro: TextView
    private lateinit var coverCard: MaterialCardView
    private lateinit var cover: ImageView
    private lateinit var webView: WebView
    private lateinit var tagsRow: LinearLayout
    private lateinit var commentsBox: MaterialCardView
    private lateinit var commentsColumn: LinearLayout
    private var articleId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        articleId = intent.getLongExtra(EXTRA_ARTICLE_ID, 0L)
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
                rightIcon = R.drawable.ic_search,
                onLeftClick = { finish() },
                onRightClick = { openSearch() },
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
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        coverCard.addView(cover)
        contentRoot.addView(coverCard)

        val infoBox = dataBox(marginTop = 0).apply { applyDataBox(14) }
        val infoColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(18), dp(15), dp(16))
        }
        infoBox.addView(infoColumn)
        title = TextView(this).apply {
            text = "加载中..."
            applyTitleStyle(24f)
            setLineSpacing(dp(5).toFloat(), 1f)
        }
        infoColumn.addView(title)
        meta = TextView(this).apply {
            applyBodyStyle(12.5f)
            setTextColor(KkColors.softMuted)
            setPadding(0, dp(10), 0, dp(14))
        }
        infoColumn.addView(meta)
        infoColumn.addView(buildAuthorCard())
        contentRoot.addView(infoBox)

        val contentBox = dataBox(marginTop = 12).apply { applyDataBox(14) }
        val articleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(12), dp(15), dp(16))
        }
        contentBox.addView(articleColumn)
        articleColumn.addView(sectionHeader("▌ 正文"))
        webView = WebView(this).apply {
            setBackgroundColor(Color.WHITE)
            settings.defaultTextEncodingName = "utf-8"
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    view.evaluateJavascript(
                        "(function(){return Math.max(document.body.scrollHeight,document.documentElement.scrollHeight);})()",
                    ) { value ->
                        val height = value.trim('"').toFloatOrNull()?.toInt() ?: return@evaluateJavascript
                        view.layoutParams = view.layoutParams.apply {
                            this.height = dp(height.coerceAtLeast(260))
                        }
                    }
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(320),
            )
        }
        articleColumn.addView(webView)
        tagsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }
        articleColumn.addView(tagsRow)
        contentRoot.addView(contentBox)

        commentsBox = dataBox(marginTop = 12).apply { applyDataBox(14) }
        commentsColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(2), 0, dp(6))
        }
        commentsBox.addView(commentsColumn)
        contentRoot.addView(commentsBox)
        return root
    }

    private fun buildAuthorCard(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setRoundedBackground(Color.parseColor("#F7F8FA"), 12)
            authorAvatar = ImageView(this@ArticleDetailActivity).apply {
                setImageResource(R.drawable.ic_account)
                setColorFilter(KkColors.softMuted)
                setPadding(dp(11), dp(11), dp(11), dp(11))
                background = roundedDrawable(Color.parseColor("#EDF0F4"), 999)
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            }
            addView(authorAvatar)
            addView(LinearLayout(this@ArticleDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = dp(10)
                }
                authorName = TextView(this@ArticleDetailActivity).apply {
                    text = "站长"
                    applyTitleStyle(15f)
                }
                addView(authorName)
                authorIntro = TextView(this@ArticleDetailActivity).apply {
                    text = "这是站长大大哦~"
                    applyBodyStyle(12f)
                    setPadding(0, dp(5), 0, 0)
                }
                addView(authorIntro)
            })
            addView(TextView(this@ArticleDetailActivity).apply {
                text = "信息"
                applyTitleStyle(12f)
                setTextColor(KkColors.orange)
                gravity = Gravity.CENTER
                setRoundedBackground(Color.parseColor("#FFF1E8"), 999)
                setPadding(dp(10), dp(5), dp(10), dp(5))
            })
        }
    }

    private fun loadArticle(id: Long) {
        lifecycleScope.launch {
            runCatching { AppGraph.repository.article(id) }
                .onSuccess { article ->
                    render(article)
                    AppGraph.repository.incrementArticleView(id)
                    loadComments(id)
                }
                .onFailure {
                    Snackbar.make(contentRoot, it.message ?: "文章加载失败", Snackbar.LENGTH_SHORT).show()
                }
        }
    }

    private fun render(article: Article) {
        title.text = article.title
        meta.text = "${article.typeName.orEmpty().ifBlank { "暂无分类" }} · ${article.displayTime()} · ${article.views()} 次阅读"
        authorName.text = article.authorName?.takeIf { it.isNotBlank() } ?: "站长"
        authorIntro.text = "这是站长大大哦~"
        val authorAvatarUrl = AppGraph.repository.absoluteUrl(article.authorAvatar)
        if (authorAvatarUrl.isNotBlank()) {
            authorAvatar.clearColorFilter()
            authorAvatar.setPadding(0, 0, 0, 0)
            Glide.with(authorAvatar)
                .load(authorAvatarUrl)
                .placeholder(R.drawable.bg_avatar)
                .centerCrop()
                .into(authorAvatar)
        }

        val coverUrl = AppGraph.repository.absoluteUrl(article.coverPath())
        coverCard.visibility = if (coverUrl.isNotBlank()) View.VISIBLE else View.GONE
        if (coverUrl.isNotBlank()) {
            Glide.with(cover)
                .load(coverUrl)
                .placeholder(R.drawable.bg_image_placeholder)
                .centerCrop()
                .into(cover)
        }

        webView.loadDataWithBaseURL(
            AppGraph.apiClient.baseUrl,
            htmlDocument(article.content.orEmpty()),
            "text/html",
            "utf-8",
            null,
        )
        renderTags(article)
    }

    private fun renderTags(article: Article) {
        tagsRow.removeAllViews()
        tagsRow.isVisible = article.tags.isNotEmpty()
        article.tags.forEach { tag ->
            tagsRow.addView(TextView(this).apply {
                text = tag.name
                applyTitleStyle(12f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.black, 8)
                setPadding(dp(10), dp(5), dp(10), dp(5))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    rightMargin = dp(8)
                }
            })
        }
    }

    private suspend fun loadComments(id: Long) {
        runCatching { AppGraph.repository.articleComments(id) }
            .onSuccess { renderComments(it) }
            .onFailure { renderComments(emptyList()) }
    }

    private fun renderComments(comments: List<Comment>) {
        commentsColumn.removeAllViews()
        commentsColumn.addView(
            sectionHeader("▌ 评论区${if (comments.isNotEmpty()) " (${comments.size})" else ""}", "发布评论 ›") {
                openCommentForm()
            },
        )
        if (comments.isEmpty()) {
            commentsColumn.addView(TextView(this).apply {
                text = "暂时没有评论"
                applyBodyStyle(14f)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(72),
                )
            })
            return
        }
        comments.take(8).forEach { comment ->
            commentsColumn.addView(commentRow(comment))
        }
    }

    private fun commentRow(comment: Comment): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            val avatarView = ImageView(this@ArticleDetailActivity).apply {
                setImageResource(R.drawable.ic_account)
                setColorFilter(KkColors.softMuted)
                background = roundedDrawable(Color.parseColor("#EDF0F4"), 999)
                setPadding(dp(9), dp(9), dp(9), dp(9))
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            }
            val avatarUrl = AppGraph.repository.absoluteUrl(comment.user?.avatar)
            if (avatarUrl.isNotBlank()) {
                avatarView.clearColorFilter()
                avatarView.setPadding(0, 0, 0, 0)
                Glide.with(avatarView).load(avatarUrl).placeholder(R.drawable.bg_avatar).centerCrop().into(avatarView)
            }
            addView(avatarView)
            addView(LinearLayout(this@ArticleDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = dp(10)
                }
                addView(TextView(this@ArticleDetailActivity).apply {
                    text = comment.displayName()
                    applyTitleStyle(13.5f)
                })
                addView(TextView(this@ArticleDetailActivity).apply {
                    setHtmlText(comment.content)
                    applyBodyStyle(13.5f)
                    setTextColor(KkColors.text)
                    setPadding(0, dp(6), 0, 0)
                })
                addView(TextView(this@ArticleDetailActivity).apply {
                    text = "${comment.createdAt.orEmpty()} · ${comment.likes ?: 0} 赞"
                    applyBodyStyle(11.5f)
                    setPadding(0, dp(8), 0, 0)
                })
            })
        }
    }

    private fun openCommentForm() {
        startActivity(
            Intent(this, FeatureFormActivity::class.java)
                .putExtra(FeatureFormActivity.EXTRA_TITLE, "发表评论")
                .putExtra(FeatureFormActivity.EXTRA_ENDPOINT, ApiRoutes.COMMENTS)
                .putExtra(FeatureFormActivity.EXTRA_MODE, FeatureMode.FORM_COMMENT.name)
                .putExtra(FeatureFormActivity.EXTRA_ROUTE, "pages/contents/commentsadd")
                .putExtra(FeatureFormActivity.EXTRA_DEFAULT_TYPE, "1")
                .putExtra(FeatureFormActivity.EXTRA_DEFAULT_TARGET_ID, articleId.toString()),
        )
    }

    private fun openSearch() {
        startActivity(
            Intent(this, cc.kkano.blog.ui.feature.GenericListActivity::class.java)
                .putExtra(cc.kkano.blog.ui.feature.GenericListActivity.EXTRA_TITLE, "搜索")
                .putExtra(cc.kkano.blog.ui.feature.GenericListActivity.EXTRA_ENDPOINT, ApiRoutes.SEARCH_UNIAPP)
                .putExtra(cc.kkano.blog.ui.feature.GenericListActivity.EXTRA_MODE, FeatureMode.LIST.name),
        )
    }

    private fun htmlDocument(raw: String): String {
        val base = AppGraph.apiClient.baseUrl.trimEnd('/')
        val normalized = raw.replace(Regex("""src=["'](/[^"']+)["']""")) { match ->
            match.value.replace(match.groupValues[1], base + match.groupValues[1])
        }
        return """
            <!doctype html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body{margin:0;padding:0;color:#252932;font-size:16px;line-height:1.82;font-family:-apple-system,BlinkMacSystemFont,'PingFang SC','HarmonyOS Sans SC',sans-serif;word-break:break-word;}
                    p{margin:0 0 14px;}
                    img,video{max-width:100%;height:auto;border-radius:12px;background:#edf0f4;}
                    pre,code{white-space:pre-wrap;background:#f7f8fa;border-radius:10px;color:#15171c;}
                    pre{padding:12px;overflow:auto;}
                    blockquote{margin:12px 0;padding:10px 14px;border-left:4px solid #15171c;background:#f7f8fa;border-radius:10px;color:#3b414c;}
                    table{width:100%;border-collapse:collapse;display:block;overflow-x:auto;}
                    th,td{border:1px solid #e7e9ee;padding:8px;}
                    a{color:#ff7a36;text-decoration:none;}
                </style>
            </head>
            <body>$normalized</body>
            </html>
        """.trimIndent()
    }

    companion object {
        const val EXTRA_ARTICLE_ID = "extra_article_id"
    }
}
