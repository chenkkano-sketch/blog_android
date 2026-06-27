package cc.kkano.blog.ui.account

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
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
import cc.kkano.blog.navigation.FeatureMode
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.common.setRoundedBackground
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class AccountFormActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val inputs = linkedMapOf<String, EditText>()
    private var endpoint: String = ""
    private var route: String = ""
    private var mode: FeatureMode = FeatureMode.LIST
    private lateinit var submitButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        endpoint = intent.getStringExtra(EXTRA_ENDPOINT).orEmpty()
        route = intent.getStringExtra(EXTRA_ROUTE).orEmpty()
        mode = runCatching { FeatureMode.valueOf(intent.getStringExtra(EXTRA_MODE).orEmpty()) }
            .getOrDefault(FeatureMode.LIST)
        setContentView(buildContent(intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "账号" }))
    }

    private fun buildContent(titleText: String): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = titleText,
                leftIcon = R.drawable.ic_back,
                onLeftClick = { finish() },
            ),
        )
        val scroll = ScrollView(this).apply {
            setBackgroundColor(KkColors.background)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(scroll)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(14), dp(12), dp(24))
        }
        scroll.addView(content)

        content.addView(headerCard(titleText))
        content.addView(formCard(titleText))
        return root
    }

    private fun headerCard(titleText: String): View {
        val user = repository.cachedUser()
        val displayName = user?.displayName().orEmpty().ifBlank { titleText }
        val subtitle = when (mode) {
            FeatureMode.FORM_REGISTER -> "创建博客账号"
            FeatureMode.FORM_FORGOT_PASSWORD -> "邮箱找回密码"
            FeatureMode.FORM_EMAIL -> user?.email.orEmpty().ifBlank { "绑定新的邮箱地址" }
            FeatureMode.FORM_PASSWORD -> "维护登录安全"
            else -> user?.email.orEmpty().ifBlank { "完善账号资料" }
        }
        return MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(5).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(LinearLayout(this@AccountFormActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                addView(FrameLayout(this@AccountFormActivity).apply {
                    background = roundedDrawable(KkColors.black, 999, KkColors.orange, 2)
                    elevation = dp(5).toFloat()
                    layoutParams = LinearLayout.LayoutParams(dp(54), dp(54))
                    addView(ImageView(this@AccountFormActivity).apply {
                        setImageResource(iconForHeader())
                        setColorFilter(Color.WHITE)
                        setPadding(dp(13), dp(13), dp(13), dp(13))
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    })
                })
                addView(LinearLayout(this@AccountFormActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        leftMargin = dp(12)
                    }
                    addView(TextView(this@AccountFormActivity).apply {
                        text = displayName
                        applyTitleStyle(17f)
                        maxLines = 1
                    })
                    addView(TextView(this@AccountFormActivity).apply {
                        text = subtitle
                        applyBodyStyle(12.5f)
                        setPadding(0, dp(6), 0, 0)
                        maxLines = 1
                    })
                })
            })
        }
    }

    private fun formCard(titleText: String): View {
        return MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(4).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
            }
            addView(LinearLayout(this@AccountFormActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(13), dp(12), dp(13), dp(14))
                addView(TextView(this@AccountFormActivity).apply {
                    text = titleText
                    applyTitleStyle(16f)
                    setPadding(dp(2), 0, 0, dp(12))
                })
                fieldsFor().forEach { field ->
                    addView(inputRow(field))
                }
                submitButton = TextView(this@AccountFormActivity).apply {
                    text = submitText()
                    applyTitleStyle(15.5f)
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    setRoundedBackground(KkColors.orange, 999)
                    elevation = dp(5).toFloat()
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply {
                        topMargin = dp(8)
                    }
                    setOnClickListener { submit() }
                }
                addView(submitButton)
            })
        }
    }

    private fun inputRow(field: Field): View {
        val row = LinearLayout(this).apply {
            gravity = if (field.multiline) Gravity.TOP else Gravity.CENTER_VERTICAL
            setPadding(dp(12), if (field.multiline) dp(10) else 0, dp(12), if (field.multiline) dp(10) else 0)
            setRoundedBackground(Color.parseColor("#F7F8FA"), 12, KkColors.line, 1)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                if (field.multiline) dp(118) else dp(52),
            ).apply {
                bottomMargin = dp(10)
            }
        }
        val iconView = ImageView(this).apply {
            setImageResource(field.icon)
            setColorFilter(KkColors.softMuted)
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                rightMargin = dp(9)
                topMargin = if (field.multiline) dp(3) else 0
            }
        }
        row.addView(iconView)
        val input = EditText(this).apply {
            hint = field.label
            setText(field.value)
            setSingleLine(!field.multiline)
            minLines = if (field.multiline) 4 else 1
            maxLines = if (field.multiline) 6 else 1
            gravity = if (field.multiline) Gravity.TOP else Gravity.CENTER_VERTICAL
            inputType = when {
                field.multiline -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                field.password -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                field.email -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                else -> InputType.TYPE_CLASS_TEXT
            }
            background = null
            textSize = 14f
            includeFontPadding = false
            setTextColor(KkColors.text)
            setHintTextColor(KkColors.softMuted)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            setOnFocusChangeListener { _, focused ->
                row.setRoundedBackground(
                    if (focused) Color.parseColor("#FFF8F2") else Color.parseColor("#F7F8FA"),
                    12,
                    if (focused) KkColors.orange else KkColors.line,
                    if (focused) 2 else 1,
                )
                iconView.setColorFilter(if (focused) KkColors.orange else KkColors.softMuted)
            }
        }
        inputs[field.key] = input
        row.addView(input)
        return row
    }

    private fun submit() {
        if (endpoint.isBlank()) {
            Toast.makeText(this, "当前页面没有可提交的数据", Toast.LENGTH_SHORT).show()
            return
        }
        val body = buildBody()
        if (!validate(body)) return
        submitButton.isEnabled = false
        submitButton.text = "提交中..."
        lifecycleScope.launch {
            val result = when (mode) {
                FeatureMode.FORM_PROFILE,
                FeatureMode.FORM_PASSWORD -> runCatching { repository.put(endpoint, body) }
                else -> runCatching { repository.post(endpoint, body) }
            }
            result.onSuccess {
                if (mode == FeatureMode.FORM_PROFILE || mode == FeatureMode.FORM_EMAIL) {
                    runCatching { repository.userInfo() }
                }
                Toast.makeText(this@AccountFormActivity, successText(), Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure {
                Toast.makeText(this@AccountFormActivity, it.message ?: "提交失败", Toast.LENGTH_SHORT).show()
            }
            submitButton.isEnabled = true
            submitButton.text = submitText()
        }
    }

    private fun validate(body: Map<String, Any?>): Boolean {
        val required = when (mode) {
            FeatureMode.FORM_PASSWORD -> listOf("old_password", "new_password", "confirm_password")
            FeatureMode.FORM_REGISTER -> listOf("email", "password", "code")
            FeatureMode.FORM_FORGOT_PASSWORD -> listOf("email")
            FeatureMode.FORM_EMAIL -> listOf("email", "code")
            else -> emptyList()
        }
        if (required.any { body[it].toString().isBlank() }) {
            Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
            return false
        }
        if (mode == FeatureMode.FORM_PASSWORD && body["new_password"] != body["confirm_password"]) {
            Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun buildBody(): Map<String, Any?> {
        val values = inputs.mapValues { it.value.text?.toString().orEmpty().trim() }
        return when (mode) {
            FeatureMode.FORM_PROFILE -> values.filterValues { it.isNotBlank() }
            FeatureMode.FORM_PASSWORD -> mapOf(
                "old_password" to values["old_password"],
                "new_password" to values["new_password"],
                "confirm_password" to values["confirm_password"],
            )
            FeatureMode.FORM_REGISTER -> mapOf(
                "email" to values["email"],
                "password" to values["password"],
                "code" to values["code"],
            )
            FeatureMode.FORM_FORGOT_PASSWORD -> mapOf("email" to values["email"])
            FeatureMode.FORM_EMAIL -> mapOf("email" to values["email"], "code" to values["code"])
            else -> values
        }
    }

    private fun fieldsFor(): List<Field> {
        val user = repository.cachedUser()
        return when {
            route == "pages/user/avatar" -> listOf(
                Field("avatar", "头像地址", R.drawable.ic_image, value = user?.avatar.orEmpty()),
            )
            mode == FeatureMode.FORM_PROFILE -> listOf(
                Field("nickname", "昵称", R.drawable.ic_account, value = user?.nickname.orEmpty()),
                Field("phone", "手机号", R.drawable.ic_message),
                Field("avatar", "头像地址", R.drawable.ic_image, value = user?.avatar.orEmpty()),
                Field("url", "个人网站", R.drawable.ic_link),
            )
            mode == FeatureMode.FORM_PASSWORD -> listOf(
                Field("old_password", "当前密码", R.drawable.ic_lock, password = true),
                Field("new_password", "新密码", R.drawable.ic_lock, password = true),
                Field("confirm_password", "确认新密码", R.drawable.ic_lock, password = true),
            )
            mode == FeatureMode.FORM_REGISTER -> listOf(
                Field("email", "邮箱", R.drawable.ic_message, email = true),
                Field("password", "密码", R.drawable.ic_lock, password = true),
                Field("code", "邮箱验证码", R.drawable.ic_lock),
            )
            mode == FeatureMode.FORM_FORGOT_PASSWORD -> listOf(
                Field("email", "邮箱", R.drawable.ic_message, email = true),
            )
            mode == FeatureMode.FORM_EMAIL -> listOf(
                Field("email", "新邮箱", R.drawable.ic_message, email = true),
                Field("code", "验证码", R.drawable.ic_lock),
            )
            else -> emptyList()
        }
    }

    private fun iconForHeader(): Int {
        return when (mode) {
            FeatureMode.FORM_PASSWORD -> R.drawable.ic_lock
            FeatureMode.FORM_EMAIL,
            FeatureMode.FORM_FORGOT_PASSWORD -> R.drawable.ic_message
            FeatureMode.FORM_REGISTER -> R.drawable.ic_add
            else -> R.drawable.ic_account
        }
    }

    private fun submitText(): String {
        return when (mode) {
            FeatureMode.FORM_FORGOT_PASSWORD -> "发送验证码"
            FeatureMode.FORM_REGISTER -> "立即注册"
            FeatureMode.FORM_PASSWORD -> "保存密码"
            FeatureMode.FORM_EMAIL -> "绑定邮箱"
            else -> "保存修改"
        }
    }

    private fun successText(): String {
        return when (mode) {
            FeatureMode.FORM_FORGOT_PASSWORD -> "验证码已发送"
            FeatureMode.FORM_REGISTER -> "注册成功"
            else -> "保存成功"
        }
    }

    private data class Field(
        val key: String,
        val label: String,
        val icon: Int,
        val password: Boolean = false,
        val email: Boolean = false,
        val multiline: Boolean = false,
        val value: String = "",
    )

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ENDPOINT = "extra_endpoint"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_ROUTE = "extra_route"
    }
}
