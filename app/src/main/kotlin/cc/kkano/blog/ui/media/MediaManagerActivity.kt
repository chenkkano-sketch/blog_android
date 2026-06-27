package cc.kkano.blog.ui.media

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.common.setRoundedBackground
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class MediaManagerActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val mediaItems = mutableListOf<JsonObject>()
    private val categories = mutableListOf<JsonObject>()
    private val selectedIds = linkedSetOf<Long>()
    private var page = 1
    private val limit = 20
    private var currentCategory = ""
    private var batchMode = false
    private var loading = false

    private lateinit var topAction: TextView
    private lateinit var totalCount: TextView
    private lateinit var totalSize: TextView
    private lateinit var todayCount: TextView
    private lateinit var searchInput: EditText
    private lateinit var categoryRow: LinearLayout
    private lateinit var waterfallRow: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var loadMoreText: TextView
    private lateinit var batchDeleteButton: TextView

    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (!uris.isNullOrEmpty()) uploadImages(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        refreshAll()
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F5F7"))
        }
        root.addView(mediaTopBar())

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F5F5F7"))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(scroll)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(20))
        }
        scroll.addView(content)
        content.addView(statisticsBar())
        content.addView(searchBar())
        content.addView(categoryTabs())
        content.addView(actionBar())

        emptyText = TextView(this).apply {
            text = "暂无图片数据"
            applyBodyStyle(15f)
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(96))
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
            setPadding(0, dp(18), 0, dp(10))
            setOnClickListener { loadMore() }
        }
        content.addView(loadMoreText)
        return root
    }

    private fun mediaTopBar(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.WHITE)
            elevation = dp(5).toFloat()
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56))
            addView(ImageView(this@MediaManagerActivity).apply {
                setImageResource(R.drawable.ic_back)
                setColorFilter(KkColors.black)
                background = roundedDrawable(Color.WHITE, 999)
                setPadding(dp(9), dp(9), dp(9), dp(9))
                layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
                setOnClickListener { finish() }
            })
            addView(TextView(this@MediaManagerActivity).apply {
                text = "图床管理"
                applyTitleStyle(18f)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            })
            topAction = TextView(this@MediaManagerActivity).apply {
                text = "批量"
                applyTitleStyle(14f)
                setTextColor(KkColors.black)
                gravity = Gravity.CENTER
                setRoundedBackground(Color.parseColor("#F2F3F5"), 999)
                layoutParams = LinearLayout.LayoutParams(dp(58), dp(36))
                setOnClickListener { toggleBatchMode() }
            }
            addView(topAction)
        }
    }

    private fun statisticsBar(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(10), dp(14), dp(10), dp(14))
            setRoundedBackground(Color.WHITE, 14)
            elevation = dp(3).toFloat()
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
        }
        totalCount = statItem(box, "总数")
        totalSize = statItem(box, "总大小")
        todayCount = statItem(box, "今日上传")
        return box
    }

    private fun statItem(parent: LinearLayout, label: String): TextView {
        val value = TextView(this).apply {
            text = "0"
            applyTitleStyle(18f)
            gravity = Gravity.CENTER
        }
        parent.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(value)
            addView(TextView(this@MediaManagerActivity).apply {
                text = label
                applyBodyStyle(11.5f)
                gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, 0)
            })
        })
        return value
    }

    private fun searchBar(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(8), dp(8))
            setRoundedBackground(Color.WHITE, 14)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)).apply {
                bottomMargin = dp(10)
            }
            addView(ImageView(this@MediaManagerActivity).apply {
                setImageResource(R.drawable.ic_search)
                setColorFilter(KkColors.softMuted)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            })
            searchInput = EditText(this@MediaManagerActivity).apply {
                hint = "搜索文件名"
                setSingleLine(true)
                background = null
                textSize = 14.5f
                setTextColor(KkColors.text)
                setHintTextColor(Color.parseColor("#9AA1AD"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                    leftMargin = dp(8)
                }
            }
            addView(searchInput)
            addView(TextView(this@MediaManagerActivity).apply {
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
            setPadding(0, dp(2), 0, dp(2))
        }
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(Color.WHITE)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)).apply {
                bottomMargin = dp(10)
            }
            addView(categoryRow)
        }
    }

    private fun actionBar(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
            addView(actionButton("上传图片", R.drawable.ic_image) { pickImages.launch("image/*") })
            addView(actionButton("选择图片", R.drawable.ic_add) { pickImages.launch("image/*") })
            batchDeleteButton = actionButton("删除图片", R.drawable.ic_delete, danger = true) { batchDelete() }
            batchDeleteButton.visibility = View.GONE
            addView(batchDeleteButton)
        }
    }

    private fun actionButton(label: String, icon: Int, danger: Boolean = false, onClick: () -> Unit): TextView {
        val primary = label == "上传图片"
        return TextView(this).apply {
            text = label
            setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)
            compoundDrawables.forEach { drawable ->
                drawable?.mutate()?.setTint(if (danger || primary) Color.WHITE else KkColors.black)
            }
            compoundDrawablePadding = dp(6)
            applyTitleStyle(13f)
            gravity = Gravity.CENTER
            setTextColor(if (danger || primary) Color.WHITE else KkColors.black)
            setRoundedBackground(
                color = when {
                    danger -> KkColors.danger
                    primary -> KkColors.orange
                    else -> Color.WHITE
                },
                radius = 999,
                strokeColor = if (danger || primary) null else KkColors.line,
            )
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                setMargins(dp(3), 0, dp(3), 0)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun refreshAll() {
        page = 1
        selectedIds.clear()
        mediaItems.clear()
        loadStatistics()
        loadCategories()
        loadMedia(isPage = false)
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            runCatching { repository.mediaStatistics() }
                .onSuccess { stats ->
                    totalCount.text = display(stats, "total_count").ifBlank { "0" }
                    totalSize.text = display(stats, "formatted_total_size").ifBlank { "0 B" }
                    todayCount.text = display(stats, "today_count").ifBlank { "0" }
                }
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            runCatching { repository.mediaCategories() }
                .onSuccess {
                    categories.clear()
                    categories.addAll(it)
                    renderCategories()
                }
        }
    }

    private fun loadMedia(isPage: Boolean) {
        if (loading) return
        loading = true
        loadMoreText.text = "正在加载中..."
        val nextPage = if (isPage) page + 1 else 1
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
                Toast.makeText(this@MediaManagerActivity, it.message ?: "加载图床失败", Toast.LENGTH_SHORT).show()
                loadMoreText.text = "加载更多"
            }
            loading = false
        }
    }

    private fun loadMore() {
        if (loadMoreText.text == "没有更多了") return
        loadMedia(isPage = true)
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
            setRoundedBackground(if (active) KkColors.orange else Color.parseColor("#F0F1F3"), 999)
            setPadding(dp(14), 0, dp(14), 0)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34)).apply {
                rightMargin = dp(8)
            }
            setOnClickListener { onClick() }
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
        updateBatchUi()
    }

    private fun mediaTile(item: JsonObject, itemWidth: Int): View {
        val imageHeight = imageHeight(item, itemWidth)
        val id = idOf(item)
        return MaterialCardView(this).apply {
            radius = dp(13).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imageHeight + dp(70)).apply {
                setMargins(0, 0, 0, dp(8))
            }
            val column = LinearLayout(this@MediaManagerActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            addView(column)
            val imageFrame = FrameLayout(this@MediaManagerActivity).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imageHeight)
            }
            column.addView(imageFrame)
            imageFrame.addView(ImageView(this@MediaManagerActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundResource(R.drawable.bg_image_placeholder)
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                Glide.with(this)
                    .load(repository.absoluteUrl(display(item, "thumbnail_url").ifBlank { display(item, "url") }))
                    .placeholder(R.drawable.bg_image_placeholder)
                    .centerCrop()
                    .into(this)
            })
            val check = TextView(this@MediaManagerActivity).apply {
                text = if (selectedIds.contains(id)) "✓" else ""
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setRoundedBackground(if (selectedIds.contains(id)) KkColors.orange else Color.parseColor("#DDE1E7"), 999)
                visibility = if (batchMode) View.VISIBLE else View.GONE
                layoutParams = FrameLayout.LayoutParams(dp(28), dp(28), Gravity.TOP or Gravity.END).apply {
                    setMargins(0, dp(7), dp(7), 0)
                }
            }
            imageFrame.addView(check)
            column.addView(LinearLayout(this@MediaManagerActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(8), dp(7), dp(8), dp(8))
                addView(TextView(this@MediaManagerActivity).apply {
                    text = display(item, "original_name").ifBlank { display(item, "filename") }.ifBlank { "未命名图片" }
                    applyTitleStyle(12f)
                    maxLines = 1
                })
                addView(TextView(this@MediaManagerActivity).apply {
                    text = listOf(
                        formatSize(displayLong(item, "file_size")),
                        display(item, "category"),
                    ).filter { it.isNotBlank() && it != "0 B" }.joinToString(" · ").ifBlank { formatSize(displayLong(item, "file_size")) }
                    applyBodyStyle(10.5f)
                    setPadding(0, dp(5), 0, 0)
                    maxLines = 1
                })
            })
            setOnClickListener {
                if (batchMode) {
                    toggleSelected(id)
                } else {
                    showDetail(item)
                }
            }
        }
    }

    private fun imageHeight(item: JsonObject, itemWidth: Int): Int {
        val width = displayLong(item, "width")
        val height = displayLong(item, "height")
        if (width > 0 && height > 0) {
            return ((itemWidth.toLong() * height) / width).toInt().coerceIn(dp(96), dp(190))
        }
        return (dp(112) + (idOf(item) % 3L).toInt() * dp(22)).coerceAtMost(dp(180))
    }

    private fun toggleBatchMode() {
        batchMode = !batchMode
        if (!batchMode) selectedIds.clear()
        topAction.text = if (batchMode) "取消" else "批量"
        renderWaterfall()
        updateBatchUi()
    }

    private fun toggleSelected(id: Long) {
        if (id <= 0L) return
        if (!selectedIds.add(id)) selectedIds.remove(id)
        renderWaterfall()
        updateBatchUi()
    }

    private fun updateBatchUi() {
        batchDeleteButton.visibility = if (batchMode && selectedIds.isNotEmpty()) View.VISIBLE else View.GONE
        batchDeleteButton.text = if (selectedIds.isEmpty()) "删除图片" else "删除(${selectedIds.size})"
    }

    private fun showDetail(item: JsonObject) {
        val dialog = BottomSheetDialog(this)
        val titleInput = EditText(this).apply {
            hint = "请输入标题"
            setText(display(item, "title"))
        }
        val altInput = EditText(this).apply {
            hint = "请输入描述"
            setText(display(item, "alt"))
        }
        val categoryInput = EditText(this).apply {
            hint = "请输入分类"
            setText(display(item, "category"))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(18))
            background = roundedDrawable(Color.WHITE, 24)
        }
        root.addView(TextView(this).apply {
            text = "图片详情"
            applyTitleStyle(18f)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(12))
        })
        root.addView(ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.bg_image_placeholder)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220))
            Glide.with(this).load(repository.absoluteUrl(display(item, "url"))).centerCrop().into(this)
        })
        root.addView(detailRow("文件名", display(item, "original_name").ifBlank { display(item, "filename") }))
        root.addView(detailRow("尺寸", "${display(item, "width")} x ${display(item, "height")}"))
        root.addView(detailRow("大小", formatSize(displayLong(item, "file_size"))))
        root.addView(detailRow("上传时间", display(item, "created_at")))
        root.addView(editRow("标题", titleInput))
        root.addView(editRow("描述", altInput))
        root.addView(editRow("分类", categoryInput))
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(14), 0, 0)
            addView(sheetButton("复制链接") {
                copyUrl(display(item, "url"))
            })
            addView(sheetButton("保存") {
                saveMedia(item, titleInput.text?.toString().orEmpty(), altInput.text?.toString().orEmpty(), categoryInput.text?.toString().orEmpty(), dialog)
            })
            addView(sheetButton("删除", danger = true) {
                confirmDelete(listOf(idOf(item)), dialog)
            })
        })
        dialog.setContentView(root)
        dialog.show()
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.window?.setDimAmount(0f)
    }

    private fun detailRow(label: String, value: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(9), 0, 0)
            addView(TextView(this@MediaManagerActivity).apply {
                text = "$label:"
                applyBodyStyle(12f)
                layoutParams = LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(TextView(this@MediaManagerActivity).apply {
                text = value.ifBlank { "-" }
                applyBodyStyle(12f)
                setTextColor(KkColors.text)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
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
            addView(TextView(this@MediaManagerActivity).apply {
                text = label
                applyTitleStyle(13f)
                layoutParams = LinearLayout.LayoutParams(dp(52), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(input.apply {
                background = null
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            })
        }
    }

    private fun sheetButton(label: String, danger: Boolean = false, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            applyTitleStyle(13f)
            gravity = Gravity.CENTER
            setTextColor(if (danger) Color.WHITE else KkColors.black)
            setRoundedBackground(if (danger) KkColors.danger else KkColors.orange, 999)
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                setMargins(dp(3), 0, dp(3), 0)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun saveMedia(item: JsonObject, title: String, alt: String, category: String, dialog: BottomSheetDialog) {
        val id = idOf(item)
        lifecycleScope.launch {
            runCatching {
                repository.updateMedia(
                    id,
                    mapOf("title" to title, "alt" to alt, "category" to category),
                )
            }.onSuccess {
                Toast.makeText(this@MediaManagerActivity, "保存成功", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                refreshAll()
            }.onFailure {
                Toast.makeText(this@MediaManagerActivity, it.message ?: "保存失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun batchDelete() {
        if (selectedIds.isEmpty()) return
        confirmDelete(selectedIds.toList(), null)
    }

    private fun confirmDelete(ids: List<Long>, dialog: BottomSheetDialog?) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除选中的 ${ids.size} 张图片吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    val result = if (ids.size == 1) {
                        runCatching { repository.deleteMedia(ids.first()) }
                    } else {
                        runCatching { repository.batchDeleteMedia(ids) }
                    }
                    result.onSuccess {
                        Toast.makeText(this@MediaManagerActivity, "删除成功", Toast.LENGTH_SHORT).show()
                        dialog?.dismiss()
                        batchMode = false
                        topAction.text = "批量"
                        refreshAll()
                    }.onFailure {
                        Toast.makeText(this@MediaManagerActivity, it.message ?: "删除失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun uploadImages(uris: List<Uri>) {
        Toast.makeText(this, "上传中...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            uris.forEach { uri ->
                runCatching {
                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("无法读取图片")
                    repository.uploadMedia(queryFileName(uri), bytes, contentResolver.getType(uri) ?: "image/jpeg")
                }.onFailure {
                    Toast.makeText(this@MediaManagerActivity, it.message ?: "上传失败", Toast.LENGTH_SHORT).show()
                }
            }
            Toast.makeText(this@MediaManagerActivity, "上传完成", Toast.LENGTH_SHORT).show()
            refreshAll()
        }
    }

    private fun queryFileName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index)
        }
        return "image_${System.currentTimeMillis()}.jpg"
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
}
