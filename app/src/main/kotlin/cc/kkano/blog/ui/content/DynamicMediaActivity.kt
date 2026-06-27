package cc.kkano.blog.ui.content

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.data.api.ApiRoutes
import cc.kkano.blog.data.model.Dynamic
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.setRoundedBackground
import cc.kkano.blog.ui.comments.CommentComposerActivity
import cc.kkano.blog.ui.dynamics.DynamicAdapter
import cc.kkano.blog.ui.dynamics.DynamicEditorActivity
import cc.kkano.blog.ui.login.LoginActivity
import kotlinx.coroutines.launch

class DynamicMediaActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val allItems = mutableListOf<Dynamic>()
    private var currentTab = "all"
    private var page = 1
    private val limit = 20
    private var loading = false

    private lateinit var searchInput: EditText
    private lateinit var tabsRow: LinearLayout
    private lateinit var adapter: DynamicAdapter
    private lateinit var emptyText: TextView
    private lateinit var loadMoreText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        loadEmojiMap()
        loadDynamics(isPage = false)
    }

    override fun onResume() {
        super.onResume()
        adapter.setCurrentUserId(repository.cachedUser()?.id ?: 0L)
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = "动态媒体",
                leftIcon = R.drawable.ic_back,
                rightIcon = R.drawable.ic_add,
                onLeftClick = { finish() },
                onRightClick = { startActivity(Intent(this, DynamicEditorActivity::class.java)) },
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

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(container)
        emptyText = TextView(this).apply {
            text = "暂无动态媒体"
            applyBodyStyle(15f)
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(100))
        }
        container.addView(emptyText)
        val recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@DynamicMediaActivity)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        container.addView(recycler)
        adapter = DynamicAdapter(
            onEdit = { openEditor(it) },
            onDelete = { confirmDelete(it) },
            onLike = { likeDynamic(it) },
            onComment = { openComment(it) },
        ).apply {
            setCurrentUserId(repository.cachedUser()?.id ?: 0L)
        }
        recycler.adapter = adapter
        loadMoreText = TextView(this).apply {
            text = "加载更多"
            applyBodyStyle(13f)
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, dp(14))
            setOnClickListener { loadMore() }
        }
        container.addView(loadMoreText)
        return root
    }

    private fun searchBar(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(8), dp(8))
            setBackgroundColor(Color.WHITE)
            addView(ImageView(this@DynamicMediaActivity).apply {
                setImageResource(R.drawable.ic_search)
                setColorFilter(KkColors.softMuted)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            })
            searchInput = EditText(this@DynamicMediaActivity).apply {
                hint = "搜索动态内容"
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
            addView(TextView(this@DynamicMediaActivity).apply {
                text = "搜索"
                applyTitleStyle(13f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.orange, 999)
                layoutParams = LinearLayout.LayoutParams(dp(58), dp(36))
                setOnClickListener { renderList() }
            })
        }
    }

    private fun renderTabs() {
        val tabs = listOf("all" to "全部", "image" to "图片", "video" to "视频", "link" to "外链")
        tabsRow.removeAllViews()
        tabs.forEach { (value, label) ->
            tabsRow.addView(TextView(this).apply {
                text = label
                applyTitleStyle(13.5f)
                gravity = Gravity.CENTER
                setTextColor(if (currentTab == value) Color.WHITE else KkColors.text)
                setRoundedBackground(if (currentTab == value) KkColors.black else Color.parseColor("#F2F3F5"), 999)
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
                setOnClickListener {
                    if (currentTab == value) return@setOnClickListener
                    currentTab = value
                    renderTabs()
                    renderList()
                }
            })
        }
    }

    private fun loadDynamics(isPage: Boolean) {
        if (loading) return
        loading = true
        val nextPage = if (isPage) page + 1 else 1
        if (isPage) loadMoreText.text = "正在加载中..."
        lifecycleScope.launch {
            runCatching { repository.dynamics(nextPage, limit) }
                .onSuccess { list ->
                    if (!isPage) allItems.clear()
                    allItems.addAll(list)
                    page = nextPage
                    renderList()
                    loadMoreText.text = if (list.size < limit) "没有更多了" else "加载更多"
                }
                .onFailure {
                    Toast.makeText(this@DynamicMediaActivity, it.message ?: "动态加载失败", Toast.LENGTH_SHORT).show()
                    loadMoreText.text = "加载更多"
                    renderList()
                }
            loading = false
        }
    }

    private fun renderList() {
        val keyword = searchInput.text?.toString().orEmpty().trim()
        val list = allItems.filter { item ->
            val matchesTab = when (currentTab) {
                "image" -> item.imagePaths().isNotEmpty()
                "video" -> !item.bvid.isNullOrBlank()
                "link" -> !item.externalUrl.isNullOrBlank()
                else -> true
            }
            val text = listOf(item.title, item.content, item.externalTitle).joinToString(" ")
            matchesTab && (keyword.isBlank() || text.contains(keyword, ignoreCase = true))
        }
        adapter.submitList(list)
        emptyText.isVisible = list.isEmpty()
    }

    private fun openEditor(dynamic: Dynamic) {
        startActivity(
            Intent(this, DynamicEditorActivity::class.java)
                .putExtra(DynamicEditorActivity.EXTRA_DYNAMIC_ID, dynamic.id),
        )
    }

    private fun confirmDelete(dynamic: Dynamic) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除这条动态吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    runCatching { repository.deleteDynamic(dynamic.id) }
                        .onSuccess {
                            Toast.makeText(this@DynamicMediaActivity, "删除成功", Toast.LENGTH_SHORT).show()
                            page = 1
                            loadDynamics(isPage = false)
                        }
                        .onFailure { Toast.makeText(this@DynamicMediaActivity, it.message ?: "删除失败", Toast.LENGTH_SHORT).show() }
                }
            }
            .show()
    }

    private fun likeDynamic(dynamic: Dynamic) {
        if (repository.cachedUser() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }
        lifecycleScope.launch {
            runCatching { repository.likeDynamic(dynamic.id) }
                .onSuccess {
                    page = 1
                    loadDynamics(isPage = false)
                }
                .onFailure { Toast.makeText(this@DynamicMediaActivity, it.message ?: "点赞失败", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun openComment(dynamic: Dynamic) {
        startActivity(
            Intent(this, CommentComposerActivity::class.java)
                .putExtra(CommentComposerActivity.EXTRA_TITLE, "评论动态")
                .putExtra(CommentComposerActivity.EXTRA_TYPE, 3)
                .putExtra(CommentComposerActivity.EXTRA_TARGET_ID, dynamic.id),
        )
    }

    private fun loadEmojiMap() {
        lifecycleScope.launch {
            runCatching { repository.genericList(ApiRoutes.EMOJI, limit = 100) }
                .onSuccess { groups ->
                    val map = buildMap {
                        groups.forEach { group ->
                            val groupName = repository.displayValue(group["name"])
                            val items = group["items"]?.takeIf { it.isJsonArray }?.asJsonArray ?: return@forEach
                            items.forEach { item ->
                                if (item.isJsonObject) {
                                    val obj = item.asJsonObject
                                    val name = repository.displayValue(obj["name"])
                                    val url = repository.displayValue(obj["url"])
                                    if (groupName.isNotBlank() && name.isNotBlank() && url.isNotBlank()) {
                                        put("[${groupName}_${name}]", repository.absoluteUrl(url))
                                    }
                                }
                            }
                        }
                    }
                    adapter.setEmojiMap(map)
                }
        }
    }

    private fun loadMore() {
        if (loadMoreText.text == "没有更多了") return
        loadDynamics(isPage = true)
    }
}
