package cc.kkano.blog.ui.friend

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.common.setRoundedBackground
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class FriendLinkManagerActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val links = mutableListOf<JsonObject>()
    private val tabs = listOf(
        FriendTab(0, "待审核"),
        FriendTab(1, "已通过"),
        FriendTab(2, "已拒绝"),
    )
    private var currentStatus = 0
    private var page = 1
    private val limit = 20
    private var loading = false

    private lateinit var tabsRow: LinearLayout
    private lateinit var listColumn: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var loadMoreText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        loadLinks(isPage = false)
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = "友链管理",
                leftIcon = R.drawable.ic_back,
                onLeftClick = { finish() },
            ),
        )
        tabsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setBackgroundColor(Color.WHITE)
        }
        root.addView(tabsRow)
        renderTabs()

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            setBackgroundColor(KkColors.background)
        }
        root.addView(scroll)
        listColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(22))
        }
        scroll.addView(listColumn)
        emptyText = TextView(this).apply {
            text = "暂无友链数据"
            applyBodyStyle(15f)
            gravity = Gravity.CENTER
            visibility = View.GONE
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

    private fun renderTabs() {
        tabsRow.removeAllViews()
        tabs.forEach { tab ->
            tabsRow.addView(TextView(this).apply {
                text = tab.label
                applyTitleStyle(13.5f)
                gravity = Gravity.CENTER
                setTextColor(if (tab.status == currentStatus) Color.WHITE else KkColors.text)
                setRoundedBackground(
                    if (tab.status == currentStatus) KkColors.black else Color.parseColor("#F2F3F5"),
                    999,
                )
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
                setOnClickListener {
                    if (currentStatus == tab.status) return@setOnClickListener
                    currentStatus = tab.status
                    page = 1
                    links.clear()
                    renderTabs()
                    renderList()
                    loadLinks(isPage = false)
                }
            })
        }
    }

    private fun loadLinks(isPage: Boolean) {
        if (loading) return
        loading = true
        val nextPage = if (isPage) page + 1 else 1
        if (isPage) loadMoreText.text = "正在加载中..."
        lifecycleScope.launch {
            runCatching { repository.friendLinksRoot(currentStatus, nextPage, limit) }
                .onSuccess { root ->
                    val list = parseList(root)
                    if (!isPage) links.clear()
                    links.addAll(list)
                    page = nextPage
                    renderList()
                    loadMoreText.text = if (list.size < limit) "没有更多了" else "加载更多"
                }
                .onFailure {
                    Toast.makeText(this@FriendLinkManagerActivity, it.message ?: "友链加载失败", Toast.LENGTH_SHORT).show()
                    loadMoreText.text = "加载更多"
                    renderList()
                }
            loading = false
        }
    }

    private fun renderList() {
        listColumn.removeAllViews()
        emptyText.visibility = if (links.isEmpty()) View.VISIBLE else View.GONE
        if (links.isEmpty()) listColumn.addView(emptyText)
        links.forEach { item -> listColumn.addView(linkCard(item)) }
        if (links.isNotEmpty()) listColumn.addView(loadMoreText)
    }

    private fun linkCard(item: JsonObject): View {
        return MaterialCardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            addView(LinearLayout(this@FriendLinkManagerActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                addView(LinearLayout(this@FriendLinkManagerActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(linkAvatar(item))
                    addView(LinearLayout(this@FriendLinkManagerActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            leftMargin = dp(10)
                        }
                        addView(TextView(this@FriendLinkManagerActivity).apply {
                            text = display(item, "name").ifBlank { "未命名站点" }
                            applyTitleStyle(15.5f)
                            maxLines = 1
                        })
                        addView(TextView(this@FriendLinkManagerActivity).apply {
                            text = display(item, "url")
                            applyBodyStyle(12f)
                            setPadding(0, dp(5), 0, 0)
                            maxLines = 1
                        })
                    })
                    if (longValue(item, "is_top") == 1L) {
                        addView(statusPill("置顶", KkColors.orange, Color.parseColor("#FFF1E8")))
                    }
                })
                addView(TextView(this@FriendLinkManagerActivity).apply {
                    text = display(item, "description").ifBlank { "暂无描述" }
                    applyBodyStyle(13f)
                    setTextColor(KkColors.text)
                    setRoundedBackground(Color.parseColor("#F7F8FA"), 10)
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = dp(12)
                    }
                })
                addView(TextView(this@FriendLinkManagerActivity).apply {
                    text = listOf(
                        "排序 ${display(item, "sort_order").ifBlank { "0" }}",
                        display(item, "rss_url").takeIf { it.isNotBlank() }?.let { "RSS $it" }.orEmpty(),
                        display(item, "created_at").takeIf { it.isNotBlank() }?.let { "申请 $it" }.orEmpty(),
                    ).filter { it.isNotBlank() }.joinToString(" · ")
                    applyBodyStyle(11.5f)
                    setPadding(0, dp(10), 0, 0)
                    maxLines = 2
                })
                addView(actionsRow(item))
            })
        }
    }

    private fun linkAvatar(item: JsonObject): View {
        val url = display(item, "avatar").ifBlank { display(item, "icon") }
        return FrameLayout(this).apply {
            background = roundedDrawable(Color.parseColor("#F0F1F3"), 999)
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            addView(ImageView(this@FriendLinkManagerActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(if (url.isBlank()) dp(12) else 0, if (url.isBlank()) dp(12) else 0, if (url.isBlank()) dp(12) else 0, if (url.isBlank()) dp(12) else 0)
                if (url.isBlank()) {
                    setImageResource(R.drawable.ic_link)
                    setColorFilter(KkColors.softMuted)
                } else {
                    Glide.with(this).load(repository.absoluteUrl(url)).placeholder(R.drawable.bg_avatar).centerCrop().into(this)
                }
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            })
        }
    }

    private fun actionsRow(item: JsonObject): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, 0)
            addView(LinearLayout(this@FriendLinkManagerActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(actionButton("编辑", KkColors.black) { showEditSheet(item) })
                if (currentStatus != 1) {
                    addView(actionButton("通过", Color.parseColor("#2FB344")) { confirmAudit(item, 1) })
                }
                if (currentStatus != 2) {
                    addView(actionButton("拒绝", KkColors.danger) { confirmAudit(item, 2) })
                }
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

    private fun statusPill(label: String, color: Int, bg: Int): TextView {
        return TextView(this).apply {
            text = label
            applyTitleStyle(11f)
            setTextColor(color)
            gravity = Gravity.CENTER
            setRoundedBackground(bg, 999)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
    }

    private fun confirmAudit(item: JsonObject, status: Int) {
        val id = idOf(item)
        val action = if (status == 1) "通过" else "拒绝"
        AlertDialog.Builder(this)
            .setTitle("确认$action")
            .setMessage("确定要${action}友链「${display(item, "name")}」吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton(action) { _, _ -> auditLink(id, status) }
            .show()
    }

    private fun auditLink(id: Long, status: Int) {
        if (id <= 0L) return
        lifecycleScope.launch {
            runCatching { repository.auditFriendLink(id, status) }
                .onSuccess {
                    Toast.makeText(this@FriendLinkManagerActivity, if (status == 1) "已通过" else "已拒绝", Toast.LENGTH_SHORT).show()
                    page = 1
                    loadLinks(isPage = false)
                }
                .onFailure {
                    Toast.makeText(this@FriendLinkManagerActivity, it.message ?: "操作失败", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showEditSheet(item: JsonObject) {
        val dialog = BottomSheetDialog(this)
        val name = editInput("请输入网站名称", display(item, "name"))
        val url = editInput("请输入网站地址", display(item, "url"))
        val avatar = editInput("请输入网站图标URL", display(item, "avatar"))
        val rss = editInput("请输入RSS地址", display(item, "rss_url"))
        val desc = editInput("请输入网站描述", display(item, "description"))
        val sort = editInput("数字越小越靠前", display(item, "sort_order").ifBlank { "0" }).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(Color.WHITE, 24)
            setPadding(dp(16), dp(14), dp(16), dp(18))
            addView(TextView(this@FriendLinkManagerActivity).apply {
                text = "编辑友链"
                applyTitleStyle(18f)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(12))
            })
            addView(editRow("网站名称", name))
            addView(editRow("网站地址", url))
            addView(editRow("网站图标", avatar))
            addView(editRow("RSS地址", rss))
            addView(editRow("网站描述", desc))
            addView(editRow("排序", sort))
            addView(TextView(this@FriendLinkManagerActivity).apply {
                text = "保存修改"
                applyTitleStyle(15f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.orange, 999)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)).apply {
                    topMargin = dp(14)
                }
                setOnClickListener {
                    saveLink(
                        id = idOf(item),
                        body = mapOf(
                            "name" to name.text?.toString().orEmpty().trim(),
                            "url" to url.text?.toString().orEmpty().trim(),
                            "avatar" to avatar.text?.toString().orEmpty().trim(),
                            "rss_url" to rss.text?.toString().orEmpty().trim(),
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
            addView(TextView(this@FriendLinkManagerActivity).apply {
                text = label
                applyTitleStyle(13f)
                layoutParams = LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }
    }

    private fun saveLink(id: Long, body: Map<String, Any?>, dialog: BottomSheetDialog) {
        if (body["name"].toString().isBlank() || body["url"].toString().isBlank()) {
            Toast.makeText(this, "网站名称和网站地址不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            runCatching { repository.updateFriendLink(id, body) }
                .onSuccess {
                    Toast.makeText(this@FriendLinkManagerActivity, "保存成功", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    page = 1
                    loadLinks(isPage = false)
                }
                .onFailure {
                    Toast.makeText(this@FriendLinkManagerActivity, it.message ?: "保存失败", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadMore() {
        if (loadMoreText.text == "没有更多了") return
        loadLinks(isPage = true)
    }

    private fun parseList(root: JsonObject): List<JsonObject> {
        val data = root["data"]?.takeIf { it.isJsonObject }?.asJsonObject ?: root
        val listElement = data["list"] ?: data["data"]
        if (listElement != null && listElement.isJsonArray) {
            return listElement.asJsonArray.mapNotNull { it.takeIf { element -> element.isJsonObject }?.asJsonObject }
        }
        return emptyList()
    }

    private fun idOf(item: JsonObject): Long {
        return longValue(item, "id")
    }

    private fun longValue(item: JsonObject, key: String): Long {
        return runCatching { item[key]?.asLong }.getOrNull() ?: 0L
    }

    private fun display(item: JsonObject, key: String): String {
        return repository.displayValue(item[key])
    }

    private data class FriendTab(val status: Int, val label: String)
}
