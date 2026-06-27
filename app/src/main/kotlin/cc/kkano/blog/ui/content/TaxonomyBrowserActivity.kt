package cc.kkano.blog.ui.content

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.data.api.ApiRoutes
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.common.setRoundedBackground
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class TaxonomyBrowserActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val items = mutableListOf<JsonObject>()
    private val tabs = listOf(
        MetaTab(ApiRoutes.ARTICLE_CATEGORIES, "分类", R.drawable.ic_tools),
        MetaTab(ApiRoutes.TAGS, "标签", R.drawable.ic_link),
    )
    private var currentTab = tabs.first()
    private var loading = false

    private lateinit var searchInput: EditText
    private lateinit var tabsRow: LinearLayout
    private lateinit var listColumn: LinearLayout
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentTab = if (intent.getStringExtra(EXTRA_ROUTE).orEmpty().contains("tag")) tabs[1] else tabs[0]
        setContentView(buildContent())
        loadItems()
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = "分类标签",
                leftIcon = R.drawable.ic_back,
                onLeftClick = { finish() },
            ),
        )
        root.addView(searchBar())
        tabsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(10), 0, dp(10), dp(10))
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
            text = "暂无分类标签"
            applyBodyStyle(15f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(120))
        }
        return root
    }

    private fun searchBar(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(8), dp(8))
            setBackgroundColor(Color.WHITE)
            addView(ImageView(this@TaxonomyBrowserActivity).apply {
                setImageResource(R.drawable.ic_search)
                setColorFilter(KkColors.softMuted)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            })
            searchInput = EditText(this@TaxonomyBrowserActivity).apply {
                hint = "搜索分类或标签"
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
            addView(TextView(this@TaxonomyBrowserActivity).apply {
                text = "搜索"
                applyTitleStyle(13f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.orange, 999)
                layoutParams = LinearLayout.LayoutParams(dp(58), dp(36))
                setOnClickListener { loadItems() }
            })
        }
    }

    private fun renderTabs() {
        tabsRow.removeAllViews()
        tabs.forEach { tab ->
            tabsRow.addView(TextView(this).apply {
                text = tab.label
                applyTitleStyle(13.5f)
                gravity = Gravity.CENTER
                setTextColor(if (tab == currentTab) Color.WHITE else KkColors.text)
                setRoundedBackground(if (tab == currentTab) KkColors.black else Color.parseColor("#F2F3F5"), 999)
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
                setOnClickListener {
                    if (currentTab == tab) return@setOnClickListener
                    currentTab = tab
                    items.clear()
                    renderTabs()
                    renderList()
                    loadItems()
                }
            })
        }
    }

    private fun loadItems() {
        if (loading) return
        loading = true
        lifecycleScope.launch {
            runCatching {
                repository.genericList(
                    endpoint = currentTab.endpoint,
                    limit = 100,
                    keyword = searchInput.text?.toString().orEmpty().trim(),
                )
            }.onSuccess { list ->
                items.clear()
                items.addAll(list)
                renderList()
            }.onFailure {
                Toast.makeText(this@TaxonomyBrowserActivity, it.message ?: "加载失败", Toast.LENGTH_SHORT).show()
                renderList()
            }
            loading = false
        }
    }

    private fun renderList() {
        listColumn.removeAllViews()
        if (items.isEmpty()) {
            listColumn.addView(emptyText)
            return
        }
        items.forEach { listColumn.addView(metaCard(it)) }
    }

    private fun metaCard(item: JsonObject): View {
        return MaterialCardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            setOnClickListener { showDetailSheet(item) }
            addView(LinearLayout(this@TaxonomyBrowserActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                addView(LinearLayout(this@TaxonomyBrowserActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(ImageView(this@TaxonomyBrowserActivity).apply {
                        setImageResource(currentTab.icon)
                        setColorFilter(KkColors.orange)
                        background = roundedDrawable(Color.parseColor("#FFF1E8"), 999)
                        setPadding(dp(10), dp(10), dp(10), dp(10))
                        layoutParams = LinearLayout.LayoutParams(dp(42), dp(42)).apply {
                            rightMargin = dp(10)
                        }
                    })
                    addView(LinearLayout(this@TaxonomyBrowserActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        addView(TextView(this@TaxonomyBrowserActivity).apply {
                            text = nameOf(item)
                            applyTitleStyle(16f)
                            maxLines = 1
                        })
                        addView(TextView(this@TaxonomyBrowserActivity).apply {
                            text = slugOf(item).ifBlank { descOf(item).ifBlank { "博客${currentTab.label}" } }
                            applyBodyStyle(12f)
                            setPadding(0, dp(5), 0, 0)
                            maxLines = 1
                        })
                    })
                    addView(countPill(countOf(item)))
                })
                descOf(item).takeIf { it.isNotBlank() }?.let { desc ->
                    addView(TextView(this@TaxonomyBrowserActivity).apply {
                        text = desc
                        applyBodyStyle(13f)
                        setTextColor(KkColors.text)
                        setRoundedBackground(Color.parseColor("#F7F8FA"), 11)
                        setPadding(dp(10), dp(8), dp(10), dp(8))
                        maxLines = 3
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            topMargin = dp(10)
                        }
                    })
                }
            })
        }
    }

    private fun countPill(count: String): TextView {
        return TextView(this).apply {
            text = if (count.isBlank()) "0 篇" else "$count 篇"
            applyTitleStyle(11.5f)
            setTextColor(KkColors.orange)
            gravity = Gravity.CENTER
            setRoundedBackground(Color.parseColor("#FFF1E8"), 999)
            setPadding(dp(9), dp(4), dp(9), dp(4))
        }
    }

    private fun showDetailSheet(item: JsonObject) {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(Color.WHITE, 24)
            setPadding(dp(16), dp(14), dp(16), dp(18))
            addView(TextView(this@TaxonomyBrowserActivity).apply {
                text = nameOf(item)
                applyTitleStyle(18f)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(12))
            })
            addView(detailRow("类型", currentTab.label))
            addView(detailRow("别名", slugOf(item).ifBlank { "-" }))
            addView(detailRow("文章", countOf(item).ifBlank { "0" }))
            addView(detailRow("排序", display(item, "sort_order").ifBlank { display(item, "order").ifBlank { "-" } }))
            descOf(item).takeIf { it.isNotBlank() }?.let {
                addView(TextView(this@TaxonomyBrowserActivity).apply {
                    text = it
                    applyBodyStyle(13f)
                    setTextColor(KkColors.text)
                    setRoundedBackground(Color.parseColor("#F7F8FA"), 12)
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = dp(10)
                    }
                })
            }
        }
        dialog.setContentView(root)
        dialog.show()
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.window?.setDimAmount(0f)
    }

    private fun detailRow(label: String, value: String): View {
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setRoundedBackground(Color.parseColor("#F7F8FA"), 11)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
            }
            addView(TextView(this@TaxonomyBrowserActivity).apply {
                text = label
                applyTitleStyle(13f)
                layoutParams = LinearLayout.LayoutParams(dp(58), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(TextView(this@TaxonomyBrowserActivity).apply {
                text = value
                applyBodyStyle(13f)
                setTextColor(KkColors.text)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
    }

    private fun nameOf(item: JsonObject): String {
        return display(item, "name")
            .ifBlank { display(item, "title") }
            .ifBlank { display(item, "label") }
            .ifBlank { "未命名${currentTab.label}" }
    }

    private fun slugOf(item: JsonObject): String {
        return display(item, "slug").ifBlank { display(item, "mid") }
    }

    private fun descOf(item: JsonObject): String {
        return display(item, "description")
            .ifBlank { display(item, "desc") }
            .ifBlank { display(item, "summary") }
    }

    private fun countOf(item: JsonObject): String {
        return display(item, "count")
            .ifBlank { display(item, "article_count") }
            .ifBlank { display(item, "post_count") }
    }

    private fun display(item: JsonObject, key: String): String {
        return repository.displayValue(item[key])
    }

    private data class MetaTab(
        val endpoint: String,
        val label: String,
        val icon: Int,
    )

    companion object {
        const val EXTRA_ROUTE = "extra_route"
    }
}
