package cc.kkano.blog.ui.content

import android.content.Intent
import android.graphics.Color
import android.net.Uri
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
import cc.kkano.blog.data.api.ApiRoutes
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

class ProjectShowcaseActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val projects = mutableListOf<JsonObject>()
    private var loading = false

    private lateinit var searchInput: EditText
    private lateinit var listColumn: LinearLayout
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        loadProjects()
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = "项目",
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
            text = "暂无项目"
            applyBodyStyle(15f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(120))
        }
        return root
    }

    private fun searchBar(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(8), dp(8))
            setBackgroundColor(Color.WHITE)
            addView(ImageView(this@ProjectShowcaseActivity).apply {
                setImageResource(R.drawable.ic_search)
                setColorFilter(KkColors.softMuted)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            })
            searchInput = EditText(this@ProjectShowcaseActivity).apply {
                hint = "搜索项目"
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
            addView(TextView(this@ProjectShowcaseActivity).apply {
                text = "搜索"
                applyTitleStyle(13f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.orange, 999)
                layoutParams = LinearLayout.LayoutParams(dp(58), dp(36))
                setOnClickListener { loadProjects() }
            })
        }
    }

    private fun loadProjects() {
        if (loading) return
        loading = true
        lifecycleScope.launch {
            runCatching {
                repository.genericList(
                    endpoint = ApiRoutes.PROJECTS,
                    limit = 100,
                    keyword = searchInput.text?.toString().orEmpty().trim(),
                )
            }.onSuccess { list ->
                projects.clear()
                projects.addAll(list)
                renderList()
            }.onFailure {
                Toast.makeText(this@ProjectShowcaseActivity, it.message ?: "项目加载失败", Toast.LENGTH_SHORT).show()
                renderList()
            }
            loading = false
        }
    }

    private fun renderList() {
        listColumn.removeAllViews()
        if (projects.isEmpty()) {
            listColumn.addView(emptyText)
            return
        }
        projects.forEach { listColumn.addView(projectCard(it)) }
    }

    private fun projectCard(item: JsonObject): View {
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
            addView(LinearLayout(this@ProjectShowcaseActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                addView(LinearLayout(this@ProjectShowcaseActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(projectImage(item))
                    addView(LinearLayout(this@ProjectShowcaseActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            leftMargin = dp(11)
                        }
                        addView(TextView(this@ProjectShowcaseActivity).apply {
                            text = titleOf(item)
                            applyTitleStyle(16f)
                            maxLines = 1
                        })
                        addView(TextView(this@ProjectShowcaseActivity).apply {
                            text = descOf(item).ifBlank { urlOf(item).ifBlank { "博客项目" } }
                            applyBodyStyle(12.5f)
                            setPadding(0, dp(6), 0, 0)
                            maxLines = 2
                        })
                    })
                })
                addView(LinearLayout(this@ProjectShowcaseActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(12), 0, 0)
                    addView(TextView(this@ProjectShowcaseActivity).apply {
                        text = metaOf(item)
                        applyBodyStyle(11.5f)
                        maxLines = 1
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    urlOf(item).takeIf { it.isNotBlank() }?.let { url ->
                        addView(actionButton("打开") { openUrl(url) })
                    }
                })
            })
        }
    }

    private fun projectImage(item: JsonObject): View {
        val url = repository.absoluteUrl(imageOf(item))
        return FrameLayout(this).apply {
            background = roundedDrawable(Color.parseColor("#F1F3F6"), 13)
            layoutParams = LinearLayout.LayoutParams(dp(68), dp(68))
            val image = ImageView(this@ProjectShowcaseActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            addView(image)
            if (url.isNotBlank()) {
                image.isVisible = true
                Glide.with(image).load(url).placeholder(R.drawable.bg_image_placeholder).centerCrop().into(image)
            } else {
                image.setImageResource(R.drawable.ic_tools)
                image.setColorFilter(KkColors.orange)
                image.setPadding(dp(18), dp(18), dp(18), dp(18))
                image.background = roundedDrawable(Color.parseColor("#FFF1E8"), 13)
            }
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

    private fun showDetailSheet(item: JsonObject) {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(Color.WHITE, 24)
            setPadding(dp(16), dp(14), dp(16), dp(18))
            addView(TextView(this@ProjectShowcaseActivity).apply {
                text = titleOf(item)
                applyTitleStyle(18f)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(12))
            })
            addView(detailRow("地址", urlOf(item).ifBlank { "-" }))
            addView(detailRow("排序", display(item, "sort_order").ifBlank { display(item, "order").ifBlank { "-" } }))
            addView(detailRow("时间", display(item, "created_at").ifBlank { display(item, "updated_at").ifBlank { "-" } }))
            descOf(item).takeIf { it.isNotBlank() }?.let {
                addView(TextView(this@ProjectShowcaseActivity).apply {
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
            urlOf(item).takeIf { it.isNotBlank() }?.let { url ->
                addView(TextView(this@ProjectShowcaseActivity).apply {
                    text = "打开项目"
                    applyTitleStyle(15f)
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    setRoundedBackground(KkColors.orange, 999)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)).apply {
                        topMargin = dp(14)
                    }
                    setOnClickListener { openUrl(url) }
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
            addView(TextView(this@ProjectShowcaseActivity).apply {
                text = label
                applyTitleStyle(13f)
                layoutParams = LinearLayout.LayoutParams(dp(58), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(TextView(this@ProjectShowcaseActivity).apply {
                text = value
                applyBodyStyle(13f)
                setTextColor(KkColors.text)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
    }

    private fun openUrl(value: String) {
        val url = when {
            value.startsWith("http://") || value.startsWith("https://") -> value
            else -> "https://$value"
        }
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(this, "无法打开链接", Toast.LENGTH_SHORT).show()
        }
    }

    private fun titleOf(item: JsonObject): String {
        return display(item, "title")
            .ifBlank { display(item, "name") }
            .ifBlank { "未命名项目" }
    }

    private fun descOf(item: JsonObject): String {
        return display(item, "description")
            .ifBlank { display(item, "summary") }
            .ifBlank { display(item, "content") }
    }

    private fun urlOf(item: JsonObject): String {
        return display(item, "url")
            .ifBlank { display(item, "link") }
            .ifBlank { display(item, "site_url") }
    }

    private fun imageOf(item: JsonObject): String {
        return display(item, "cover")
            .ifBlank { display(item, "image") }
            .ifBlank { display(item, "avatar") }
            .ifBlank { display(item, "icon") }
    }

    private fun metaOf(item: JsonObject): String {
        return listOf(
            display(item, "category"),
            display(item, "created_at"),
            display(item, "updated_at"),
        ).filter { it.isNotBlank() }.take(2).joinToString(" · ").ifBlank { "ID ${display(item, "id").ifBlank { "-" }}" }
    }

    private fun display(item: JsonObject, key: String): String {
        return repository.displayValue(item[key])
    }
}
