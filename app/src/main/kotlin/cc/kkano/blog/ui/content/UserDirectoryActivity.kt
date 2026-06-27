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

class UserDirectoryActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val users = mutableListOf<JsonObject>()
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
        loadUsers(isPage = false)
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = "用户信息",
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
            text = "暂无用户"
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
            addView(ImageView(this@UserDirectoryActivity).apply {
                setImageResource(R.drawable.ic_search)
                setColorFilter(KkColors.softMuted)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            })
            searchInput = EditText(this@UserDirectoryActivity).apply {
                hint = "搜索昵称、邮箱、主页..."
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
            addView(TextView(this@UserDirectoryActivity).apply {
                text = "搜索"
                applyTitleStyle(13f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.orange, 999)
                layoutParams = LinearLayout.LayoutParams(dp(58), dp(36))
                setOnClickListener {
                    page = 1
                    loadUsers(isPage = false)
                }
            })
        }
    }

    private fun loadUsers(isPage: Boolean) {
        if (loading) return
        loading = true
        val nextPage = if (isPage) page + 1 else 1
        if (isPage) loadMoreText.text = "正在加载中..."
        lifecycleScope.launch {
            runCatching {
                repository.genericList(
                    endpoint = ApiRoutes.USERS,
                    page = nextPage,
                    limit = limit,
                    keyword = searchInput.text?.toString().orEmpty().trim(),
                )
            }.onSuccess { list ->
                if (!isPage) users.clear()
                users.addAll(list)
                page = nextPage
                renderList()
                loadMoreText.text = if (list.size < limit) "没有更多了" else "加载更多"
            }.onFailure {
                Toast.makeText(this@UserDirectoryActivity, it.message ?: "用户加载失败", Toast.LENGTH_SHORT).show()
                loadMoreText.text = "加载更多"
                renderList()
            }
            loading = false
        }
    }

    private fun renderList() {
        listColumn.removeAllViews()
        if (users.isEmpty()) {
            listColumn.addView(emptyText)
            return
        }
        users.forEach { listColumn.addView(userCard(it)) }
        listColumn.addView(loadMoreText)
    }

    private fun userCard(item: JsonObject): View {
        return MaterialCardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            setOnClickListener { showDetailSheet(item) }
            addView(LinearLayout(this@UserDirectoryActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                addView(LinearLayout(this@UserDirectoryActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(avatarView(item))
                    addView(LinearLayout(this@UserDirectoryActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            leftMargin = dp(10)
                        }
                        addView(TextView(this@UserDirectoryActivity).apply {
                            text = displayName(item)
                            applyTitleStyle(16f)
                            maxLines = 1
                        })
                        addView(TextView(this@UserDirectoryActivity).apply {
                            text = emailOf(item).ifBlank { urlOf(item).ifBlank { "博客用户" } }
                            applyBodyStyle(12f)
                            setPadding(0, dp(5), 0, 0)
                            maxLines = 1
                        })
                    })
                    addView(rolePill(roleOf(item)))
                })
                signatureOf(item).takeIf { it.isNotBlank() }?.let { intro ->
                    addView(TextView(this@UserDirectoryActivity).apply {
                        text = intro
                        applyBodyStyle(13f)
                        setTextColor(KkColors.text)
                        setRoundedBackground(Color.parseColor("#F7F8FA"), 11)
                        setPadding(dp(10), dp(8), dp(10), dp(8))
                        maxLines = 2
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            topMargin = dp(11)
                        }
                    })
                }
            })
        }
    }

    private fun avatarView(item: JsonObject): View {
        val url = repository.absoluteUrl(display(item, "avatar"))
        val name = displayName(item)
        return FrameLayout(this).apply {
            background = roundedDrawable(Color.parseColor("#F0F1F3"), 999)
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            if (url.isBlank()) {
                addView(TextView(this@UserDirectoryActivity).apply {
                    text = name.take(1).ifBlank { "用" }
                    applyTitleStyle(17f)
                    gravity = Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                })
            } else {
                addView(ImageView(this@UserDirectoryActivity).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    Glide.with(this).load(url).placeholder(R.drawable.bg_avatar).centerCrop().into(this)
                })
            }
        }
    }

    private fun rolePill(label: String): TextView {
        return TextView(this).apply {
            text = label.ifBlank { "用户" }
            applyTitleStyle(11.5f)
            setTextColor(KkColors.orange)
            gravity = Gravity.CENTER
            setRoundedBackground(Color.parseColor("#FFF1E8"), 999)
            setPadding(dp(9), dp(4), dp(9), dp(4))
        }
    }

    private fun showDetailSheet(item: JsonObject) {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(Color.WHITE, 24)
            setPadding(dp(16), dp(14), dp(16), dp(18))
            addView(TextView(this@UserDirectoryActivity).apply {
                text = displayName(item)
                applyTitleStyle(18f)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(12))
            })
            addView(detailRow("邮箱", emailOf(item).ifBlank { "-" }))
            addView(detailRow("主页", urlOf(item).ifBlank { "-" }))
            addView(detailRow("权限", roleOf(item).ifBlank { "-" }))
            addView(detailRow("注册", display(item, "created_at").ifBlank { display(item, "created").ifBlank { "-" } }))
            signatureOf(item).takeIf { it.isNotBlank() }?.let {
                addView(TextView(this@UserDirectoryActivity).apply {
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
            addView(TextView(this@UserDirectoryActivity).apply {
                text = label
                applyTitleStyle(13f)
                layoutParams = LinearLayout.LayoutParams(dp(58), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(TextView(this@UserDirectoryActivity).apply {
                text = value
                applyBodyStyle(13f)
                setTextColor(KkColors.text)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
    }

    private fun loadMore() {
        if (loadMoreText.text == "没有更多了") return
        loadUsers(isPage = true)
    }

    private fun displayName(item: JsonObject): String {
        return display(item, "nickname")
            .ifBlank { display(item, "screenName") }
            .ifBlank { display(item, "username") }
            .ifBlank { display(item, "name") }
            .ifBlank { "未命名用户" }
    }

    private fun emailOf(item: JsonObject): String = display(item, "email").ifBlank { display(item, "mail") }

    private fun urlOf(item: JsonObject): String = display(item, "url").ifBlank { display(item, "website") }

    private fun roleOf(item: JsonObject): String {
        return display(item, "group").ifBlank { display(item, "role").ifBlank { display(item, "groupKey") } }
    }

    private fun signatureOf(item: JsonObject): String {
        return display(item, "description")
            .ifBlank { display(item, "bio") }
            .ifBlank { display(item, "intro") }
            .ifBlank { display(item, "customize") }
    }

    private fun display(item: JsonObject, key: String): String {
        return repository.displayValue(item[key])
    }
}
