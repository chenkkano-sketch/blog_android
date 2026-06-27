package cc.kkano.blog.ui.media

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
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

class MediaGalleryActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val mediaItems = mutableListOf<JsonObject>()
    private val categories = mutableListOf<JsonObject>()
    private var page = 1
    private val limit = 30
    private var currentCategory = ""
    private var loading = false

    private lateinit var searchInput: EditText
    private lateinit var categoryRow: LinearLayout
    private lateinit var waterfallRow: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var loadMoreText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        refreshAll()
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "图库" },
                leftIcon = R.drawable.ic_back,
                onLeftClick = { finish() },
            ),
        )
        root.addView(searchBar())
        root.addView(categoryTabs())
        val scroll = ScrollView(this).apply {
            setBackgroundColor(KkColors.background)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(scroll)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(22))
        }
        scroll.addView(content)
        emptyText = TextView(this).apply {
            text = "暂无媒体图片"
            applyBodyStyle(15f)
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(110))
        }
        content.addView(emptyText)
        waterfallRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        content.addView(waterfallRow)
        loadMoreText = TextView(this).apply {
            text = "加载更多"
            applyBodyStyle(13f)
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(8))
            setOnClickListener { loadMore() }
        }
        content.addView(loadMoreText)
        return root
    }

    private fun searchBar(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(8), dp(8))
            setBackgroundColor(Color.WHITE)
            addView(ImageView(this@MediaGalleryActivity).apply {
                setImageResource(R.drawable.ic_search)
                setColorFilter(KkColors.softMuted)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            })
            searchInput = EditText(this@MediaGalleryActivity).apply {
                hint = "搜索图片"
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
            addView(TextView(this@MediaGalleryActivity).apply {
                text = "搜索"
                applyTitleStyle(13f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.orange, 999)
                layoutParams = LinearLayout.LayoutParams(dp(58), dp(36))
                setOnClickListener {
                    page = 1
                    mediaItems.clear()
                    loadMedia(isPage = false)
                }
            })
        }
    }

    private fun categoryTabs(): View {
        categoryRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(Color.WHITE)
            addView(categoryRow)
        }
    }

    private fun refreshAll() {
        page = 1
        mediaItems.clear()
        lifecycleScope.launch {
            runCatching { repository.mediaCategories() }
                .onSuccess {
                    categories.clear()
                    categories.addAll(it)
                    renderCategories()
                }
        }
        loadMedia(isPage = false)
    }

    private fun renderCategories() {
        categoryRow.removeAllViews()
        categoryRow.addView(categoryChip("全部", currentCategory.isBlank()) {
            currentCategory = ""
            page = 1
            mediaItems.clear()
            loadMedia(isPage = false)
            renderCategories()
        })
        categories.forEach { category ->
            val name = display(category, "name")
            val count = display(category, "count")
            categoryRow.addView(categoryChip(if (count.isBlank()) name else "$name($count)", currentCategory == name) {
                currentCategory = name
                page = 1
                mediaItems.clear()
                loadMedia(isPage = false)
                renderCategories()
            })
        }
    }

    private fun categoryChip(label: String, active: Boolean, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            applyTitleStyle(12.5f)
            gravity = Gravity.CENTER
            setTextColor(if (active) Color.WHITE else KkColors.muted)
            setRoundedBackground(if (active) KkColors.black else Color.parseColor("#F2F3F5"), 999)
            setPadding(dp(14), 0, dp(14), 0)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34)).apply {
                rightMargin = dp(8)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun loadMedia(isPage: Boolean) {
        if (loading) return
        loading = true
        val nextPage = if (isPage) page + 1 else 1
        loadMoreText.text = if (isPage) "正在加载中..." else "加载中..."
        lifecycleScope.launch {
            runCatching {
                repository.mediaRoot(
                    page = nextPage,
                    limit = limit,
                    keyword = searchInput.text?.toString().orEmpty().trim(),
                    category = currentCategory,
                )
            }.onSuccess { root ->
                val data = root["data"]?.takeIf { it.isJsonObject }?.asJsonObject ?: root
                val list = data["list"]?.takeIf { it.isJsonArray }?.asJsonArray
                    ?.mapNotNull { it.takeIf { element -> element.isJsonObject }?.asJsonObject }
                    ?: emptyList()
                if (!isPage) mediaItems.clear()
                mediaItems.addAll(list)
                page = nextPage
                renderWaterfall()
                emptyText.visibility = if (mediaItems.isEmpty()) View.VISIBLE else View.GONE
                loadMoreText.text = if (list.size < limit) "没有更多了" else "加载更多"
            }.onFailure {
                Toast.makeText(this@MediaGalleryActivity, it.message ?: "图库加载失败", Toast.LENGTH_SHORT).show()
                loadMoreText.text = "加载更多"
            }
            loading = false
        }
    }

    private fun renderWaterfall() {
        waterfallRow.removeAllViews()
        val columns = List(3) {
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                }
            }
        }
        columns.forEach { waterfallRow.addView(it) }
        val heights = IntArray(3)
        val itemWidth = ((resources.displayMetrics.widthPixels - dp(38)) / 3).coerceAtLeast(dp(90))
        mediaItems.forEach { item ->
            val columnIndex = heights.indices.minBy { heights[it] }
            val tile = mediaTile(item, itemWidth)
            columns[columnIndex].addView(tile)
            heights[columnIndex] += tile.layoutParams.height.takeIf { it > 0 } ?: itemWidth
        }
    }

    private fun mediaTile(item: JsonObject, itemWidth: Int): View {
        val imageHeight = imageHeight(item, itemWidth)
        return MaterialCardView(this).apply {
            radius = dp(13).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imageHeight + dp(58)).apply {
                bottomMargin = dp(8)
            }
            addView(LinearLayout(this@MediaGalleryActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(ImageView(this@MediaGalleryActivity).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setBackgroundResource(R.drawable.bg_image_placeholder)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imageHeight)
                    Glide.with(this)
                        .load(repository.absoluteUrl(display(item, "thumbnail_url").ifBlank { display(item, "url") }))
                        .placeholder(R.drawable.bg_image_placeholder)
                        .centerCrop()
                        .into(this)
                })
                addView(TextView(this@MediaGalleryActivity).apply {
                    text = display(item, "title")
                        .ifBlank { display(item, "original_name") }
                        .ifBlank { display(item, "filename") }
                        .ifBlank { "未命名图片" }
                    applyTitleStyle(11.5f)
                    setPadding(dp(8), dp(7), dp(8), 0)
                    maxLines = 1
                })
                addView(TextView(this@MediaGalleryActivity).apply {
                    text = display(item, "category").ifBlank { formatSize(displayLong(item, "file_size")) }
                    applyBodyStyle(10.5f)
                    setPadding(dp(8), dp(5), dp(8), dp(8))
                    maxLines = 1
                })
            })
            setOnClickListener { showDetail(item) }
        }
    }

    private fun showDetail(item: JsonObject) {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(Color.WHITE, 24)
            setPadding(dp(16), dp(12), dp(16), dp(18))
            addView(TextView(this@MediaGalleryActivity).apply {
                text = "图片详情"
                applyTitleStyle(18f)
                gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, dp(12))
            })
            addView(ImageView(this@MediaGalleryActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundResource(R.drawable.bg_image_placeholder)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(230))
                Glide.with(this).load(repository.absoluteUrl(display(item, "url"))).centerCrop().into(this)
            })
            addView(detailRow("文件名", display(item, "original_name").ifBlank { display(item, "filename") }))
            addView(detailRow("分类", display(item, "category")))
            addView(detailRow("大小", formatSize(displayLong(item, "file_size"))))
            addView(detailRow("上传时间", display(item, "created_at")))
            addView(TextView(this@MediaGalleryActivity).apply {
                text = "复制链接"
                applyTitleStyle(15f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.orange, 999)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)).apply {
                    topMargin = dp(14)
                }
                setOnClickListener {
                    copyUrl(display(item, "url"))
                    dialog.dismiss()
                }
            })
        }
        dialog.setContentView(root)
        dialog.show()
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.window?.setDimAmount(0f)
    }

    private fun detailRow(label: String, value: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
            addView(TextView(this@MediaGalleryActivity).apply {
                text = "$label:"
                applyBodyStyle(12f)
                layoutParams = LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(TextView(this@MediaGalleryActivity).apply {
                text = value.ifBlank { "-" }
                applyBodyStyle(12f)
                setTextColor(KkColors.text)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
    }

    private fun loadMore() {
        if (loadMoreText.text == "没有更多了") return
        loadMedia(isPage = true)
    }

    private fun imageHeight(item: JsonObject, itemWidth: Int): Int {
        val width = displayLong(item, "width")
        val height = displayLong(item, "height")
        if (width > 0 && height > 0) {
            return ((itemWidth.toLong() * height) / width).toInt().coerceIn(dp(96), dp(190))
        }
        return (dp(112) + (idOf(item) % 3L).toInt() * dp(22)).coerceAtMost(dp(180))
    }

    private fun copyUrl(url: String) {
        val fullUrl = repository.absoluteUrl(url)
        val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("media_url", fullUrl))
        Toast.makeText(this, "已复制链接", Toast.LENGTH_SHORT).show()
    }

    private fun idOf(item: JsonObject): Long {
        return runCatching { item["id"]?.asLong }.getOrNull() ?: 0L
    }

    private fun display(item: JsonObject, key: String): String {
        return repository.displayValue(item[key])
    }

    private fun displayLong(item: JsonObject, key: String): Long {
        return runCatching { item[key]?.asLong }.getOrNull() ?: 0L
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = listOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var index = 0
        while (size >= 1024 && index < units.lastIndex) {
            size /= 1024.0
            index++
        }
        return if (index == 0) "${size.toInt()} ${units[index]}" else String.format("%.1f %s", size, units[index])
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
    }
}
