package cc.kkano.blog.ui.manage

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
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

class UserManagerActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val users = mutableListOf<JsonObject>()
    private val groups = listOf(
        "administrator" to "管理员",
        "editor" to "编辑",
        "contributor" to "贡献者",
        "subscriber" to "关注者",
        "visitor" to "游客",
    )
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
                title = "用户管理",
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
            text = "暂时没有用户"
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
            addView(ImageView(this@UserManagerActivity).apply {
                setImageResource(R.drawable.ic_search)
                setColorFilter(KkColors.softMuted)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            })
            searchInput = EditText(this@UserManagerActivity).apply {
                hint = "输入搜索关键字"
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
            addView(TextView(this@UserManagerActivity).apply {
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
                Toast.makeText(this@UserManagerActivity, it.message ?: "用户加载失败", Toast.LENGTH_SHORT).show()
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
            addView(LinearLayout(this@UserManagerActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                addView(LinearLayout(this@UserManagerActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(avatarView(item))
                    addView(LinearLayout(this@UserManagerActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            leftMargin = dp(10)
                        }
                        addView(TextView(this@UserManagerActivity).apply {
                            text = displayName(item)
                            applyTitleStyle(15.5f)
                            maxLines = 1
                        })
                        addView(TextView(this@UserManagerActivity).apply {
                            text = display(item, "email").ifBlank { display(item, "mail") }.ifBlank { "未设置邮箱" }
                            applyBodyStyle(12f)
                            setPadding(0, dp(5), 0, 0)
                            maxLines = 1
                        })
                    })
                    addView(groupPill(groupLabel(groupKey(item))))
                })
                addView(TextView(this@UserManagerActivity).apply {
                    text = listOf(
                        display(item, "username").ifBlank { display(item, "name") },
                        display(item, "url").ifBlank { display(item, "website") },
                        display(item, "created_at"),
                    ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "暂无更多资料" }
                    applyBodyStyle(12f)
                    setRoundedBackground(Color.parseColor("#F7F8FA"), 10)
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = dp(12)
                    }
                    maxLines = 2
                })
                addView(LinearLayout(this@UserManagerActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(12), 0, 0)
                    addView(TextView(this@UserManagerActivity).apply {
                        text = "ID ${idOf(item)}"
                        applyBodyStyle(11.5f)
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    addView(actionButton("编辑", Color.parseColor("#2F80ED")) { showEditSheet(item) })
                    addView(actionButton("删除", KkColors.danger) { confirmDelete(item) })
                })
            })
        }
    }

    private fun avatarView(item: JsonObject): View {
        val url = display(item, "avatar")
        val name = displayName(item)
        return FrameLayout(this).apply {
            background = roundedDrawable(Color.parseColor("#F0F1F3"), 999)
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            if (url.isBlank()) {
                addView(TextView(this@UserManagerActivity).apply {
                    text = name.take(1).ifBlank { "用" }
                    applyTitleStyle(17f)
                    gravity = Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                })
            } else {
                addView(ImageView(this@UserManagerActivity).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    Glide.with(this).load(repository.absoluteUrl(url)).placeholder(R.drawable.bg_avatar).centerCrop().into(this)
                })
            }
        }
    }

    private fun showEditSheet(item: JsonObject) {
        val dialog = BottomSheetDialog(this)
        val nickname = editInput("请输入昵称", display(item, "nickname").ifBlank { display(item, "screenName") })
        val email = editInput("请输入邮箱", display(item, "email").ifBlank { display(item, "mail") })
        val url = editInput("请输入网址", display(item, "url").ifBlank { display(item, "website") })
        val title = editInput("为用户自定义头衔", display(item, "customize").ifBlank { display(item, "title") })
        val group = editInput("administrator/editor/contributor/subscriber/visitor", groupKey(item))
        val password = editInput("不填则不修改", "").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(Color.WHITE, 24)
            setPadding(dp(16), dp(14), dp(16), dp(18))
            addView(TextView(this@UserManagerActivity).apply {
                text = "用户编辑"
                applyTitleStyle(18f)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(12))
            })
            addView(readOnlyRow("用户名", display(item, "username").ifBlank { display(item, "name").ifBlank { "未命名" } }))
            addView(editRow("昵称", nickname))
            addView(editRow("邮箱", email))
            addView(editRow("网址", url))
            addView(editRow("头衔", title))
            addView(editRow("权限", group))
            addView(editRow("密码", password))
            addView(TextView(this@UserManagerActivity).apply {
                text = "保存修改"
                applyTitleStyle(15f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.orange, 999)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)).apply {
                    topMargin = dp(14)
                }
                setOnClickListener {
                    saveUser(
                        id = idOf(item),
                        body = buildMap {
                            put("nickname", nickname.text?.toString().orEmpty().trim())
                            put("screenName", nickname.text?.toString().orEmpty().trim())
                            put("email", email.text?.toString().orEmpty().trim())
                            put("mail", email.text?.toString().orEmpty().trim())
                            put("url", url.text?.toString().orEmpty().trim())
                            put("website", url.text?.toString().orEmpty().trim())
                            put("customize", title.text?.toString().orEmpty().trim())
                            put("group", group.text?.toString().orEmpty().trim())
                            put("groupKey", group.text?.toString().orEmpty().trim())
                            val pass = password.text?.toString().orEmpty().trim()
                            if (pass.isNotBlank()) put("password", pass)
                        },
                        dialog = dialog,
                    )
                }
            })
        }
        dialog.setContentView(root)
        dialog.show()
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.window?.setDimAmount(0f)
    }

    private fun editInput(hintText: String, value: String): EditText {
        return EditText(this).apply {
            hint = hintText
            setText(value)
            setSingleLine(true)
            textSize = 14f
            setTextColor(KkColors.text)
            setHintTextColor(KkColors.softMuted)
            background = null
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
            addView(TextView(this@UserManagerActivity).apply {
                text = label
                applyTitleStyle(13f)
                layoutParams = LinearLayout.LayoutParams(dp(52), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }
    }

    private fun readOnlyRow(label: String, value: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setRoundedBackground(Color.parseColor("#F7F8FA"), 11)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply {
                topMargin = dp(9)
            }
            addView(TextView(this@UserManagerActivity).apply {
                text = label
                applyTitleStyle(13f)
                layoutParams = LinearLayout.LayoutParams(dp(52), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(TextView(this@UserManagerActivity).apply {
                text = value
                applyBodyStyle(14f)
                setTextColor(KkColors.muted)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
    }

    private fun saveUser(id: Long, body: Map<String, Any?>, dialog: BottomSheetDialog) {
        if (id <= 0L) return
        lifecycleScope.launch {
            runCatching { repository.updateUser(id, body) }
                .onSuccess {
                    Toast.makeText(this@UserManagerActivity, "保存成功", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    page = 1
                    loadUsers(isPage = false)
                }
                .onFailure { Toast.makeText(this@UserManagerActivity, it.message ?: "保存失败", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun confirmDelete(item: JsonObject) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定删除用户「${displayName(item)}」吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    runCatching { repository.deleteUser(idOf(item)) }
                        .onSuccess {
                            Toast.makeText(this@UserManagerActivity, "删除成功", Toast.LENGTH_SHORT).show()
                            page = 1
                            loadUsers(isPage = false)
                        }
                        .onFailure { Toast.makeText(this@UserManagerActivity, it.message ?: "删除失败", Toast.LENGTH_SHORT).show() }
                }
            }
            .show()
    }

    private fun actionButton(label: String, color: Int, onClick: () -> Unit): TextView {
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

    private fun groupPill(label: String): TextView {
        return TextView(this).apply {
            text = label
            applyTitleStyle(11.5f)
            setTextColor(KkColors.orange)
            gravity = Gravity.CENTER
            setRoundedBackground(Color.parseColor("#FFF1E8"), 999)
            setPadding(dp(9), dp(4), dp(9), dp(4))
        }
    }

    private fun loadMore() {
        if (loadMoreText.text == "没有更多了") return
        loadUsers(isPage = true)
    }

    private fun idOf(item: JsonObject): Long {
        return runCatching { item["id"]?.asLong }.getOrNull()
            ?: runCatching { item["uid"]?.asLong }.getOrNull()
            ?: 0L
    }

    private fun displayName(item: JsonObject): String {
        return display(item, "nickname")
            .ifBlank { display(item, "screenName") }
            .ifBlank { display(item, "username") }
            .ifBlank { display(item, "name") }
            .ifBlank { "未命名用户" }
    }

    private fun groupKey(item: JsonObject): String {
        return display(item, "group")
            .ifBlank { display(item, "groupKey") }
            .ifBlank { display(item, "role") }
            .ifBlank { "subscriber" }
    }

    private fun groupLabel(value: String): String {
        return groups.firstOrNull { it.first == value }?.second ?: value
    }

    private fun display(item: JsonObject, key: String): String {
        return repository.displayValue(item[key])
    }
}
