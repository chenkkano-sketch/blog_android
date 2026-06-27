package cc.kkano.blog.ui.manage

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
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

class MetaManagerActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val items = mutableListOf<JsonObject>()
    private val tabs = listOf("category" to "分类", "tag" to "标签")
    private var type = "category"
    private var page = 1
    private val limit = 20
    private var loading = false

    private lateinit var searchInput: EditText
    private lateinit var tabsRow: LinearLayout
    private lateinit var listColumn: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var loadMoreText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        loadItems(isPage = false)
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
                rightIcon = R.drawable.ic_add,
                onLeftClick = { finish() },
                onRightClick = { showEditSheet(null) },
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
            text = "暂时没有数据"
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
            addView(ImageView(this@MetaManagerActivity).apply {
                setImageResource(R.drawable.ic_search)
                setColorFilter(KkColors.softMuted)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            })
            searchInput = EditText(this@MetaManagerActivity).apply {
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
            addView(TextView(this@MetaManagerActivity).apply {
                text = "搜索"
                applyTitleStyle(13f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.orange, 999)
                layoutParams = LinearLayout.LayoutParams(dp(58), dp(36))
                setOnClickListener {
                    page = 1
                    loadItems(isPage = false)
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
                setTextColor(if (type == value) Color.WHITE else KkColors.text)
                setRoundedBackground(if (type == value) KkColors.black else Color.parseColor("#F2F3F5"), 999)
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
                setOnClickListener {
                    if (type == value) return@setOnClickListener
                    type = value
                    page = 1
                    items.clear()
                    renderTabs()
                    renderList()
                    loadItems(isPage = false)
                }
            })
        }
    }

    private fun loadItems(isPage: Boolean) {
        if (loading) return
        loading = true
        val nextPage = if (isPage) page + 1 else 1
        if (isPage) loadMoreText.text = "正在加载中..."
        lifecycleScope.launch {
            runCatching {
                repository.genericList(
                    endpoint = endpoint(),
                    page = nextPage,
                    limit = limit,
                    keyword = searchInput.text?.toString().orEmpty().trim(),
                )
            }.onSuccess { list ->
                if (!isPage) items.clear()
                items.addAll(list)
                page = nextPage
                renderList()
                loadMoreText.text = if (list.size < limit) "没有更多了" else "加载更多"
            }.onFailure {
                Toast.makeText(this@MetaManagerActivity, it.message ?: "加载失败", Toast.LENGTH_SHORT).show()
                loadMoreText.text = "加载更多"
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
        listColumn.addView(loadMoreText)
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
            addView(LinearLayout(this@MetaManagerActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                addView(LinearLayout(this@MetaManagerActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(TextView(this@MetaManagerActivity).apply {
                        text = display(item, "name").ifBlank { display(item, "title").ifBlank { "未命名" } }
                        applyTitleStyle(16f)
                        maxLines = 1
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    addView(TextView(this@MetaManagerActivity).apply {
                        text = "ID ${idOf(item)}"
                        applyTitleStyle(11.5f)
                        setTextColor(KkColors.orange)
                        gravity = Gravity.CENTER
                        setRoundedBackground(Color.parseColor("#FFF1E8"), 999)
                        setPadding(dp(9), dp(4), dp(9), dp(4))
                    })
                })
                addView(TextView(this@MetaManagerActivity).apply {
                    text = display(item, "description").ifBlank { display(item, "slug").ifBlank { "暂无描述" } }
                    applyBodyStyle(13f)
                    setTextColor(KkColors.text)
                    setRoundedBackground(Color.parseColor("#F7F8FA"), 10)
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = dp(10)
                    }
                })
                addView(TextView(this@MetaManagerActivity).apply {
                    text = listOf(
                        if (type == "category") "分类" else "标签",
                        "文章 ${display(item, "count").ifBlank { display(item, "article_count").ifBlank { "0" } }}",
                        display(item, "created_at"),
                    ).filter { it.isNotBlank() }.joinToString(" · ")
                    applyBodyStyle(11.5f)
                    setPadding(0, dp(10), 0, 0)
                })
                addView(LinearLayout(this@MetaManagerActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(12), 0, 0)
                    addView(actionButton("编辑", Color.parseColor("#2F80ED")) { showEditSheet(item) })
                    addView(actionButton(if (flag(item)) "取消推荐" else "推荐", KkColors.orange) {
                        toggleRecommend(item, !flag(item))
                    })
                    addView(actionButton("删除", KkColors.danger) { confirmDelete(item) })
                })
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

    private fun showEditSheet(item: JsonObject?) {
        val dialog = BottomSheetDialog(this)
        val name = editInput("请输入${if (type == "category") "分类" else "标签"}名称", item?.let { display(it, "name") }.orEmpty())
        val slug = editInput("请输入别名，可留空", item?.let { display(it, "slug") }.orEmpty())
        val desc = editInput("请输入描述", item?.let { display(it, "description") }.orEmpty())
        val sort = editInput("排序，数字越小越靠前", item?.let { display(it, "sort_order") }.orEmpty()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(Color.WHITE, 24)
            setPadding(dp(16), dp(14), dp(16), dp(18))
            addView(TextView(this@MetaManagerActivity).apply {
                text = if (item == null) "新增${label()}" else "编辑${label()}"
                applyTitleStyle(18f)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(12))
            })
            addView(editRow("名称", name))
            addView(editRow("别名", slug))
            addView(editRow("描述", desc))
            addView(editRow("排序", sort))
            addView(TextView(this@MetaManagerActivity).apply {
                text = if (item == null) "新增" else "保存修改"
                applyTitleStyle(15f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.orange, 999)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)).apply {
                    topMargin = dp(14)
                }
                setOnClickListener {
                    saveMeta(
                        id = item?.let { idOf(it) } ?: 0L,
                        body = mapOf(
                            "name" to name.text?.toString().orEmpty().trim(),
                            "slug" to slug.text?.toString().orEmpty().trim(),
                            "description" to desc.text?.toString().orEmpty().trim(),
                            "sort_order" to (sort.text?.toString().orEmpty().trim().toIntOrNull() ?: 0),
                        ),
                        dialog = dialog,
                    )
                }
            })
        }
        dialog.setContentView(root)
        dialog.show()
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.window?.setDimAmount(0f)
    }

    private fun editInput(hintText: String, value: String): EditText {
        return EditText(this).apply {
            hint = hintText
            setText(value)
            setSingleLine(true)
            textSize = 14f
            setTextColor(KkColors.text)
            setHintTextColor(KkColors.softMuted)
            background = null
        }
    }

    private fun editRow(label: String, input: EditText): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setRoundedBackground(Color.parseColor("#F7F8FA"), 11)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply {
                topMargin = dp(9)
            }
            addView(TextView(this@MetaManagerActivity).apply {
                text = label
                applyTitleStyle(13f)
                layoutParams = LinearLayout.LayoutParams(dp(52), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }
    }

    private fun saveMeta(id: Long, body: Map<String, Any?>, dialog: BottomSheetDialog) {
        if (body["name"].toString().isBlank()) {
            Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val result = if (id > 0L) {
                runCatching { repository.put("${endpoint().trimEnd('/')}/$id", body) }
            } else {
                runCatching { repository.post(endpoint(), body) }
            }
            result.onSuccess {
                Toast.makeText(this@MetaManagerActivity, "保存成功", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                page = 1
                loadItems(isPage = false)
            }.onFailure {
                Toast.makeText(this@MetaManagerActivity, it.message ?: "保存失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleRecommend(item: JsonObject, enabled: Boolean) {
        val id = idOf(item)
        lifecycleScope.launch {
            runCatching {
                repository.put(
                    "${endpoint().trimEnd('/')}/$id",
                    mapOf("is_recommend" to enabled, "isrecommend" to if (enabled) 1 else 0),
                )
            }.onSuccess {
                Toast.makeText(this@MetaManagerActivity, "操作成功", Toast.LENGTH_SHORT).show()
                page = 1
                loadItems(isPage = false)
            }.onFailure {
                Toast.makeText(this@MetaManagerActivity, it.message ?: "操作失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDelete(item: JsonObject) {
        val id = idOf(item)
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定删除「${display(item, "name")}」吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    runCatching { repository.delete("${endpoint().trimEnd('/')}/$id") }
                        .onSuccess {
                            Toast.makeText(this@MetaManagerActivity, "删除成功", Toast.LENGTH_SHORT).show()
                            page = 1
                            loadItems(isPage = false)
                        }
                        .onFailure { Toast.makeText(this@MetaManagerActivity, it.message ?: "删除失败", Toast.LENGTH_SHORT).show() }
                }
            }
            .show()
    }

    private fun loadMore() {
        if (loadMoreText.text == "没有更多了") return
        loadItems(isPage = true)
    }

    private fun endpoint(): String {
        return if (type == "category") ApiRoutes.ARTICLE_CATEGORIES else ApiRoutes.TAGS
    }

    private fun label(): String {
        return if (type == "category") "分类" else "标签"
    }

    private fun flag(item: JsonObject): Boolean {
        val value = item["is_recommend"] ?: item["isrecommend"]
        return runCatching { value?.asBoolean }.getOrNull()
            ?: runCatching { value?.asInt == 1 }.getOrNull()
            ?: false
    }

    private fun idOf(item: JsonObject): Long {
        return runCatching { item["id"]?.asLong }.getOrNull()
            ?: runCatching { item["mid"]?.asLong }.getOrNull()
            ?: 0L
    }

    private fun display(item: JsonObject, key: String): String {
        return repository.displayValue(item[key])
    }
}
