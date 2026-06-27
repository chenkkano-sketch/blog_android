package cc.kkano.blog.ui.feature

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.navigation.FeatureMode
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.article.ArticleEditorActivity
import cc.kkano.blog.ui.article.ArticleDetailActivity
import cc.kkano.blog.ui.dynamics.DynamicEditorActivity
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

class GenericListActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var adapter: GenericAdapter
    private lateinit var emptyText: TextView
    private lateinit var root: LinearLayout
    private var endpoint: String = ""
    private var mode: FeatureMode = FeatureMode.LIST
    private var route: String = ""
    private var titleText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        endpoint = intent.getStringExtra(EXTRA_ENDPOINT).orEmpty()
        route = intent.getStringExtra(EXTRA_ROUTE).orEmpty()
        titleText = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "列表" }
        mode = runCatching {
            FeatureMode.valueOf(intent.getStringExtra(EXTRA_MODE).orEmpty())
        }.getOrDefault(FeatureMode.LIST)
        setContentView(buildContent(titleText))
        load()
    }

    private fun buildContent(titleText: String): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = titleText,
                leftIcon = R.drawable.ic_back,
                rightIcon = if (isManageable()) R.drawable.ic_add else null,
                onLeftClick = { finish() },
                onRightClick = { openCreate() },
            ),
        )

        refreshLayout = SwipeRefreshLayout(this).apply {
            setColorSchemeResources(R.color.kk_orange, R.color.kk_black)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            setOnRefreshListener { load() }
        }
        root.addView(refreshLayout)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(12))
        }
        refreshLayout.addView(container)

        emptyText = TextView(this).apply {
            text = "暂无数据"
            applyBodyStyle(15f)
            gravity = android.view.Gravity.CENTER
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(96))
        }
        container.addView(emptyText)

        val recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@GenericListActivity)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        container.addView(recycler)
        adapter = GenericAdapter(mode)
        recycler.adapter = adapter
        return root
    }

    private fun load() {
        if (endpoint.isBlank()) {
            emptyText.isVisible = true
            return
        }
        refreshLayout.isRefreshing = true
        lifecycleScope.launch {
            runCatching { repository.genericList(endpoint) }
                .onSuccess {
                    refreshLayout.isRefreshing = false
                    adapter.submitList(it)
                    emptyText.isVisible = it.isEmpty()
                }
                .onFailure {
                    refreshLayout.isRefreshing = false
                    Snackbar.make(root, it.message ?: "加载失败", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun isManageable(): Boolean {
        return route.startsWith("pages/manage/") ||
            route.contains("-manage") ||
            endpoint.contains("/admin/") ||
            endpoint.endsWith("-manage")
    }

    private fun openCreate() {
        if (!isManageable()) return
        startActivity(
            Intent(this, GenericEditActivity::class.java)
                .putExtra(GenericEditActivity.EXTRA_TITLE, titleText)
                .putExtra(GenericEditActivity.EXTRA_ENDPOINT, endpoint),
        )
    }

    inner class GenericAdapter(private val mode: FeatureMode) : RecyclerView.Adapter<GenericViewHolder>() {
        private val items = mutableListOf<JsonObject>()

        fun submitList(list: List<JsonObject>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericViewHolder {
            val card = MaterialCardView(parent.context).apply {
                applyInnerCard(13)
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    setMargins(dp(11), dp(5), dp(11), dp(9))
                }
            }
            val row = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(10), dp(10), dp(10), dp(10))
            }
            card.addView(row)
            val image = ImageView(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(82), dp(72))
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundResource(R.drawable.bg_image_placeholder)
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
            val subtitle = TextView(parent.context).apply {
                applyBodyStyle(13f)
                maxLines = 2
                margin(top = 6)
            }
            column.addView(subtitle)
            val meta = TextView(parent.context).apply {
                applyBodyStyle(11.5f)
                setTextColor(KkColors.softMuted)
                maxLines = 1
                margin(top = 8)
            }
            column.addView(meta)
            val actions = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                margin(top = 10)
                visibility = View.GONE
            }
            column.addView(actions)
            val edit = smallAction("编辑")
            val delete = smallAction("删除", danger = true)
            actions.addView(edit)
            actions.addView(delete)
            return GenericViewHolder(card, image, title, subtitle, meta, actions, edit, delete)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: GenericViewHolder, position: Int) {
            val item = items[position]
            holder.title.text = titleOf(item)
            holder.subtitle.text = subtitleOf(item)
            holder.meta.text = metaOf(item)

            val imageUrl = repository.absoluteUrl(imageOf(item))
            holder.image.isVisible = imageUrl.isNotBlank()
            if (imageUrl.isNotBlank()) {
                Glide.with(holder.image)
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_image_placeholder)
                    .centerCrop()
                    .into(holder.image)
            } else {
                Glide.with(holder.image).clear(holder.image)
            }

            holder.itemView.setOnClickListener {
                if (mode == FeatureMode.ARTICLE_LIST) {
                    val id = item["id"]?.asLong ?: return@setOnClickListener
                    startActivity(
                        Intent(this@GenericListActivity, ArticleDetailActivity::class.java)
                            .putExtra(ArticleDetailActivity.EXTRA_ARTICLE_ID, id),
                    )
                }
            }
            holder.actions.isVisible = isManageable()
            holder.edit.setOnClickListener { openEdit(item) }
            holder.delete.setOnClickListener { confirmDelete(item) }
        }

        private fun smallAction(textValue: String, danger: Boolean = false): TextView {
            return TextView(this@GenericListActivity).apply {
                text = textValue
                applyTitleStyle(12f)
                gravity = android.view.Gravity.CENTER
                setTextColor(if (danger) android.graphics.Color.WHITE else KkColors.black)
                setRoundedBackground(if (danger) KkColors.danger else KkColors.orange, 999)
                layoutParams = LinearLayout.LayoutParams(0, dp(34), 1f).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                }
            }
        }

        private fun openEdit(item: JsonObject) {
            val id = idOf(item)
            when {
                endpoint.contains("articles") && id > 0L -> {
                    startActivity(
                        Intent(this@GenericListActivity, ArticleEditorActivity::class.java)
                            .putExtra(ArticleEditorActivity.EXTRA_ARTICLE_ID, id),
                    )
                }
                endpoint.contains("dynamics") && id > 0L -> {
                    startActivity(
                        Intent(this@GenericListActivity, DynamicEditorActivity::class.java)
                            .putExtra(DynamicEditorActivity.EXTRA_DYNAMIC_ID, id),
                    )
                }
                else -> {
                    startActivity(
                        Intent(this@GenericListActivity, GenericEditActivity::class.java)
                            .putExtra(GenericEditActivity.EXTRA_TITLE, titleText)
                            .putExtra(GenericEditActivity.EXTRA_ENDPOINT, endpoint)
                            .putExtra(GenericEditActivity.EXTRA_ITEM_JSON, AppGraph.apiClient.gson.toJson(item)),
                    )
                }
            }
        }

        private fun confirmDelete(item: JsonObject) {
            val id = idOf(item)
            if (id <= 0L) {
                Snackbar.make(root, "没有可删除的 ID", Snackbar.LENGTH_SHORT).show()
                return
            }
            AlertDialog.Builder(this@GenericListActivity)
                .setTitle("确认删除")
                .setMessage("确定要删除「${titleOf(item)}」吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除") { _, _ ->
                    lifecycleScope.launch {
                        runCatching { repository.delete("${endpoint.trimEnd('/')}/$id") }
                            .onSuccess {
                                Snackbar.make(root, "删除成功", Snackbar.LENGTH_SHORT).show()
                                load()
                            }
                            .onFailure {
                                Snackbar.make(root, it.message ?: "删除失败", Snackbar.LENGTH_SHORT).show()
                            }
                    }
                }
                .show()
        }

        private fun idOf(item: JsonObject): Long {
            return runCatching { item["id"]?.asLong }.getOrNull()
                ?: runCatching { item["uid"]?.asLong }.getOrNull()
                ?: runCatching { item["coid"]?.asLong }.getOrNull()
                ?: 0L
        }

        private fun titleOf(item: JsonObject): String {
            val keys = listOf("title", "name", "nickname", "username", "original_name", "city", "email", "filename")
            return keys.firstNotNullOfOrNull { key ->
                repository.displayValue(item[key]).takeIf { it.isNotBlank() }
            } ?: "未命名"
        }

        private fun subtitleOf(item: JsonObject): String {
            val keys = listOf("summary", "description", "content", "url", "email", "province", "type_name", "mime_type")
            return keys.firstNotNullOfOrNull { key ->
                repository.displayValue(item[key]).takeIf { it.isNotBlank() }
            } ?: ""
        }

        private fun metaOf(item: JsonObject): String {
            val values = listOf("created_at", "create_time", "updated_at", "visit_date", "status")
                .mapNotNull { key -> repository.displayValue(item[key]).takeIf { it.isNotBlank() } }
                .take(2)
            return values.joinToString(" · ")
        }

        private fun imageOf(item: JsonObject): String {
            listOf("home_img", "cover", "avatar", "thumbnail_url", "url").forEach { key ->
                val value = repository.displayValue(item[key])
                if (value.startsWith("/") || value.startsWith("http")) return value
            }
            val images = item["images"]
            if (images != null && images.isJsonArray && images.asJsonArray.size() > 0) {
                val first = images.asJsonArray.first()
                if (first.isJsonPrimitive) return first.asString
            }
            return ""
        }
    }

    class GenericViewHolder(
        itemView: View,
        val image: ImageView,
        val title: TextView,
        val subtitle: TextView,
        val meta: TextView,
        val actions: LinearLayout,
        val edit: TextView,
        val delete: TextView,
    ) : RecyclerView.ViewHolder(itemView)

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ENDPOINT = "extra_endpoint"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_ROUTE = "extra_route"
    }
}
