package cc.kkano.blog.ui.manage

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.ui.article.ArticleEditorActivity
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.common.setRoundedBackground
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class ArticleManagerActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val articles = mutableListOf<JsonObject>()
    private val tabs = listOf("waiting" to "待审核", "publish" to "已发布")
    private var status = "waiting"
    private var page = 1
    private val limit = 20
    private var loading = false

    private lateinit var searchInput: EditText
    private lateinit var tabsRow: LinearLayout
    private lateinit var listColumn: LinearLayout
    private lateinit var loadMoreText: TextView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                title = "文章管理",
                leftIcon = R.drawable.ic_back,
                rightIcon = R.drawable.ic_write,
                onLeftClick = { finish() },
                onRightClick = {
                    startActivity(Intent(this, ArticleEditorActivity::class.java))
                },
            ),
        )
        root.addView(searchBar())
        tabsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(0), dp(10), dp(10))
            setBackgroundColor(Color.WHITE)
        }
        root.addView(tabsRow)
        renderTabs()

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
            text = "暂时没有文章"
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
            addView(ImageView(this@ArticleManagerActivity).apply {
                setImageResource(R.drawable.ic_search)
                setColorFilter(KkColors.softMuted)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            })
            searchInput = EditText(this@ArticleManagerActivity).apply {
                hint = "输入搜索关键字"
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
            addView(TextView(this@ArticleManagerActivity).apply {
                text = "搜索"
                applyTitleStyle(13f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.orange, 999)
                layoutParams = LinearLayout.LayoutParams(dp(58), dp(36))
                setOnClickListener {
                    page = 1
                    loadArticles(isPage = false)
                }
            })
        }
    }

    private fun renderTabs() {
        tabsRow.removeAllViews()
        tabs.forEach { (value, label) ->
            tabsRow.addView(TextView(this).apply {
                text = label
                applyTitleStyle(13.5f)
                gravity = Gravity.CENTER
                setTextColor(if (status == value) Color.WHITE else KkColors.text)
                setRoundedBackground(if (status == value) KkColors.black else Color.parseColor("#F2F3F5"), 999)
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
                setOnClickListener {
                    if (status == value) return@setOnClickListener
                    status = value
                    page = 1
                    articles.clear()
                    renderTabs()
                    renderList()
                    loadArticles(isPage = false)
                }
            })
        }
    }

    private fun loadArticles(isPage: Boolean) {
        if (loading) return
        loading = true
        val nextPage = if (isPage) page + 1 else 1
        if (isPage) loadMoreText.text = "正在加载中..."
        lifecycleScope.launch {
            runCatching {
                repository.adminArticlesRoot(
                    status = status,
                    page = nextPage,
                    limit = limit,
                    keyword = searchInput.text?.toString().orEmpty().trim(),
                )
            }.onSuccess { root ->
                val list = parseList(root)
                if (!isPage) articles.clear()
                articles.addAll(list)
                page = nextPage
                renderList()
                loadMoreText.text = if (list.size < limit) "没有更多了" else "加载更多"
            }.onFailure {
                Toast.makeText(this@ArticleManagerActivity, it.message ?: "文章加载失败", Toast.LENGTH_SHORT).show()
                loadMoreText.text = "加载更多"
                renderList()
            }
            loading = false
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

    private fun articleCard(item: JsonObject): View {
        val id = idOf(item)
        return MaterialCardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            addView(LinearLayout(this@ArticleManagerActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                addView(LinearLayout(this@ArticleManagerActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(coverView(item))
                    addView(LinearLayout(this@ArticleManagerActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            leftMargin = dp(10)
                        }
                        addView(TextView(this@ArticleManagerActivity).apply {
                            text = display(item, "title").ifBlank { "未命名文章" }
                            applyTitleStyle(15.5f)
                            maxLines = 2
                            setLineSpacing(dp(2).toFloat(), 1f)
                        })
                        addView(TextView(this@ArticleManagerActivity).apply {
                            text = display(item, "summary").ifBlank { display(item, "description") }.ifBlank { "暂无摘要" }
                            applyBodyStyle(12.5f)
                            setPadding(0, dp(7), 0, 0)
                            maxLines = 2
                        })
                    })
                })
                addView(LinearLayout(this@ArticleManagerActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(10), 0, 0)
                    addView(statusPill(statusLabel(item), if (isWaiting(item)) KkColors.orange else Color.parseColor("#2FB344")))
                    addView(TextView(this@ArticleManagerActivity).apply {
                        text = listOf(
                            display(item, "author_name").ifBlank { display(item, "author") },
                            display(item, "created_at").ifBlank { display(item, "create_time") },
                            "${display(item, "view_count").ifBlank { display(item, "count").ifBlank { "0" } }} 浏览",
                        ).filter { it.isNotBlank() }.joinToString(" · ")
                        applyBodyStyle(11.5f)
                        maxLines = 1
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            leftMargin = dp(8)
                        }
                    })
                })
                addView(actionsRow(item, id))
            })
        }
    }

    private fun coverView(item: JsonObject): View {
        val url = display(item, "home_img").ifBlank { display(item, "cover") }
        return FrameLayout(this).apply {
            setRoundedBackground(Color.parseColor("#F0F1F3"), 10)
            layoutParams = LinearLayout.LayoutParams(dp(94), dp(72))
            if (url.isBlank()) {
                addView(ImageView(this@ArticleManagerActivity).apply {
                    setImageResource(R.drawable.ic_write)
                    setColorFilter(KkColors.softMuted)
                    setPadding(dp(22), dp(18), dp(22), dp(18))
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                })
            } else {
                addView(ImageView(this@ArticleManagerActivity).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    Glide.with(this).load(repository.absoluteUrl(url)).placeholder(R.drawable.bg_image_placeholder).centerCrop().into(this)
                })
            }
        }
    }

    private fun actionsRow(item: JsonObject, id: Long): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, 0)
            addView(LinearLayout(this@ArticleManagerActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                if (isWaiting(item)) {
                    addView(actionButton("快捷审核", KkColors.orange) { confirmStatus(id, "publish") })
                } else {
                    addView(actionButton(if (flag(item, "is_recommend", "isrecommend")) "取消推荐" else "推荐", KkColors.orange) {
                        toggleFlag(id, "is_recommend", !flag(item, "is_recommend", "isrecommend"))
                    })
                    addView(actionButton(if (flag(item, "is_top", "istop")) "取消置顶" else "置顶", KkColors.black) {
                        toggleFlag(id, "is_top", !flag(item, "is_top", "istop"))
                    })
                }
                addView(actionButton("编辑", Color.parseColor("#2F80ED")) {
                    startActivity(
                        Intent(this@ArticleManagerActivity, ArticleEditorActivity::class.java)
                            .putExtra(ArticleEditorActivity.EXTRA_ARTICLE_ID, id),
                    )
                })
                addView(actionButton("删除", KkColors.danger) { confirmDelete(item, id) })
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
            layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                setMargins(dp(3), 0, dp(3), 0)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun statusPill(label: String, color: Int): TextView {
        return TextView(this).apply {
            text = label
            applyTitleStyle(11.5f)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setRoundedBackground(color, 999)
            setPadding(dp(9), dp(4), dp(9), dp(4))
        }
    }

    private fun confirmStatus(id: Long, targetStatus: String) {
        AlertDialog.Builder(this)
            .setTitle("审核文章")
            .setMessage("确定通过这篇文章吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("通过") { _, _ ->
                updateArticle(id, mapOf("status" to targetStatus, "is_visible" to true))
            }
            .show()
    }

    private fun confirmDelete(item: JsonObject, id: Long) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定删除「${display(item, "title")}」吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    runCatching { repository.deleteArticle(id) }
                        .onSuccess {
                            Toast.makeText(this@ArticleManagerActivity, "删除成功", Toast.LENGTH_SHORT).show()
                            page = 1
                            loadArticles(isPage = false)
                        }
                        .onFailure { Toast.makeText(this@ArticleManagerActivity, it.message ?: "删除失败", Toast.LENGTH_SHORT).show() }
                }
            }
            .show()
    }

    private fun toggleFlag(id: Long, key: String, enabled: Boolean) {
        val legacyKey = if (key == "is_recommend") "isrecommend" else "istop"
        updateArticle(id, mapOf(key to enabled, legacyKey to if (enabled) 1 else 0))
    }

    private fun updateArticle(id: Long, body: Map<String, Any?>) {
        lifecycleScope.launch {
            runCatching { repository.updateArticle(id, body) }
                .onSuccess {
                    Toast.makeText(this@ArticleManagerActivity, "操作成功", Toast.LENGTH_SHORT).show()
                    page = 1
                    loadArticles(isPage = false)
                }
                .onFailure { Toast.makeText(this@ArticleManagerActivity, it.message ?: "操作失败", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun loadMore() {
        if (loadMoreText.text == "没有更多了") return
        loadArticles(isPage = true)
    }

    private fun parseList(root: JsonObject): List<JsonObject> {
        val data = root["data"]?.takeIf { it.isJsonObject }?.asJsonObject ?: root
        val listElement = data["list"] ?: data["data"]
        if (listElement != null && listElement.isJsonArray) {
            return listElement.asJsonArray.mapNotNull { it.takeIf { element -> element.isJsonObject }?.asJsonObject }
        }
        return emptyList()
    }

    private fun isWaiting(item: JsonObject): Boolean {
        val value = display(item, "status")
        return value == "waiting" || value == "pending" || value == "0"
    }

    private fun statusLabel(item: JsonObject): String {
        return when (display(item, "status")) {
            "waiting", "pending", "0" -> "待审核"
            "publish", "published", "1" -> "已发布"
            else -> if (status == "waiting") "待审核" else "已发布"
        }
    }

    private fun flag(item: JsonObject, key: String, legacyKey: String): Boolean {
        val value = item[key] ?: item[legacyKey]
        return runCatching { value?.asBoolean }.getOrNull()
            ?: runCatching { value?.asInt == 1 }.getOrNull()
            ?: false
    }

    private fun idOf(item: JsonObject): Long {
        return runCatching { item["id"]?.asLong }.getOrNull()
            ?: runCatching { item["cid"]?.asLong }.getOrNull()
            ?: 0L
    }

    private fun display(item: JsonObject, key: String): String {
        return repository.displayValue(item[key])
    }
}
