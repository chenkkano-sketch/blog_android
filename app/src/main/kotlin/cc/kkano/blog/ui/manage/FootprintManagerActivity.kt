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

class FootprintManagerActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val items = mutableListOf<JsonObject>()
    private val tabs = listOf("" to "全部", "1" to "已发布", "0" to "草稿")
    private var currentStatus = ""
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
        loadFootprints(isPage = false)
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = "足迹管理",
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
            text = "暂无足迹记录"
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
            addView(ImageView(this@FootprintManagerActivity).apply {
                setImageResource(R.drawable.ic_search)
                setColorFilter(KkColors.softMuted)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            })
            searchInput = EditText(this@FootprintManagerActivity).apply {
                hint = "搜索城市、省份..."
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
            addView(TextView(this@FootprintManagerActivity).apply {
                text = "搜索"
                applyTitleStyle(13f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.orange, 999)
                layoutParams = LinearLayout.LayoutParams(dp(58), dp(36))
                setOnClickListener {
                    page = 1
                    loadFootprints(isPage = false)
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
                setTextColor(if (currentStatus == value) Color.WHITE else KkColors.text)
                setRoundedBackground(if (currentStatus == value) KkColors.black else Color.parseColor("#F2F3F5"), 999)
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
                setOnClickListener {
                    if (currentStatus == value) return@setOnClickListener
                    currentStatus = value
                    page = 1
                    items.clear()
                    renderTabs()
                    renderList()
                    loadFootprints(isPage = false)
                }
            })
        }
    }

    private fun loadFootprints(isPage: Boolean) {
        if (loading) return
        loading = true
        val nextPage = if (isPage) page + 1 else 1
        if (isPage) loadMoreText.text = "正在加载中..."
        lifecycleScope.launch {
            runCatching {
                repository.footprintsRoot(
                    status = currentStatus,
                    page = nextPage,
                    limit = limit,
                    keyword = searchInput.text?.toString().orEmpty().trim(),
                )
            }.onSuccess { root ->
                val list = parseList(root)
                if (!isPage) items.clear()
                items.addAll(list)
                page = nextPage
                renderList()
                loadMoreText.text = if (list.size < limit) "没有更多了" else "加载更多"
            }.onFailure {
                Toast.makeText(this@FootprintManagerActivity, it.message ?: "足迹加载失败", Toast.LENGTH_SHORT).show()
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
        items.forEach { listColumn.addView(footprintCard(it)) }
        listColumn.addView(loadMoreText)
    }

    private fun footprintCard(item: JsonObject): View {
        return MaterialCardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            addView(LinearLayout(this@FootprintManagerActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                addView(LinearLayout(this@FootprintManagerActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(ImageView(this@FootprintManagerActivity).apply {
                        setImageResource(R.drawable.ic_location)
                        setColorFilter(KkColors.orange)
                        layoutParams = LinearLayout.LayoutParams(dp(25), dp(25)).apply {
                            rightMargin = dp(8)
                        }
                    })
                    addView(TextView(this@FootprintManagerActivity).apply {
                        text = "${display(item, "province")} · ${display(item, "city")}".trim(' ', '·')
                        applyTitleStyle(16f)
                        maxLines = 1
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    addView(statusPill(if (isPublished(item)) "已发布" else "草稿", if (isPublished(item)) Color.parseColor("#2FB344") else KkColors.orange))
                })
                addView(infoGrid(item))
                addView(LinearLayout(this@FootprintManagerActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(12), 0, 0)
                    addView(TextView(this@FootprintManagerActivity).apply {
                        text = display(item, "created_at").ifBlank { "刚刚创建" }
                        applyBodyStyle(11.5f)
                        maxLines = 1
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    addView(actionButton("编辑", Color.parseColor("#2F80ED")) { showEditSheet(item) })
                    addView(actionButton("删除", KkColors.danger) { confirmDelete(item) })
                })
            })
        }
    }

    private fun infoGrid(item: JsonObject): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
            addView(infoBox("出行类型", display(item, "trip_type").ifBlank { "未分类" }))
            addView(infoBox("访问日期", display(item, "visit_date").ifBlank { "-" }))
            addView(infoBox("排序", display(item, "sort_order").ifBlank { "0" }))
        }
    }

    private fun infoBox(label: String, value: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setRoundedBackground(Color.parseColor("#F7F8FA"), 11)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(3), 0, dp(3), 0)
            }
            addView(TextView(this@FootprintManagerActivity).apply {
                text = label
                applyBodyStyle(11f)
                gravity = Gravity.CENTER
            })
            addView(TextView(this@FootprintManagerActivity).apply {
                text = value
                applyTitleStyle(12.5f)
                gravity = Gravity.CENTER
                maxLines = 1
                setPadding(0, dp(6), 0, 0)
            })
        }
    }

    private fun showEditSheet(item: JsonObject?) {
        val dialog = BottomSheetDialog(this)
        val province = editInput("请输入省份", item?.let { display(it, "province") }.orEmpty())
        val city = editInput("请输入城市", item?.let { display(it, "city") }.orEmpty())
        val date = editInput("例如 2026-06-27", item?.let { display(it, "visit_date").replace("/", "-") }.orEmpty())
        val tripType = editInput("如：旅游、出差、探亲", item?.let { display(it, "trip_type") }.orEmpty())
        val sort = editInput("数字越小越靠前", item?.let { display(it, "sort_order") }.orEmpty()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        val statusInput = editInput("1 为已发布，0 为草稿", item?.let { if (isPublished(it)) "1" else "0" } ?: "1").apply {
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(Color.WHITE, 24)
            setPadding(dp(16), dp(14), dp(16), dp(18))
            addView(TextView(this@FootprintManagerActivity).apply {
                text = if (item == null) "新增足迹" else "编辑足迹"
                applyTitleStyle(18f)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(12))
            })
            addView(editRow("省份", province))
            addView(editRow("城市", city))
            addView(editRow("访问日期", date))
            addView(editRow("出行类型", tripType))
            addView(editRow("排序", sort))
            addView(editRow("状态", statusInput))
            addView(TextView(this@FootprintManagerActivity).apply {
                text = if (item == null) "立即创建" else "保存修改"
                applyTitleStyle(15f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.orange, 999)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)).apply {
                    topMargin = dp(14)
                }
                setOnClickListener {
                    saveFootprint(
                        id = item?.let { idOf(it) } ?: 0L,
                        body = mapOf(
                            "province" to province.text?.toString().orEmpty().trim(),
                            "city" to city.text?.toString().orEmpty().trim(),
                            "visit_date" to date.text?.toString().orEmpty().trim(),
                            "trip_type" to tripType.text?.toString().orEmpty().trim(),
                            "sort_order" to (sort.text?.toString().orEmpty().trim().toIntOrNull() ?: 0),
                            "status" to (statusInput.text?.toString().orEmpty().trim().toIntOrNull() ?: 1),
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
            addView(TextView(this@FootprintManagerActivity).apply {
                text = label
                applyTitleStyle(13f)
                layoutParams = LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }
    }

    private fun saveFootprint(id: Long, body: Map<String, Any?>, dialog: BottomSheetDialog) {
        if (body["province"].toString().isBlank() || body["city"].toString().isBlank() || body["visit_date"].toString().isBlank()) {
            Toast.makeText(this, "省份、城市和访问日期不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            runCatching { repository.saveFootprint(id, body) }
                .onSuccess {
                    Toast.makeText(this@FootprintManagerActivity, "保存成功", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    page = 1
                    loadFootprints(isPage = false)
                }
                .onFailure { Toast.makeText(this@FootprintManagerActivity, it.message ?: "保存失败", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun confirmDelete(item: JsonObject) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定删除「${display(item, "province")} ${display(item, "city")}」足迹吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    runCatching { repository.deleteFootprint(idOf(item)) }
                        .onSuccess {
                            Toast.makeText(this@FootprintManagerActivity, "删除成功", Toast.LENGTH_SHORT).show()
                            page = 1
                            loadFootprints(isPage = false)
                        }
                        .onFailure { Toast.makeText(this@FootprintManagerActivity, it.message ?: "删除失败", Toast.LENGTH_SHORT).show() }
                }
            }
            .show()
    }

    private fun actionButton(label: String, color: Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            applyTitleStyle(12.5f)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setRoundedBackground(color, 999)
            layoutParams = LinearLayout.LayoutParams(dp(68), dp(34)).apply {
                leftMargin = dp(7)
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

    private fun loadMore() {
        if (loadMoreText.text == "没有更多了") return
        loadFootprints(isPage = true)
    }

    private fun parseList(root: JsonObject): List<JsonObject> {
        val data = root["data"]?.takeIf { it.isJsonObject }?.asJsonObject ?: root
        val listElement = data["list"] ?: data["data"]
        if (listElement != null && listElement.isJsonArray) {
            return listElement.asJsonArray.mapNotNull { it.takeIf { element -> element.isJsonObject }?.asJsonObject }
        }
        return emptyList()
    }

    private fun isPublished(item: JsonObject): Boolean {
        return runCatching { item["status"]?.asInt == 1 }.getOrNull()
            ?: display(item, "status").let { it == "1" || it == "publish" || it == "published" }
    }

    private fun idOf(item: JsonObject): Long {
        return runCatching { item["id"]?.asLong }.getOrNull() ?: 0L
    }

    private fun display(item: JsonObject, key: String): String {
        return repository.displayValue(item[key])
    }
}
