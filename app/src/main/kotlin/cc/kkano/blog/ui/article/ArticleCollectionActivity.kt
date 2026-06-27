package cc.kkano.blog.ui.article

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.data.model.Article
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.common.setRoundedBackground
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class ArticleCollectionActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val articles = mutableListOf<Article>()
    private var route = ""
    private var titleText = "文章列表"
    private var page = 1
    private val limit = 20
    private var loading = false

    private lateinit var searchInput: EditText
    private lateinit var listColumn: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var loadMoreText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        route = intent.getStringExtra(EXTRA_ROUTE).orEmpty()
        titleText = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { titleForRoute() }
        setContentView(buildContent())
        loadArticles(isPage = false)
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = titleText,
                leftIcon = R.drawable.ic_back,
                rightIcon = if (isMine()) R.drawable.ic_add else R.drawable.ic_search,
                onLeftClick = { finish() },
                onRightClick = {
                    if (isMine()) startActivity(Intent(this, ArticleEditorActivity::class.java)) else search()
                },
            ),
        )
        root.addView(searchBar())
        val scroll = ScrollView(this).apply {
            setBackgroundColor(KkColors.background)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(scroll)
        listColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(22))
        }
        scroll.addView(listColumn)
        emptyText = TextView(this).apply {
            text = "暂无文章"
            applyBodyStyle(15f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(120))
        }
        loadMoreText = TextView(this).apply {
            text = "加载更多"
            applyBodyStyle(13f)
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(8))
            setOnClickListener { loadMore() }
        }
        return root
    }

    private fun searchBar(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(8), dp(8))
            setBackgroundColor(Color.WHITE)
            addView(ImageView(this@ArticleCollectionActivity).apply {
                setImageResource(R.drawable.ic_search)
                setColorFilter(KkColors.softMuted)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            })
            searchInput = EditText(this@ArticleCollectionActivity).apply {
                hint = "搜索文章标题、摘要..."
                setSingleLine(true)
                textSize = 14f
                background = null
                setTextColor(KkColors.text)
                setHintTextColor(KkColors.softMuted)
                layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                    leftMargin = dp(8)
                }
            }
            addView(searchInput)
            addView(TextView(this@ArticleCollectionActivity).apply {
                text = "搜索"
                applyTitleStyle(13f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.orange, 999)
                layoutParams = LinearLayout.LayoutParams(dp(58), dp(36))
                setOnClickListener { search() }
            })
        }
    }

    private fun search() {
        page = 1
        loadArticles(isPage = false)
    }

    private fun loadArticles(isPage: Boolean) {
        if (loading) return
        loading = true
        val nextPage = if (isPage) page + 1 else 1
        if (isPage) loadMoreText.text = "正在加载中..."
        lifecycleScope.launch {
            runCatching {
                repository.articles(
                    page = nextPage,
                    limit = limit,
                    keyword = searchInput.text?.toString().orEmpty().trim(),
                )
            }.onSuccess { list ->
                val normalized = normalizeList(list)
                if (!isPage) articles.clear()
                articles.addAll(normalized)
                page = nextPage
                renderList()
                loadMoreText.text = if (list.size < limit || route.contains("randlist")) "没有更多了" else "加载更多"
            }.onFailure {
                Toast.makeText(this@ArticleCollectionActivity, it.message ?: "文章加载失败", Toast.LENGTH_SHORT).show()
                loadMoreText.text = "加载更多"
                renderList()
            }
            loading = false
        }
    }

    private fun normalizeList(list: List<Article>): List<Article> {
        return when {
            route.contains("randlist") -> list.shuffled().take(12)
            route.contains("recommend") -> list.sortedWith(compareByDescending<Article> { it.views() }.thenByDescending { it.id })
            route.contains("userpost") -> {
                val uid = repository.cachedUser()?.id ?: 0L
                if (uid > 0L) list.filter { it.authorId == null || it.authorId == uid } else list
            }
            else -> list
        }
    }

    private fun renderList() {
        listColumn.removeAllViews()
        if (articles.isEmpty()) {
            listColumn.addView(emptyText)
            return
        }
        articles.forEach { listColumn.addView(articleCard(it)) }
        listColumn.addView(loadMoreText)
    }

    private fun articleCard(article: Article): View {
        return MaterialCardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            setOnClickListener { openArticle(article) }
            addView(LinearLayout(this@ArticleCollectionActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(10))
                addView(LinearLayout(this@ArticleCollectionActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    val coverUrl = repository.absoluteUrl(article.coverPath())
                    addView(ImageView(this@ArticleCollectionActivity).apply {
                        isVisible = coverUrl.isNotBlank()
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setBackgroundResource(R.drawable.bg_image_placeholder)
                        layoutParams = LinearLayout.LayoutParams(dp(103), dp(76)).apply {
                            rightMargin = if (coverUrl.isNotBlank()) dp(10) else 0
                        }
                        if (coverUrl.isNotBlank()) {
                            Glide.with(this).load(coverUrl).placeholder(R.drawable.bg_image_placeholder).centerCrop().into(this)
                        }
                    })
                    addView(LinearLayout(this@ArticleCollectionActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        addView(TextView(this@ArticleCollectionActivity).apply {
                            text = article.title.ifBlank { "未命名文章" }
                            applyTitleStyle(15.5f)
                            maxLines = 2
                            setLineSpacing(dp(2).toFloat(), 1f)
                        })
                        addView(TextView(this@ArticleCollectionActivity).apply {
                            text = article.summary?.takeIf { it.isNotBlank() } ?: "暂无摘要"
                            applyBodyStyle(12.5f)
                            setTextColor(KkColors.text)
                            setPadding(0, dp(6), 0, 0)
                            maxLines = 2
                        })
                        addView(metaRow(article))
                    })
                })
                if (isMine()) {
                    addView(LinearLayout(this@ArticleCollectionActivity).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, dp(12), 0, 0)
                        addView(TextView(this@ArticleCollectionActivity).apply {
                            text = "ID ${article.id}"
                            applyBodyStyle(11.5f)
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        })
                        addView(actionButton("编辑", Color.parseColor("#2F80ED")) { openEditor(article) })
                    })
                }
            })
        }
    }

    private fun metaRow(article: Article): View {
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(9), 0, 0)
            addView(TextView(this@ArticleCollectionActivity).apply {
                text = article.typeName?.takeIf { it.isNotBlank() } ?: "文章"
                applyTitleStyle(11f)
                setTextColor(KkColors.orange)
                gravity = Gravity.CENTER
                setRoundedBackground(Color.parseColor("#FFF1E8"), 999)
                setPadding(dp(7), dp(3), dp(7), dp(3))
                maxLines = 1
            })
            addView(TextView(this@ArticleCollectionActivity).apply {
                text = article.displayTime().ifBlank { "刚刚" }
                applyBodyStyle(11f)
                setTextColor(KkColors.softMuted)
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = dp(8)
                }
            })
            addView(TextView(this@ArticleCollectionActivity).apply {
                text = "${article.views()} 浏览"
                applyBodyStyle(11f)
                setTextColor(KkColors.softMuted)
            })
        }
    }

    private fun actionButton(label: String, color: Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            applyTitleStyle(12.5f)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setRoundedBackground(color, 999)
            layoutParams = LinearLayout.LayoutParams(dp(68), dp(34))
            setOnClickListener {
                onClick()
            }
        }
    }

    private fun openArticle(article: Article) {
        if (article.id <= 0L) return
        startActivity(
            Intent(this, ArticleDetailActivity::class.java)
                .putExtra(ArticleDetailActivity.EXTRA_ARTICLE_ID, article.id),
        )
    }

    private fun openEditor(article: Article) {
        if (article.id <= 0L) return
        startActivity(
            Intent(this, ArticleEditorActivity::class.java)
                .putExtra(ArticleEditorActivity.EXTRA_ARTICLE_ID, article.id),
        )
    }

    private fun loadMore() {
        if (loadMoreText.text == "没有更多了") return
        loadArticles(isPage = true)
    }

    private fun isMine(): Boolean = route.contains("userpost")

    private fun titleForRoute(): String {
        return when {
            route.contains("userpost") -> "我的文章"
            route.contains("recommend") -> "推荐文章"
            route.contains("randlist") -> "随机阅读"
            else -> "文章列表"
        }
    }

    companion object {
        const val EXTRA_ROUTE = "extra_route"
        const val EXTRA_TITLE = "extra_title"
    }
}
