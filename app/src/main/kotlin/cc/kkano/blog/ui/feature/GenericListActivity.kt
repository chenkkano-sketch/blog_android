package cc.kkano.blog.ui.feature

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
import cc.kkano.blog.ui.article.ArticleDetailActivity
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.margin
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        endpoint = intent.getStringExtra(EXTRA_ENDPOINT).orEmpty()
        mode = runCatching {
            FeatureMode.valueOf(intent.getStringExtra(EXTRA_MODE).orEmpty())
        }.getOrDefault(FeatureMode.LIST)
        setContentView(buildContent(intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "列表" }))
        load()
    }

    private fun buildContent(titleText: String): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.kk_background))
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(14), dp(14), dp(8))
        }
        root.addView(toolbar)
        toolbar.addView(MaterialButton(this).apply {
            text = "返回"
            setTextColor(getColor(R.color.kk_text))
            setBackgroundColor(getColor(R.color.kk_surface))
            layoutParams = LinearLayout.LayoutParams(dp(84), dp(44))
            setOnClickListener { finish() }
        })
        toolbar.addView(TextView(this).apply {
            text = titleText
            applyTitleStyle(22f)
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                leftMargin = dp(10)
            }
        })

        refreshLayout = SwipeRefreshLayout(this).apply {
            setColorSchemeResources(R.color.kk_orange, R.color.kk_black)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            setOnRefreshListener { load() }
        }
        root.addView(refreshLayout)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
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

    inner class GenericAdapter(private val mode: FeatureMode) : RecyclerView.Adapter<GenericViewHolder>() {
        private val items = mutableListOf<JsonObject>()

        fun submitList(list: List<JsonObject>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericViewHolder {
            val card = MaterialCardView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    setMargins(dp(14), dp(7), dp(14), dp(7))
                }
                radius = dp(18).toFloat()
                cardElevation = dp(1).toFloat()
                strokeColor = getColor(R.color.kk_line)
                strokeWidth = dp(1)
                setCardBackgroundColor(getColor(R.color.kk_surface))
            }
            val row = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }
            card.addView(row)
            val image = ImageView(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(72), dp(72))
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
                applyTitleStyle(16f)
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
                applyBodyStyle(12f)
                maxLines = 1
                margin(top = 8)
            }
            column.addView(meta)
            return GenericViewHolder(card, image, title, subtitle, meta)
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
    ) : RecyclerView.ViewHolder(itemView)

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ENDPOINT = "extra_endpoint"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_ROUTE = "extra_route"
    }
}
