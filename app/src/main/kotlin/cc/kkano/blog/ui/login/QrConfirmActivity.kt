package cc.kkano.blog.ui.login

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
import cc.kkano.blog.data.model.User
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.common.setRoundedBackground
import com.bumptech.glide.Glide
import com.google.gson.JsonParser
import kotlinx.coroutines.launch

class QrConfirmActivity : AppCompatActivity() {
    private lateinit var sceneInput: EditText
    private lateinit var avatar: ImageView
    private lateinit var name: TextView
    private lateinit var email: TextView
    private lateinit var confirmButton: TextView
    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        loadUser()
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F7F4FF"))
        }
        root.addView(
            kkTopBar(
                title = "扫码授权",
                leftIcon = R.drawable.ic_back,
                onLeftClick = { finish() },
            ),
        )

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F7F4FF"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        }
        root.addView(scroll)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(24), dp(18), dp(20))
            background = roundedDrawable(Color.WHITE, 16)
            elevation = dp(12).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(dp(16), dp(20), dp(16), 0)
            }
        }
        scroll.addView(card)

        card.addView(TextView(this).apply {
            text = "APP Login"
            applyTitleStyle(12f)
            setTextColor(Color.parseColor("#7C3AED"))
            gravity = Gravity.CENTER
            setPadding(dp(11), dp(5), dp(11), dp(5))
            setRoundedBackground(Color.parseColor("#F1EAFF"), 999)
        })

        card.addView(FrameLayout(this).apply {
            background = roundedDrawable(Color.parseColor("#7C3AED"), 999)
            elevation = dp(8).toFloat()
            layoutParams = LinearLayout.LayoutParams(dp(76), dp(76)).apply {
                topMargin = dp(18)
            }
            avatar = ImageView(this@QrConfirmActivity).apply {
                setImageResource(R.drawable.ic_account)
                setColorFilter(Color.WHITE)
                setPadding(dp(16), dp(16), dp(16), dp(16))
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
            addView(avatar)
        })

        card.addView(TextView(this).apply {
            text = "确认登录网页端"
            applyTitleStyle(22f)
            setTextColor(Color.parseColor("#2B2350"))
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, 0)
        })
        card.addView(TextView(this).apply {
            text = "将使用当前 APP 账户授权网页端登录"
            applyBodyStyle(14f)
            setTextColor(Color.parseColor("#7A708F"))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })

        val userCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setRoundedBackground(Color.parseColor("#FAF7FF"), 12, Color.parseColor("#E8DEFB"), 1)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(18)
            }
        }
        card.addView(userCard)
        name = TextView(this).apply {
            text = "未登录用户"
            applyTitleStyle(17f)
            setTextColor(Color.parseColor("#2B2350"))
        }
        userCard.addView(name)
        email = TextView(this).apply {
            text = "请先登录 APP"
            applyBodyStyle(13f)
            setTextColor(Color.parseColor("#776D8D"))
            setPadding(0, dp(6), 0, 0)
        }
        userCard.addView(email)

        sceneInput = EditText(this).apply {
            hint = "scene_id"
            setText(intent.getStringExtra(EXTRA_SCENE_ID).orEmpty())
            setSingleLine(true)
            textSize = 14f
            setTextColor(KkColors.text)
            setHintTextColor(Color.parseColor("#B9BEC7"))
            background = roundedDrawable(Color.parseColor("#FBF9FF"), 10, Color.parseColor("#EFE7FF"), 1)
            setPadding(dp(12), 0, dp(12), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48),
            ).apply {
                topMargin = dp(18)
            }
        }
        card.addView(sceneInput)

        card.addView(tip("1", "确认后，当前网页会自动登录"))
        card.addView(tip("2", "不会影响你在 APP 上的登录状态"))

        confirmButton = TextView(this).apply {
            text = "确认登录"
            applyTitleStyle(15f)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = roundedDrawable(Color.parseColor("#7C3AED"), 999)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46),
            ).apply {
                topMargin = dp(18)
            }
            setOnClickListener { confirm() }
        }
        card.addView(confirmButton)
        card.addView(TextView(this).apply {
            text = "取消"
            applyBodyStyle(14f)
            setTextColor(Color.parseColor("#7C3AED"))
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, 0)
            setOnClickListener { finish() }
        })
        return root
    }

    private fun tip(index: String, textValue: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setRoundedBackground(Color.parseColor("#FBF9FF"), 10, Color.parseColor("#EFE7FF"), 1)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(12)
            }
            addView(TextView(this@QrConfirmActivity).apply {
                text = index
                applyTitleStyle(12f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = roundedDrawable(Color.parseColor("#7C3AED"), 999)
                layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply {
                    rightMargin = dp(10)
                }
            })
            addView(TextView(this@QrConfirmActivity).apply {
                text = textValue
                applyBodyStyle(13f)
                setTextColor(Color.parseColor("#675D7C"))
            })
        }
    }

    private fun loadUser() {
        user = AppGraph.repository.cachedUser()
        val value = user
        if (value == null) {
            name.text = "未登录用户"
            email.text = "请先登录 APP"
            return
        }
        name.text = value.displayName()
        email.text = value.email.orEmpty().ifBlank { "当前账号未绑定邮箱" }
        val avatarUrl = AppGraph.repository.absoluteUrl(value.avatar)
        if (avatarUrl.isNotBlank()) {
            avatar.clearColorFilter()
            avatar.setPadding(0, 0, 0, 0)
            Glide.with(avatar)
                .load(avatarUrl)
                .placeholder(R.drawable.bg_avatar)
                .centerCrop()
                .into(avatar)
        }
    }

    private fun confirm() {
        val sceneId = parseSceneId(sceneInput.text?.toString().orEmpty())
        if (sceneId.isBlank()) {
            Toast.makeText(this, "二维码信息无效", Toast.LENGTH_SHORT).show()
            return
        }
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }
        confirmButton.isEnabled = false
        confirmButton.text = "授权中..."
        lifecycleScope.launch {
            runCatching {
                runCatching { AppGraph.repository.markQrScanned(sceneId) }
                AppGraph.repository.confirmQrLogin(sceneId)
            }.onSuccess {
                Toast.makeText(this@QrConfirmActivity, "授权成功", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure {
                Toast.makeText(this@QrConfirmActivity, it.message ?: "授权失败", Toast.LENGTH_SHORT).show()
            }
            confirmButton.isEnabled = true
            confirmButton.text = "确认登录"
        }
    }

    private fun parseSceneId(raw: String): String {
        val value = raw.trim()
        if (!value.startsWith("{")) return value
        return runCatching {
            val obj = JsonParser.parseString(value).asJsonObject
            obj["data"]?.asString
                ?: obj["sceneId"]?.asString
                ?: obj["scene_id"]?.asString
                ?: value
        }.getOrDefault(value)
    }

    companion object {
        const val EXTRA_SCENE_ID = "extra_scene_id"
    }
}
