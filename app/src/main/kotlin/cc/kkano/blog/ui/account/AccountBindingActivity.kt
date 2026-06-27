package cc.kkano.blog.ui.account

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.common.setRoundedBackground
import com.google.android.material.card.MaterialCardView
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class AccountBindingActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val devices = mutableListOf<JsonObject>()
    private val providers = listOf(
        Provider("qq", "QQ", "#12B7F5", "企鹅号授权"),
        Provider("wechat", "微信", "#07C160", "微信开放平台"),
        Provider("weibo", "微博", "#E6162D", "微博授权"),
    )
    private var loading = false

    private lateinit var contentColumn: LinearLayout
    private lateinit var deviceColumn: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        loadDevices()
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = "账号绑定",
                leftIcon = R.drawable.ic_back,
                rightIcon = R.drawable.ic_settings,
                onLeftClick = { finish() },
                onRightClick = { loadDevices() },
            ),
        )
        val scroll = ScrollView(this).apply {
            setBackgroundColor(KkColors.background)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(scroll)
        contentColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), dp(12), dp(11), dp(22))
        }
        scroll.addView(contentColumn)
        render()
        return root
    }

    private fun render() {
        contentColumn.removeAllViews()
        contentColumn.addView(profileCard())
        contentColumn.addView(statusSection())
        contentColumn.addView(sectionTitle("登录设备", "刷新") { loadDevices() })
        deviceColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        contentColumn.addView(deviceColumn)
        renderDevices()
    }

    private fun profileCard(): View {
        val user = repository.cachedUser()
        val name = user?.nickname.orEmpty()
            .ifBlank { user?.username.orEmpty() }
            .ifBlank { "当前账号" }
        val email = user?.email.orEmpty().ifBlank { "blog_android" }
        return MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(5).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(LinearLayout(this@AccountBindingActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                addView(LinearLayout(this@AccountBindingActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(FrameLayout(this@AccountBindingActivity).apply {
                        background = roundedDrawable(Color.parseColor("#FFF1E8"), 999)
                        layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
                        addView(TextView(this@AccountBindingActivity).apply {
                            text = name.take(1).ifBlank { "账" }
                            applyTitleStyle(18f)
                            setTextColor(KkColors.orange)
                            gravity = Gravity.CENTER
                            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        })
                    })
                    addView(LinearLayout(this@AccountBindingActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            leftMargin = dp(12)
                        }
                        addView(TextView(this@AccountBindingActivity).apply {
                            text = name
                            applyTitleStyle(17f)
                            maxLines = 1
                        })
                        addView(TextView(this@AccountBindingActivity).apply {
                            text = email
                            applyBodyStyle(12.5f)
                            setPadding(0, dp(6), 0, 0)
                            maxLines = 1
                        })
                    })
                    addView(statusPill(if (loading) "同步中" else "${devices.size} 台设备", KkColors.orange, Color.parseColor("#FFF1E8")))
                })
                addView(LinearLayout(this@AccountBindingActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(14), 0, 0)
                    addView(statBox("绑定", "${boundProviderCount()}/${providers.size}"))
                    addView(statBox("设备", devices.size.toString()))
                    addView(statBox("安全", if (devices.isEmpty()) "待同步" else "正常"))
                })
            })
        }
    }

    private fun statBox(label: String, value: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setRoundedBackground(Color.parseColor("#F7F8FA"), 12)
            setPadding(dp(6), dp(10), dp(6), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(3), 0, dp(3), 0)
            }
            addView(TextView(this@AccountBindingActivity).apply {
                text = value
                applyTitleStyle(16f)
                gravity = Gravity.CENTER
            })
            addView(TextView(this@AccountBindingActivity).apply {
                text = label
                applyBodyStyle(11.5f)
                gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, 0)
            })
        }
    }

    private fun statusSection(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
            }
            addView(sectionTitle("第三方账号", null, null))
            providers.forEach { provider ->
                addView(providerRow(provider))
            }
        }
    }

    private fun providerRow(provider: Provider): View {
        val bound = isProviderBound(provider.key)
        val color = Color.parseColor(provider.color)
        return MaterialCardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(9)
            }
            addView(LinearLayout(this@AccountBindingActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                addView(FrameLayout(this@AccountBindingActivity).apply {
                    background = roundedDrawable(lightColor(color), 999)
                    layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                    addView(ImageView(this@AccountBindingActivity).apply {
                        setImageResource(R.drawable.ic_link)
                        setColorFilter(color)
                        setPadding(dp(11), dp(11), dp(11), dp(11))
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    })
                })
                addView(LinearLayout(this@AccountBindingActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        leftMargin = dp(11)
                    }
                    addView(TextView(this@AccountBindingActivity).apply {
                        text = provider.label
                        applyTitleStyle(15.5f)
                    })
                    addView(TextView(this@AccountBindingActivity).apply {
                        text = provider.subtitle
                        applyBodyStyle(12f)
                        setPadding(0, dp(5), 0, 0)
                    })
                })
                addView(statusPill(if (bound) "已绑定" else "未绑定", if (bound) color else KkColors.softMuted, if (bound) lightColor(color) else Color.parseColor("#F2F3F5")))
            })
        }
    }

    private fun renderDevices() {
        if (!::deviceColumn.isInitialized) return
        deviceColumn.removeAllViews()
        if (devices.isEmpty()) {
            deviceColumn.addView(emptyStateView())
            return
        }
        devices.forEach { deviceColumn.addView(deviceCard(it)) }
    }

    private fun emptyStateView(): TextView {
        return TextView(this).apply {
            text = "暂无登录设备"
            applyBodyStyle(15f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(120))
        }
    }

    private fun deviceCard(item: JsonObject): View {
        return MaterialCardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            addView(LinearLayout(this@AccountBindingActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                addView(LinearLayout(this@AccountBindingActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(FrameLayout(this@AccountBindingActivity).apply {
                        background = roundedDrawable(Color.parseColor("#F1F3F6"), 999)
                        layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                        addView(ImageView(this@AccountBindingActivity).apply {
                            setImageResource(R.drawable.ic_lock)
                            setColorFilter(KkColors.black)
                            setPadding(dp(11), dp(11), dp(11), dp(11))
                            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        })
                    })
                    addView(LinearLayout(this@AccountBindingActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            leftMargin = dp(11)
                        }
                        addView(TextView(this@AccountBindingActivity).apply {
                            text = deviceTitle(item)
                            applyTitleStyle(15.5f)
                            maxLines = 1
                        })
                        addView(TextView(this@AccountBindingActivity).apply {
                            text = deviceSubtitle(item)
                            applyBodyStyle(12f)
                            setPadding(0, dp(5), 0, 0)
                            maxLines = 2
                        })
                    })
                    addView(statusPill(display(item, "status").ifBlank { "在线" }, KkColors.orange, Color.parseColor("#FFF1E8")))
                })
                addView(LinearLayout(this@AccountBindingActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(12), 0, 0)
                    addView(TextView(this@AccountBindingActivity).apply {
                        text = listOf(display(item, "created_at"), display(item, "last_active_at"), display(item, "updated_at"))
                            .firstOrNull { it.isNotBlank() }
                            ?.let { "最近 $it" }
                            ?: "ID ${idOf(item)}"
                        applyBodyStyle(11.5f)
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    if (idOf(item) > 0L) {
                        addView(actionButton("解绑", KkColors.danger) { confirmDelete(item) })
                    }
                })
            })
        }
    }

    private fun sectionTitle(title: String, action: String?, onActionClick: (() -> Unit)?): View {
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(12), dp(2), dp(9))
            addView(TextView(this@AccountBindingActivity).apply {
                text = title
                applyTitleStyle(16f)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (action != null) {
                addView(TextView(this@AccountBindingActivity).apply {
                    text = action
                    applyTitleStyle(12.5f)
                    setTextColor(KkColors.orange)
                    gravity = Gravity.CENTER
                    setRoundedBackground(Color.parseColor("#FFF1E8"), 999)
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    setOnClickListener { onActionClick?.invoke() }
                })
            }
        }
    }

    private fun statusPill(label: String, color: Int, backgroundColor: Int): TextView {
        return TextView(this).apply {
            text = label
            applyTitleStyle(11.5f)
            setTextColor(color)
            gravity = Gravity.CENTER
            setRoundedBackground(backgroundColor, 999)
            setPadding(dp(9), dp(4), dp(9), dp(4))
        }
    }

    private fun actionButton(label: String, color: Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            applyTitleStyle(12.5f)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setRoundedBackground(color, 999)
            layoutParams = LinearLayout.LayoutParams(dp(68), dp(34))
            setOnClickListener { onClick() }
        }
    }

    private fun loadDevices() {
        if (loading) return
        loading = true
        render()
        lifecycleScope.launch {
            runCatching { repository.userDevices(limit = 100) }
                .onSuccess { list ->
                    devices.clear()
                    devices.addAll(list)
                    loading = false
                    render()
                }
                .onFailure {
                    loading = false
                    Toast.makeText(this@AccountBindingActivity, it.message ?: "账号绑定加载失败", Toast.LENGTH_SHORT).show()
                    render()
                }
        }
    }

    private fun confirmDelete(item: JsonObject) {
        AlertDialog.Builder(this)
            .setTitle("确认解绑")
            .setMessage("确定移除「${deviceTitle(item)}」吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("解绑") { _, _ ->
                lifecycleScope.launch {
                    runCatching { repository.deleteUserDevice(idOf(item)) }
                        .onSuccess {
                            Toast.makeText(this@AccountBindingActivity, "解绑成功", Toast.LENGTH_SHORT).show()
                            loadDevices()
                        }
                        .onFailure { Toast.makeText(this@AccountBindingActivity, it.message ?: "解绑失败", Toast.LENGTH_SHORT).show() }
                }
            }
            .show()
    }

    private fun boundProviderCount(): Int {
        return providers.count { isProviderBound(it.key) }
    }

    private fun isProviderBound(key: String): Boolean {
        val aliases = when (key) {
            "wechat" -> listOf("wechat", "weixin", "wx", "微信")
            "weibo" -> listOf("weibo", "微博")
            else -> listOf(key, "qq")
        }
        return devices.any { item ->
            val text = item.entrySet().joinToString(" ") { (_, value) -> repository.displayValue(value) }.lowercase()
            aliases.any { alias -> text.contains(alias.lowercase()) }
        }
    }

    private fun deviceTitle(item: JsonObject): String {
        return display(item, "device_name")
            .ifBlank { display(item, "name") }
            .ifBlank { display(item, "platform") }
            .ifBlank { display(item, "type") }
            .ifBlank { display(item, "browser") }
            .ifBlank { "登录设备" }
    }

    private fun deviceSubtitle(item: JsonObject): String {
        return listOf(
            display(item, "os"),
            display(item, "browser"),
            display(item, "ip").ifBlank { display(item, "ip_address") },
            display(item, "location"),
        ).filter { it.isNotBlank() }.distinct().joinToString(" · ").ifBlank { "设备信息待同步" }
    }

    private fun idOf(item: JsonObject): Long {
        return runCatching { item["id"]?.asLong }.getOrNull()
            ?: runCatching { item["device_id"]?.asLong }.getOrNull()
            ?: 0L
    }

    private fun display(item: JsonObject, key: String): String {
        return repository.displayValue(item[key])
    }

    private fun lightColor(color: Int): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.rgb(
            (red + (255 - red) * 0.88f).toInt(),
            (green + (255 - green) * 0.88f).toInt(),
            (blue + (255 - blue) * 0.88f).toInt(),
        )
    }

    private data class Provider(
        val key: String,
        val label: String,
        val color: String,
        val subtitle: String,
    )
}
