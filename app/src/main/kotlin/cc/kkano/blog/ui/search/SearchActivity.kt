package cc.kkano.blog.ui.search

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.ui.article.ArticleDetailActivity
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyInnerCard
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.margin
import cc.kkano.blog.ui.common.setRoundedBackground
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val tabs = listOf(
        SearchTab("articles", "文章"),
        SearchTab("dynamics", "动态"),
        SearchTab("comments", "评论"),
    )
    private var activeTab = 0
    private var page = 1
    private var total = 0
    private var loading = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var searchInput: EditText
    private lateinit var tabRow: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var emptyText: TextView
    private lateinit var adapter: SearchAdapter
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
    }

    private fun buildContent(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F7FB"))
        }
        root.addView(
            kkTopBar(
                title = "博客搜索",
                leftIcon = R.drawable.ic_back,
                onLeftClick = { finish() },
            ),
        )

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), dp(12), dp(11), dp(8))
            setBackgroundColor(Color.parseColor("#F5F7FB"))
        }
        root.addView(panel)

        val searchBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
            setRoundedBackground(Color.WHITE, 12, Color.parseColor("#DCE5F2"), 1)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48),
            )
        }
        panel.addView(searchBox)
        searchBox.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_search)
            setColorFilter(KkColors.orange)
            layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))
        })
        searchInput = EditText(this).apply {
            hint = "搜索文章、动态、评论"
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            background = null
            textSize = 15f
            setTextColor(KkColors.text)
            setHintTextColor(Color.parseColor("#98A2B3"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                leftMargin = dp(10)
            }
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    runSearch(reset = true)
                    true
                } else {
                    false
                }
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    handler.removeCallbacksAndMessages(null)
                    handler.postDelayed({ runSearch(reset = true) }, 300)
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        searchBox.addView(searchInput)
        searchBox.addView(TextView(this).apply {
            text = "×"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#7A8496"))
            setRoundedBackground(Color.parseColor("#EDF2F7"), 999)
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
            setOnClickListener {
                searchInput.setText("")
                adapter.submitList(emptyList(), tabs[activeTab].key)
                statusText.text = "输入关键词开始搜索"
                emptyText.isVisible = true
            }
        })

        tabRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        panel.addView(tabRow)
        buildTabs()

        statusText = TextView(this).apply {
            text = "输入关键词开始搜索"
            applyBodyStyle(12.5f)
            setPadding(dp(3), dp(8), dp(3), dp(7))
        }
        panel.addView(statusText)

        val recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val manager = recyclerView.layoutManager as LinearLayoutManager
                    if (!loading && this@SearchActivity.adapter.itemCount > 0 && manager.findLastVisibleItemPosition() >= this@SearchActivity.adapter.itemCount - 3) {
                        if (this@SearchActivity.adapter.itemCount < total) runSearch(reset = false)
                    }
                }
            })
        }
        root.addView(recycler)
        adapter = SearchAdapter { item ->
            openResult(item)
        }
        recycler.adapter = adapter

        emptyText = TextView(this).apply {
            text = "输入关键词开始搜索"
            applyBodyStyle(15f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(96))
        }
        root.addView(emptyText)
        return root
    }

    private fun buildTabs() {
        tabRow.removeAllViews()
        tabs.forEachIndexed { index, tab ->
            tabRow.addView(TextView(this).apply {
                text = tab.label
                applyTitleStyle(13.5f)
                gravity = Gravity.CENTER
                setTextColor(if (index == activeTab) Color.WHITE else KkColors.muted)
                setRoundedBackground(if (index == activeTab) KkColors.orange else Color.WHITE, 10, Color.parseColor("#E3E9F2"), 1)
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                }
                setOnClickListener {
                    activeTab = index
                    buildTabs()
                    runSearch(reset = true)
                }
            })
        }
    }

    private fun runSearch(reset: Boolean) {
        val keyword = searchInput.text?.toString().orEmpty().trim()
        if (keyword.isBlank()) {
            page = 1
            total = 0
            adapter.submitList(emptyList(), tabs[activeTab].key)
            emptyText.isVisible = true
            statusText.text = "输入关键词开始搜索"
            return
        }
        if (loading) return
        loading = true
        if (reset) {
            page = 1
            adapter.submitList(emptyList(), tabs[activeTab].key)
        }
        statusText.text = "正在检索..."
        lifecycleScope.launch {
            runCatching { repository.search(tabs[activeTab].key, keyword, page, LIMIT) }
                .onSuccess { root ->
                    val data = root["data"]?.takeIf { it.isJsonObject }?.asJsonObject
                    val list = data?.get("list")?.takeIf { it.isJsonArray }?.asJsonArray
                        ?.mapNotNull { it.takeIf { element -> element.isJsonObject }?.asJsonObject }
                        ?: emptyList()
                    total = data?.get("total")?.asInt ?: list.size
                    if (reset) adapter.submitList(list, tabs[activeTab].key) else adapter.addList(list)
                    page++
                    emptyText.isVisible = adapter.itemCount == 0
                    statusText.text = "${tabs[activeTab].label} · ${total} 条结果"
                }
                .onFailure {
                    Snackbar.make(root, it.message ?: "搜索失败", Snackbar.LENGTH_SHORT).show()
                    statusText.text = "搜索失败"
                }
            loading = false
        }
    }

    private fun openResult(item: JsonObject) {
        val module = tabs[activeTab].key
        val articleId = when (module) {
            "articles" -> idOf(item)
            "comments" -> if ((item["type"]?.asInt ?: 0) == 1) item["target_id"]?.asLong ?: 0L else 0L
            else -> 0L
        }
        if (articleId > 0L) {
            startActivity(
                Intent(this, ArticleDetailActivity::class.java)
                    .putExtra(ArticleDetailActivity.EXTRA_ARTICLE_ID, articleId),
            )
        }
    }

    private fun idOf(item: JsonObject): Long {
        return runCatching { item["id"]?.asLong }.getOrNull()
            ?: runCatching { item["cid"]?.asLong }.getOrNull()
            ?: 0L
    }

    inner class SearchAdapter(
        private val onClick: (JsonObject) -> Unit,
    ) : RecyclerView.Adapter<SearchViewHolder>() {
        private val items = mutableListOf<JsonObject>()
        private var module = "articles"

        fun submitList(list: List<JsonObject>, moduleName: String) {
            module = moduleName
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        fun addList(list: List<JsonObject>) {
            val start = items.size
            items.addAll(list)
            notifyItemRangeInserted(start, list.size)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
            val card = MaterialCardView(parent.context).apply {
                applyInnerCard(13)
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    setMargins(dp(11), dp(5), dp(11), dp(10))
                }
            }
            val row = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(11), dp(11), dp(11), dp(11))
            }
            card.addView(row)
            val image = ImageView(parent.context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundResource(R.drawable.bg_image_placeholder)
                layoutParams = LinearLayout.LayoutParams(dp(92), dp(78))
            }
            row.addView(image)
            val column = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                margin(left = 12)
            }
            row.addView(column)
            val title = TextView(parent.context).apply {
                applyTitleStyle(15.5f)
                maxLines = 2
            }
            column.addView(title)
            val summary = TextView(parent.context).apply {
                applyBodyStyle(13f)
                maxLines = 2
                margin(top = 6)
            }
            column.addView(summary)
            val meta = TextView(parent.context).apply {
                applyBodyStyle(11.5f)
                setTextColor(KkColors.softMuted)
                margin(top = 8)
            }
            column.addView(meta)
            return SearchViewHolder(card, image, title, summary, meta)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
            val item = items[position]
            holder.title.text = titleOf(item)
            holder.summary.text = summaryOf(item)
            holder.meta.text = metaOf(item)
            val imageUrl = repository.absoluteUrl(imageOf(item))
            holder.image.isVisible = imageUrl.isNotBlank()
            if (imageUrl.isNotBlank()) {
                Glide.with(holder.image).load(imageUrl).placeholder(R.drawable.bg_image_placeholder).centerCrop().into(holder.image)
            } else {
                Glide.with(holder.image).clear(holder.image)
            }
            holder.itemView.setOnClickListener { onClick(item) }
        }

        private fun titleOf(item: JsonObject): String {
            return when (module) {
                "comments" -> display(item, "nickname").ifBlank { "评论" }
                else -> display(item, "title").ifBlank { display(item, "name") }.ifBlank { "未命名" }
            }
        }

        private fun summaryOf(item: JsonObject): String {
            return listOf("summary", "content", "description", "text")
                .firstNotNullOfOrNull { key -> display(item, key).takeIf { it.isNotBlank() } }
                ?.replace(Regex("<[^>]+>"), " ")
                ?.trim()
                ?: ""
        }

        private fun metaOf(item: JsonObject): String {
            val type = when (module) {
                "articles" -> "文章"
                "dynamics" -> "动态"
                else -> "评论"
            }
            val time = display(item, "created_at").ifBlank { display(item, "create_time") }
            return listOf(type, time).filter { it.isNotBlank() }.joinToString(" · ")
        }

        private fun imageOf(item: JsonObject): String {
            listOf("cover", "home_img", "avatar", "thumbnail_url", "url").forEach { key ->
                val value = display(item, key)
                if (value.startsWith("/") || value.startsWith("http")) return value
            }
            val user = item["user"]?.takeIf { it.isJsonObject }?.asJsonObject
            val avatar = display(user ?: JsonObject(), "avatar")
            if (avatar.isNotBlank()) return avatar
            val images = item["images"]
            if (images != null && images.isJsonArray && images.asJsonArray.size() > 0) {
                val first = images.asJsonArray.first()
                if (first.isJsonPrimitive) return first.asString
            }
            return ""
        }
    }

    private fun display(item: JsonObject, key: String): String = repository.displayValue(item[key])

    class SearchViewHolder(
        itemView: View,
        val image: ImageView,
        val title: TextView,
        val summary: TextView,
        val meta: TextView,
    ) : RecyclerView.ViewHolder(itemView)

    private data class SearchTab(val key: String, val label: String)

    companion object {
        private const val LIMIT = 10
    }
}
