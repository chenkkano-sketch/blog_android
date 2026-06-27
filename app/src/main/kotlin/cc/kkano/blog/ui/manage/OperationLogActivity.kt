package cc.kkano.blog.ui.manage

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

class OperationLogActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val logs = mutableListOf<JsonObject>()
    private val tabs = listOf(
        "" to "全部",
        "info" to "信息",
        "warning" to "警告",
        "error" to "异常",
    )
    private var currentLevel = ""
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
        loadLogs(isPage = false)
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = "操作日志",
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
            text = "暂无操作日志"
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
            addView(ImageView(this@OperationLogActivity).apply {
                setImageResource(R.drawable.ic_search)
                setColorFilter(KkColors.softMuted)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            })
            searchInput = EditText(this@OperationLogActivity).apply {
                hint = "搜索用户、路径、动作..."
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
            addView(TextView(this@OperationLogActivity).apply {
                text = "搜索"
                applyTitleStyle(13f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.orange, 999)
                layoutParams = LinearLayout.LayoutParams(dp(58), dp(36))
                setOnClickListener {
                    page = 1
                    loadLogs(isPage = false)
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
                setTextColor(if (currentLevel == value) Color.WHITE else KkColors.text)
                setRoundedBackground(if (currentLevel == value) KkColors.black else Color.parseColor("#F2F3F5"), 999)
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
                setOnClickListener {
                    if (currentLevel == value) return@setOnClickListener
                    currentLevel = value
                    page = 1
                    logs.clear()
                    renderTabs()
                    renderList()
                    loadLogs(isPage = false)
                }
            })
        }
    }

    private fun loadLogs(isPage: Boolean) {
        if (loading) return
        loading = true
        val nextPage = if (isPage) page + 1 else 1
        if (isPage) loadMoreText.text = "正在加载中..."
        lifecycleScope.launch {
            runCatching {
                repository.operationLogsRoot(
                    level = currentLevel,
                    page = nextPage,
                    limit = limit,
                    keyword = searchInput.text?.toString().orEmpty().trim(),
                )
            }.onSuccess { root ->
                val list = parseList(root)
                if (!isPage) logs.clear()
                logs.addAll(list)
                page = nextPage
                renderList()
                loadMoreText.text = if (list.size < limit) "没有更多了" else "加载更多"
            }.onFailure {
                Toast.makeText(this@OperationLogActivity, it.message ?: "日志加载失败", Toast.LENGTH_SHORT).show()
                loadMoreText.text = "加载更多"
                renderList()
            }
            loading = false
        }
    }

    private fun renderList() {
        listColumn.removeAllViews()
        if (logs.isEmpty()) {
            listColumn.addView(emptyText)
            return
        }
        logs.forEach { listColumn.addView(logCard(it)) }
        listColumn.addView(loadMoreText)
    }

    private fun logCard(item: JsonObject): View {
        val level = levelOf(item)
        return MaterialCardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            addView(LinearLayout(this@OperationLogActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                addView(LinearLayout(this@OperationLogActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(ImageView(this@OperationLogActivity).apply {
                        setImageResource(if (level == "error") R.drawable.ic_delete else R.drawable.ic_settings)
                        setColorFilter(levelColor(level))
                        background = roundedDrawable(levelBackground(level), 999)
                        setPadding(dp(10), dp(10), dp(10), dp(10))
                        layoutParams = LinearLayout.LayoutParams(dp(42), dp(42)).apply {
                            rightMargin = dp(10)
                        }
                    })
                    addView(LinearLayout(this@OperationLogActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        addView(TextView(this@OperationLogActivity).apply {
                            text = titleOf(item)
                            applyTitleStyle(15.5f)
                            maxLines = 1
                        })
                        addView(TextView(this@OperationLogActivity).apply {
                            text = subtitleOf(item)
                            applyBodyStyle(12f)
                            setPadding(0, dp(5), 0, 0)
                            maxLines = 1
                        })
                    })
                    addView(levelPill(level))
                })
                addView(metaStrip(item))
                addView(LinearLayout(this@OperationLogActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(12), 0, 0)
                    addView(TextView(this@OperationLogActivity).apply {
                        text = timeOf(item).ifBlank { "ID ${idOf(item)}" }
                        applyBodyStyle(11.5f)
                        maxLines = 1
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    addView(actionButton("详情") { showDetailSheet(item) })
                })
            })
        }
    }

    private fun metaStrip(item: JsonObject): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
            addView(metaBox("用户", userOf(item).ifBlank { "系统" }))
            addView(metaBox("状态", statusOf(item).ifBlank { "-" }))
            addView(metaBox("IP", display(item, "ip").ifBlank { display(item, "ip_address").ifBlank { "-" } }))
        }
    }

    private fun metaBox(label: String, value: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setRoundedBackground(Color.parseColor("#F7F8FA"), 11)
            setPadding(dp(7), dp(8), dp(7), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(3), 0, dp(3), 0)
            }
            addView(TextView(this@OperationLogActivity).apply {
                text = label
                applyBodyStyle(11f)
                gravity = Gravity.CENTER
            })
            addView(TextView(this@OperationLogActivity).apply {
                text = value
                applyTitleStyle(12.5f)
                gravity = Gravity.CENTER
                maxLines = 1
                setPadding(0, dp(6), 0, 0)
            })
        }
    }

    private fun showDetailSheet(item: JsonObject) {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(Color.WHITE, 24)
            setPadding(dp(16), dp(14), dp(16), dp(18))
            addView(TextView(this@OperationLogActivity).apply {
                text = "日志详情"
                applyTitleStyle(18f)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(12))
            })
            addView(detailRow("动作", titleOf(item)))
            addView(detailRow("用户", userOf(item).ifBlank { "系统" }))
            addView(detailRow("级别", levelLabel(levelOf(item))))
            addView(detailRow("请求", requestLine(item)))
            addView(detailRow("状态", statusOf(item).ifBlank { "-" }))
            addView(detailRow("IP", display(item, "ip").ifBlank { display(item, "ip_address").ifBlank { "-" } }))
            addView(detailRow("时间", timeOf(item).ifBlank { "-" }))
            detailText(item).takeIf { it.isNotBlank() }?.let {
                addView(TextView(this@OperationLogActivity).apply {
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
            addView(TextView(this@OperationLogActivity).apply {
                text = label
                applyTitleStyle(13f)
                layoutParams = LinearLayout.LayoutParams(dp(58), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(TextView(this@OperationLogActivity).apply {
                text = value
                applyBodyStyle(13f)
                setTextColor(KkColors.text)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
    }

    private fun levelPill(level: String): TextView {
        return TextView(this).apply {
            text = levelLabel(level)
            applyTitleStyle(11.5f)
            setTextColor(levelColor(level))
            gravity = Gravity.CENTER
            setRoundedBackground(levelBackground(level), 999)
            setPadding(dp(9), dp(4), dp(9), dp(4))
        }
    }

    private fun actionButton(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            applyTitleStyle(12.5f)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setRoundedBackground(KkColors.orange, 999)
            layoutParams = LinearLayout.LayoutParams(dp(68), dp(34))
            setOnClickListener { onClick() }
        }
    }

    private fun loadMore() {
        if (loadMoreText.text == "没有更多了") return
        loadLogs(isPage = true)
    }

    private fun parseList(root: JsonObject): List<JsonObject> {
        val data = root["data"]?.takeIf { it.isJsonObject }?.asJsonObject ?: root
        val listElement = data["list"] ?: data["data"] ?: data["records"]
        if (listElement != null && listElement.isJsonArray) {
            return listElement.asJsonArray.mapNotNull { it.takeIf { element -> element.isJsonObject }?.asJsonObject }
        }
        return emptyList()
    }

    private fun titleOf(item: JsonObject): String {
        return display(item, "action")
            .ifBlank { display(item, "title") }
            .ifBlank { display(item, "operation") }
            .ifBlank { display(item, "module") }
            .ifBlank { requestLine(item).ifBlank { "后台操作" } }
    }

    private fun subtitleOf(item: JsonObject): String {
        return display(item, "description")
            .ifBlank { display(item, "message") }
            .ifBlank { requestLine(item) }
            .ifBlank { detailText(item) }
            .ifBlank { "记录了一次后台操作" }
    }

    private fun requestLine(item: JsonObject): String {
        val method = display(item, "method").ifBlank { display(item, "request_method") }
        val path = display(item, "path").ifBlank { display(item, "url").ifBlank { display(item, "route") } }
        return listOf(method, path).filter { it.isNotBlank() }.joinToString(" ")
    }

    private fun userOf(item: JsonObject): String {
        val user = item["user"]?.takeIf { it.isJsonObject }?.asJsonObject
        return display(item, "username")
            .ifBlank { display(item, "nickname") }
            .ifBlank { display(item, "operator") }
            .ifBlank { user?.let { display(it, "nickname").ifBlank { display(it, "username") } }.orEmpty() }
    }

    private fun detailText(item: JsonObject): String {
        return display(item, "content")
            .ifBlank { display(item, "params") }
            .ifBlank { display(item, "request_body") }
            .ifBlank { display(item, "response") }
    }

    private fun levelOf(item: JsonObject): String {
        val raw = display(item, "level")
            .ifBlank { display(item, "type") }
            .ifBlank { display(item, "status") }
            .lowercase()
        return when {
            raw.contains("error") || raw.contains("fail") || raw.contains("异常") -> "error"
            raw.contains("warn") || raw.contains("警告") -> "warning"
            else -> "info"
        }
    }

    private fun levelLabel(level: String): String {
        return when (level) {
            "error" -> "异常"
            "warning" -> "警告"
            else -> "信息"
        }
    }

    private fun levelColor(level: String): Int {
        return when (level) {
            "error" -> KkColors.danger
            "warning" -> KkColors.orange
            else -> Color.parseColor("#2F80ED")
        }
    }

    private fun levelBackground(level: String): Int {
        return when (level) {
            "error" -> Color.parseColor("#FFEFF1")
            "warning" -> Color.parseColor("#FFF1E8")
            else -> Color.parseColor("#EAF3FF")
        }
    }

    private fun statusOf(item: JsonObject): String {
        return display(item, "status_code")
            .ifBlank { display(item, "http_status") }
            .ifBlank { display(item, "code") }
    }

    private fun timeOf(item: JsonObject): String {
        return display(item, "created_at")
            .ifBlank { display(item, "created") }
            .ifBlank { display(item, "time") }
            .ifBlank { display(item, "updated_at") }
    }

    private fun idOf(item: JsonObject): Long {
        return runCatching { item["id"]?.asLong }.getOrNull() ?: 0L
    }

    private fun display(item: JsonObject, key: String): String {
        return repository.displayValue(item[key])
    }
}
