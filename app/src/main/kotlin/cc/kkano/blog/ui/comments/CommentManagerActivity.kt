package cc.kkano.blog.ui.comments

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Html
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
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class CommentManagerActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val comments = mutableListOf<JsonObject>()
    private val tabs = listOf(
        CommentTab(1, "文章评论"),
        CommentTab(2, "留言板"),
        CommentTab(3, "动态评论"),
    )
    private var currentType = 1
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
        loadComments(isPage = false)
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = "评论管理",
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
            text = "暂无评论"
            applyBodyStyle(15f)
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(120))
        }
        listColumn.addView(emptyText)

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
                setTextColor(if (tab.type == currentType) Color.WHITE else KkColors.text)
                setRoundedBackground(
                    if (tab.type == currentType) KkColors.black else Color.parseColor("#F2F3F5"),
                    999,
                )
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
                setOnClickListener {
                    if (currentType == tab.type) return@setOnClickListener
                    currentType = tab.type
                    page = 1
                    comments.clear()
                    renderTabs()
                    renderList()
                    loadComments(isPage = false)
                }
            })
        }
    }

    private fun loadComments(isPage: Boolean) {
        if (loading) return
        loading = true
        val nextPage = if (isPage) page + 1 else 1
        if (isPage) loadMoreText.text = "正在加载中..."
        lifecycleScope.launch {
            runCatching { repository.commentsRoot(currentType, nextPage, limit) }
                .onSuccess { root ->
                    val list = parseList(root)
                    if (!isPage) comments.clear()
                    comments.addAll(list)
                    page = nextPage
                    renderList()
                    loadMoreText.text = if (list.size < limit) "没有更多了" else "加载更多"
                }
                .onFailure {
                    Toast.makeText(this@CommentManagerActivity, it.message ?: "评论加载失败", Toast.LENGTH_SHORT).show()
                    loadMoreText.text = "加载更多"
                    renderList()
                }
            loading = false
        }
    }

    private fun renderList() {
        listColumn.removeAllViews()
        emptyText.visibility = if (comments.isEmpty()) View.VISIBLE else View.GONE
        if (comments.isEmpty()) listColumn.addView(emptyText)
        comments.forEach { item -> listColumn.addView(commentCard(item)) }
        if (comments.isNotEmpty()) listColumn.addView(loadMoreText)
    }

    private fun commentCard(item: JsonObject): View {
        return MaterialCardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            addView(LinearLayout(this@CommentManagerActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                addView(LinearLayout(this@CommentManagerActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(avatarView(avatarOf(item), displayName(item)))
                    addView(LinearLayout(this@CommentManagerActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            leftMargin = dp(10)
                        }
                        addView(TextView(this@CommentManagerActivity).apply {
                            text = displayName(item)
                            applyTitleStyle(15f)
                            maxLines = 1
                        })
                        addView(TextView(this@CommentManagerActivity).apply {
                            text = display(item, "created_at").ifBlank { display(item, "created") }
                            applyBodyStyle(11.5f)
                            setPadding(0, dp(5), 0, 0)
                        })
                    })
                    addView(TextView(this@CommentManagerActivity).apply {
                        text = tabs.firstOrNull { it.type == typeOf(item) }?.label ?: "评论"
                        applyTitleStyle(11.5f)
                        setTextColor(KkColors.orange)
                        gravity = Gravity.CENTER
                        setRoundedBackground(Color.parseColor("#FFF1E8"), 999)
                        setPadding(dp(9), dp(4), dp(9), dp(4))
                    })
                })
                addView(TextView(this@CommentManagerActivity).apply {
                    text = Html.fromHtml(contentOf(item), Html.FROM_HTML_MODE_COMPACT)
                    applyBodyStyle(14f)
                    setTextColor(KkColors.text)
                    setLineSpacing(dp(2).toFloat(), 1.05f)
                    setPadding(0, dp(12), 0, 0)
                })
                sourceTitle(item).takeIf { it.isNotBlank() }?.let { source ->
                    addView(TextView(this@CommentManagerActivity).apply {
                        text = source
                        applyBodyStyle(12f)
                        setTextColor(KkColors.muted)
                        setRoundedBackground(Color.parseColor("#F7F8FA"), 10)
                        setPadding(dp(10), dp(8), dp(10), dp(8))
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            topMargin = dp(10)
                        }
                    })
                }
                replyArray(item).takeIf { it.size() > 0 }?.let { replies ->
                    addView(replyPreview(replies))
                }
                addView(LinearLayout(this@CommentManagerActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(12), 0, 0)
                    addView(TextView(this@CommentManagerActivity).apply {
                        text = "ID ${idOf(item)}"
                        applyBodyStyle(11.5f)
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    addView(actionButton("回复") { showReplySheet(item) })
                    addView(actionButton("删除", KkColors.danger) { confirmDelete(item) })
                })
            })
        }
    }

    private fun avatarView(url: String, name: String): View {
        return FrameLayout(this).apply {
            background = roundedDrawable(Color.parseColor("#F0F1F3"), 999)
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            if (url.isBlank()) {
                addView(TextView(this@CommentManagerActivity).apply {
                    text = name.take(1).ifBlank { "评" }
                    applyTitleStyle(16f)
                    gravity = Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                })
            } else {
                addView(ImageView(this@CommentManagerActivity).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    Glide.with(this).load(repository.absoluteUrl(url)).placeholder(R.drawable.bg_avatar).centerCrop().into(this)
                })
            }
        }
    }

    private fun replyPreview(replies: JsonArray): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setRoundedBackground(Color.parseColor("#FAFAFB"), 10, Color.parseColor("#EDEFF3"), 1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10)
            }
            addView(TextView(this@CommentManagerActivity).apply {
                text = "回复 ${replies.size()}"
                applyTitleStyle(12f)
                setTextColor(KkColors.black)
            })
            replies.take(3).forEach { element ->
                if (element.isJsonObject) {
                    val reply = element.asJsonObject
                    addView(TextView(this@CommentManagerActivity).apply {
                        text = "${displayName(reply)}：${Html.fromHtml(contentOf(reply), Html.FROM_HTML_MODE_COMPACT)}"
                        applyBodyStyle(12.5f)
                        setTextColor(KkColors.text)
                        setPadding(0, dp(6), 0, 0)
                        maxLines = 2
                    })
                }
            }
        }
    }

    private fun actionButton(label: String, color: Int = KkColors.orange, onClick: () -> Unit): TextView {
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

    private fun showReplySheet(item: JsonObject) {
        val dialog = BottomSheetDialog(this)
        val input = EditText(this).apply {
            hint = "回复 ${displayName(item)}"
            minLines = 4
            maxLines = 7
            gravity = Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setTextColor(KkColors.text)
            setHintTextColor(KkColors.softMuted)
            background = roundedDrawable(Color.parseColor("#F7F8FA"), 12, KkColors.line, 1)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(Color.WHITE, 24)
            setPadding(dp(16), dp(14), dp(16), dp(18))
            addView(TextView(this@CommentManagerActivity).apply {
                text = "回复评论"
                applyTitleStyle(18f)
                gravity = Gravity.CENTER
            })
            addView(TextView(this@CommentManagerActivity).apply {
                text = Html.fromHtml(contentOf(item), Html.FROM_HTML_MODE_COMPACT)
                applyBodyStyle(13f)
                setTextColor(KkColors.muted)
                setRoundedBackground(Color.parseColor("#F7F8FA"), 12)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(14)
                }
                maxLines = 4
            })
            addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(128)).apply {
                topMargin = dp(12)
            })
            addView(TextView(this@CommentManagerActivity).apply {
                text = "发布回复"
                applyTitleStyle(15f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.orange, 999)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)).apply {
                    topMargin = dp(14)
                }
                setOnClickListener { submitReply(item, input.text?.toString().orEmpty(), dialog) }
            })
        }
        dialog.setContentView(root)
        dialog.show()
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.window?.setDimAmount(0f)
    }

    private fun submitReply(item: JsonObject, content: String, dialog: BottomSheetDialog) {
        val text = content.trim()
        if (text.isBlank()) {
            Toast.makeText(this, "请输入回复内容", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            runCatching {
                repository.createComment(
                    buildMap {
                        put("type", typeOf(item))
                        put("content", text)
                        put("parent_id", idOf(item))
                        val targetId = longValue(item, "target_id")
                        if (targetId > 0L) put("target_id", targetId)
                    },
                )
            }.onSuccess {
                Toast.makeText(this@CommentManagerActivity, "回复成功", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                page = 1
                loadComments(isPage = false)
            }.onFailure {
                Toast.makeText(this@CommentManagerActivity, it.message ?: "回复失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDelete(item: JsonObject) {
        val id = idOf(item)
        if (id <= 0L) {
            Toast.makeText(this, "没有可删除的评论 ID", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定删除「${displayName(item)}」的这条评论吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    runCatching { repository.deleteComment(id) }
                        .onSuccess {
                            Toast.makeText(this@CommentManagerActivity, "删除成功", Toast.LENGTH_SHORT).show()
                            page = 1
                            loadComments(isPage = false)
                        }
                        .onFailure { Toast.makeText(this@CommentManagerActivity, it.message ?: "删除失败", Toast.LENGTH_SHORT).show() }
                }
            }
            .show()
    }

    private fun loadMore() {
        if (loadMoreText.text == "没有更多了") return
        loadComments(isPage = true)
    }

    private fun parseList(root: JsonObject): List<JsonObject> {
        val data = root["data"]?.takeIf { it.isJsonObject }?.asJsonObject ?: root
        val listElement = data["list"] ?: data["data"]
        if (listElement != null && listElement.isJsonArray) {
            return listElement.asJsonArray.mapNotNull { it.takeIf { element -> element.isJsonObject }?.asJsonObject }
        }
        return emptyList()
    }

    private fun replyArray(item: JsonObject): JsonArray {
        listOf("replies", "children", "reply_list").forEach { key ->
            val value = item[key]
            if (value != null && value.isJsonArray) return value.asJsonArray
        }
        return JsonArray()
    }

    private fun typeOf(item: JsonObject): Int {
        return runCatching { item["type"]?.asInt }.getOrNull() ?: currentType
    }

    private fun idOf(item: JsonObject): Long {
        return longValue(item, "id").takeIf { it > 0L }
            ?: longValue(item, "coid").takeIf { it > 0L }
            ?: 0L
    }

    private fun longValue(item: JsonObject, key: String): Long {
        return runCatching { item[key]?.asLong }.getOrNull() ?: 0L
    }

    private fun displayName(item: JsonObject): String {
        val user = item["user"]?.takeIf { it.isJsonObject }?.asJsonObject
        return display(item, "nickname")
            .ifBlank { display(item, "author") }
            .ifBlank { user?.let { display(it, "nickname").ifBlank { display(it, "username") } }.orEmpty() }
            .ifBlank { "匿名用户" }
    }

    private fun avatarOf(item: JsonObject): String {
        val user = item["user"]?.takeIf { it.isJsonObject }?.asJsonObject
        return display(item, "avatar").ifBlank { user?.let { display(it, "avatar") }.orEmpty() }
    }

    private fun contentOf(item: JsonObject): String {
        return display(item, "content").ifBlank { display(item, "text") }
    }

    private fun sourceTitle(item: JsonObject): String {
        val target = item["target"]?.takeIf { it.isJsonObject }?.asJsonObject
        return display(item, "target_title")
            .ifBlank { display(item, "contenTitle") }
            .ifBlank { target?.let { display(it, "title").ifBlank { display(it, "content") } }.orEmpty() }
    }

    private fun display(item: JsonObject, key: String): String {
        return repository.displayValue(item[key])
    }

    private data class CommentTab(val type: Int, val label: String)
}
