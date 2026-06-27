package cc.kkano.blog.ui.account

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
import cc.kkano.blog.AppGraph
import cc.kkano.blog.BuildConfig
import cc.kkano.blog.R
import cc.kkano.blog.navigation.FeatureLauncher
import cc.kkano.blog.navigation.NativeRouteRegistry
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.common.setRoundedBackground
import com.google.android.material.card.MaterialCardView

class SettingsActivity : AppCompatActivity() {
    private val repository = AppGraph.repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = "系统设置",
                leftIcon = R.drawable.ic_back,
                onLeftClick = { finish() },
            ),
        )
        val scroll = ScrollView(this).apply {
            setBackgroundColor(KkColors.background)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(scroll)
        scroll.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), dp(12), dp(11), dp(24))
            addView(accountCard())
            addView(settingsCard())
        })
        return root
    }

    private fun accountCard(): View {
        val user = repository.cachedUser()
        val name = user?.displayName().orEmpty().ifBlank { "未登录账号" }
        val email = user?.email.orEmpty().ifBlank { "blog_android ${BuildConfig.VERSION_NAME}" }
        return MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(5).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(LinearLayout(this@SettingsActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                addView(FrameLayout(this@SettingsActivity).apply {
                    background = roundedDrawable(Color.parseColor("#FFF1E8"), 999)
                    layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
                    addView(ImageView(this@SettingsActivity).apply {
                        setImageResource(R.drawable.ic_settings)
                        setColorFilter(KkColors.orange)
                        setPadding(dp(13), dp(13), dp(13), dp(13))
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    })
                })
                addView(LinearLayout(this@SettingsActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        leftMargin = dp(12)
                    }
                    addView(TextView(this@SettingsActivity).apply {
                        text = name
                        applyTitleStyle(17f)
                        maxLines = 1
                    })
                    addView(TextView(this@SettingsActivity).apply {
                        text = email
                        applyBodyStyle(12.5f)
                        setPadding(0, dp(6), 0, 0)
                        maxLines = 1
                    })
                })
                addView(TextView(this@SettingsActivity).apply {
                    text = BuildConfig.VERSION_NAME
                    applyTitleStyle(11.5f)
                    setTextColor(KkColors.orange)
                    gravity = Gravity.CENTER
                    setRoundedBackground(Color.parseColor("#FFF1E8"), 999)
                    setPadding(dp(10), dp(5), dp(10), dp(5))
                })
            })
        }
    }

    private fun settingsCard(): View {
        return MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(4).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
            }
            addView(LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(10))
                addView(settingRow("个人信息", "昵称、头像与主页资料", R.drawable.ic_edit) { openRoute("pages/user/useredit") })
                addView(settingRow("账号绑定", "第三方账号与登录设备", R.drawable.ic_link) { openRoute("pages/user/userbind") })
                addView(settingRow("修改密码", "维护登录安全", R.drawable.ic_lock) { openRoute("pages/user/password") })
                addView(settingRow("本地资源库", "图片、壁纸和视频目录", R.drawable.ic_image) { openRoute("pages/user/local-library") })
                addView(settingRow("清理缓存", "刷新本地临时状态", R.drawable.ic_delete) {
                    Toast.makeText(this@SettingsActivity, "缓存已清理", Toast.LENGTH_SHORT).show()
                })
                if (repository.cachedUser() != null) {
                    addView(settingRow("退出登录", "移除本机登录状态", R.drawable.ic_account, danger = true) {
                        repository.logout()
                        Toast.makeText(this@SettingsActivity, "已退出登录", Toast.LENGTH_SHORT).show()
                        finish()
                    })
                }
            })
        }
    }

    private fun settingRow(
        title: String,
        subtitle: String,
        icon: Int,
        danger: Boolean = false,
        onClick: () -> Unit,
    ): View {
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setRoundedBackground(Color.parseColor("#F7F8FA"), 12)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(62)).apply {
                bottomMargin = dp(8)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            addView(ImageView(this@SettingsActivity).apply {
                setImageResource(icon)
                setColorFilter(if (danger) KkColors.danger else KkColors.black)
                layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply {
                    rightMargin = dp(10)
                }
            })
            addView(LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@SettingsActivity).apply {
                    text = title
                    applyTitleStyle(14.5f)
                    setTextColor(if (danger) KkColors.danger else KkColors.black)
                })
                addView(TextView(this@SettingsActivity).apply {
                    text = subtitle
                    applyBodyStyle(11.5f)
                    setPadding(0, dp(5), 0, 0)
                    maxLines = 1
                })
            })
            addView(TextView(this@SettingsActivity).apply {
                text = "›"
                textSize = 24f
                setTextColor(Color.parseColor("#BDC1C8"))
                includeFontPadding = false
            })
        }
    }

    private fun openRoute(route: String) {
        NativeRouteRegistry.find(route)?.let { FeatureLauncher.open(this, it) }
    }
}
