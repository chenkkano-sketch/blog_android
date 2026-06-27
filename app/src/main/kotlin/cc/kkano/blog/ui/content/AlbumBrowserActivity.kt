package cc.kkano.blog.ui.content

import android.graphics.Color
import android.os.Bundle
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
import androidx.core.view.isVisible
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

class AlbumBrowserActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val albums = mutableListOf<JsonObject>()
    private var page = 1
    private val limit = 20
    private var loading = false

    private lateinit var searchInput: EditText
    private lateinit var listColumn: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var loadMoreText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        loadAlbums(isPage = false)
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = "相册",
                leftIcon = R.drawable.ic_back,
                onLeftClick = { finish() },
            ),
        )
        root.addView(searchBar())
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
            text = "暂无相册"
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
            addView(ImageView(this@AlbumBrowserActivity).apply {
                setImageResource(R.drawable.ic_search)
                setColorFilter(KkColors.softMuted)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            })
            searchInput = EditText(this@AlbumBrowserActivity).apply {
                hint = "搜索相册"
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
            addView(TextView(this@AlbumBrowserActivity).apply {
                text = "搜索"
                applyTitleStyle(13f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.orange, 999)
                layoutParams = LinearLayout.LayoutParams(dp(58), dp(36))
                setOnClickListener {
                    page = 1
                    loadAlbums(isPage = false)
                }
            })
        }
    }

    private fun loadAlbums(isPage: Boolean) {
        if (loading) return
        loading = true
        val nextPage = if (isPage) page + 1 else 1
        if (isPage) loadMoreText.text = "正在加载中..."
        lifecycleScope.launch {
            runCatching {
                repository.genericList(
                    endpoint = "api/albums",
                    page = nextPage,
                    limit = limit,
                    keyword = searchInput.text?.toString().orEmpty().trim(),
                )
            }.onSuccess { list ->
                if (!isPage) albums.clear()
                albums.addAll(list)
                page = nextPage
                renderList()
                loadMoreText.text = if (list.size < limit) "没有更多了" else "加载更多"
            }.onFailure {
                Toast.makeText(this@AlbumBrowserActivity, it.message ?: "相册加载失败", Toast.LENGTH_SHORT).show()
                loadMoreText.text = "加载更多"
                renderList()
            }
            loading = false
        }
    }

    private fun renderList() {
        listColumn.removeAllViews()
        if (albums.isEmpty()) {
            listColumn.addView(emptyText)
            return
        }
        albums.forEach { listColumn.addView(albumCard(it)) }
        listColumn.addView(loadMoreText)
    }

    private fun albumCard(item: JsonObject): View {
        val imageUrl = repository.absoluteUrl(coverOf(item))
        return MaterialCardView(this).apply {
            radius = dp(15).toFloat()
            cardElevation = dp(4).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(11)
            }
            setOnClickListener { showDetailSheet(item) }
            addView(LinearLayout(this@AlbumBrowserActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(FrameLayout(this@AlbumBrowserActivity).apply {
                    background = roundedDrawable(Color.parseColor("#F1F3F6"), 15)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(168))
                    addView(ImageView(this@AlbumBrowserActivity).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        isVisible = imageUrl.isNotBlank()
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        if (imageUrl.isNotBlank()) {
                            Glide.with(this).load(imageUrl).placeholder(R.drawable.bg_image_placeholder).centerCrop().into(this)
                        }
                    })
                    if (imageUrl.isBlank()) {
                        addView(ImageView(this@AlbumBrowserActivity).apply {
                            setImageResource(R.drawable.ic_image)
                            setColorFilter(KkColors.orange)
                            setPadding(dp(52), dp(52), dp(52), dp(52))
                            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        })
                    }
                    addView(TextView(this@AlbumBrowserActivity).apply {
                        text = countOf(item).ifBlank { "相册" }
                        applyTitleStyle(11.5f)
                        setTextColor(Color.WHITE)
                        gravity = Gravity.CENTER
                        setRoundedBackground(Color.parseColor("#AA15171C"), 999)
                        setPadding(dp(10), dp(5), dp(10), dp(5))
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END or Gravity.BOTTOM).apply {
                            setMargins(0, 0, dp(10), dp(10))
                        }
                    })
                })
                addView(LinearLayout(this@AlbumBrowserActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                    addView(TextView(this@AlbumBrowserActivity).apply {
                        text = titleOf(item)
                        applyTitleStyle(16f)
                        maxLines = 1
                    })
                    addView(TextView(this@AlbumBrowserActivity).apply {
                        text = descOf(item).ifBlank { timeOf(item).ifBlank { "博客相册" } }
                        applyBodyStyle(12.5f)
                        setTextColor(KkColors.text)
                        setPadding(0, dp(7), 0, 0)
                        maxLines = 2
                    })
                })
            })
        }
    }

    private fun showDetailSheet(item: JsonObject) {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(Color.WHITE, 24)
            setPadding(dp(16), dp(14), dp(16), dp(18))
            addView(TextView(this@AlbumBrowserActivity).apply {
                text = titleOf(item)
                applyTitleStyle(18f)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(12))
            })
            addView(detailRow("数量", countOf(item).ifBlank { "-" }))
            addView(detailRow("时间", timeOf(item).ifBlank { "-" }))
            addView(detailRow("分类", display(item, "category").ifBlank { display(item, "type").ifBlank { "-" } }))
            descOf(item).takeIf { it.isNotBlank() }?.let {
                addView(TextView(this@AlbumBrowserActivity).apply {
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
            addView(TextView(this@AlbumBrowserActivity).apply {
                text = label
                applyTitleStyle(13f)
                layoutParams = LinearLayout.LayoutParams(dp(58), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(TextView(this@AlbumBrowserActivity).apply {
                text = value
                applyBodyStyle(13f)
                setTextColor(KkColors.text)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
    }

    private fun loadMore() {
        if (loadMoreText.text == "没有更多了") return
        loadAlbums(isPage = true)
    }

    private fun titleOf(item: JsonObject): String {
        return display(item, "title").ifBlank { display(item, "name").ifBlank { "未命名相册" } }
    }

    private fun descOf(item: JsonObject): String {
        return display(item, "description").ifBlank { display(item, "summary").ifBlank { display(item, "content") } }
    }

    private fun coverOf(item: JsonObject): String {
        return display(item, "cover")
            .ifBlank { display(item, "thumbnail") }
            .ifBlank { display(item, "url") }
            .ifBlank { firstImage(item) }
    }

    private fun firstImage(item: JsonObject): String {
        val images = item["images"] ?: return ""
        if (!images.isJsonArray || images.asJsonArray.size() == 0) return ""
        val first = images.asJsonArray.first()
        return when {
            first.isJsonPrimitive -> first.asString
            first.isJsonObject -> display(first.asJsonObject, "url").ifBlank { display(first.asJsonObject, "path") }
            else -> ""
        }
    }

    private fun countOf(item: JsonObject): String {
        val count = display(item, "count").ifBlank { display(item, "image_count").ifBlank { display(item, "total") } }
        return if (count.isBlank()) "" else "$count 张"
    }

    private fun timeOf(item: JsonObject): String {
        return display(item, "created_at").ifBlank { display(item, "updated_at").ifBlank { display(item, "create_time") } }
    }

    private fun display(item: JsonObject, key: String): String {
        return repository.displayValue(item[key])
    }
}
