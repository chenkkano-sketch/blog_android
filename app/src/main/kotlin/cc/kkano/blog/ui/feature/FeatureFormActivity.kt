package cc.kkano.blog.ui.feature

import android.os.Bundle
import android.text.InputType
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.navigation.FeatureMode
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyDataBox
import cc.kkano.blog.ui.common.dataBox
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.sectionHeader
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class FeatureFormActivity : AppCompatActivity() {
    private val inputs = linkedMapOf<String, TextInputEditText>()
    private var endpoint: String = ""
    private var mode: FeatureMode = FeatureMode.LIST
    private lateinit var submitButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        endpoint = intent.getStringExtra(EXTRA_ENDPOINT).orEmpty()
        mode = runCatching {
            FeatureMode.valueOf(intent.getStringExtra(EXTRA_MODE).orEmpty())
        }.getOrDefault(FeatureMode.LIST)
        setContentView(buildContent(intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "表单" }))
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
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        }
        root.addView(scroll)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(20))
        }
        scroll.addView(content)

        val box = dataBox(marginTop = 0).apply { applyDataBox(14) }
        val formColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(14))
        }
        box.addView(formColumn)
        formColumn.addView(sectionHeader("▌ 原生表单"))

        fieldsFor(mode).forEach { field ->
            val input = TextInputEditText(this).apply {
                hint = field.label
                setSingleLine(!field.multiline)
                inputType = if (field.multiline) {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                } else if (field.password) {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                } else {
                    InputType.TYPE_CLASS_TEXT
                }
                if (field.defaultValue.isNotBlank()) setText(field.defaultValue)
            }
            inputs[field.key] = input
            formColumn.addView(TextInputLayout(this).apply {
                hint = field.label
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                boxBackgroundColor = Color.parseColor("#F8F9FB")
                setBoxCornerRadii(dp(14).toFloat(), dp(14).toFloat(), dp(14).toFloat(), dp(14).toFloat())
                addView(input)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = dp(12)
                }
            })
        }

        submitButton = MaterialButton(this).apply {
            text = submitText(mode)
            setTextColor(KkColors.black)
            setBackgroundColor(KkColors.orange)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52),
            ).apply {
                topMargin = dp(8)
            }
            setOnClickListener { submit() }
        }
        formColumn.addView(submitButton)
        content.addView(box)
        return root
    }

    private fun submit() {
        if (endpoint.isBlank()) {
            Toast.makeText(this, "此功能等待后端适配", Toast.LENGTH_SHORT).show()
            return
        }
        val body = buildBody()
        submitButton.isEnabled = false
        submitButton.text = "提交中..."
        lifecycleScope.launch {
            val result = when (mode) {
                FeatureMode.FORM_PROFILE,
                FeatureMode.FORM_PASSWORD -> runCatching { AppGraph.repository.put(endpoint, body) }
                else -> runCatching { AppGraph.repository.post(endpoint, body) }
            }
            result
                .onSuccess {
                    Toast.makeText(this@FeatureFormActivity, "提交成功", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .onFailure {
                    Toast.makeText(this@FeatureFormActivity, it.message ?: "提交失败", Toast.LENGTH_SHORT).show()
                }
            submitButton.isEnabled = true
            submitButton.text = submitText(mode)
        }
    }

    private fun buildBody(): Map<String, Any?> {
        val values = inputs.mapValues { it.value.text?.toString().orEmpty().trim() }
        return when (mode) {
            FeatureMode.FORM_DYNAMIC -> mapOf(
                "type" to 1,
                "title" to values["title"],
                "content" to values["content"],
                "images" to emptyList<String>(),
            )
            FeatureMode.FORM_ARTICLE -> mapOf(
                "title" to values["title"],
                "summary" to values["summary"],
                "content" to values["content"],
                "category_id" to (values["category_id"]?.toLongOrNull() ?: 1L),
                "is_visible" to true,
            )
            FeatureMode.FORM_COMMENT -> mapOf(
                "type" to (values["type"]?.toIntOrNull() ?: 2),
                "target_id" to values["target_id"]?.toLongOrNull(),
                "content" to values["content"],
                "nickname" to values["nickname"],
                "email" to values["email"],
            ).filterValues { it != null && it.toString().isNotBlank() }
            FeatureMode.FORM_FRIEND_LINK -> values
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
            FeatureMode.FORM_QR_CONFIRM -> mapOf("scene_id" to values["scene_id"])
            FeatureMode.FORM_EMAIL -> mapOf("email" to values["email"], "code" to values["code"])
            else -> values
        }
    }

    private fun fieldsFor(mode: FeatureMode): List<Field> {
        return when (mode) {
            FeatureMode.FORM_DYNAMIC -> listOf(Field("title", "标题"), Field("content", "内容", multiline = true))
            FeatureMode.FORM_ARTICLE -> listOf(
                Field("title", "标题"),
                Field("summary", "摘要", multiline = true),
                Field("content", "正文", multiline = true),
                Field("category_id", "分类 ID", defaultValue = "1"),
            )
            FeatureMode.FORM_COMMENT -> listOf(
                Field("type", "类型：1文章 2留言板 3动态", defaultValue = "2"),
                Field("target_id", "目标 ID，可留空"),
                Field("content", "评论内容", multiline = true),
                Field("nickname", "昵称，登录后可留空"),
                Field("email", "邮箱，登录后可留空"),
            )
            FeatureMode.FORM_FRIEND_LINK -> listOf(
                Field("name", "网站名称"),
                Field("url", "网站地址"),
                Field("avatar", "头像地址"),
                Field("rss_url", "RSS 地址"),
                Field("description", "描述", multiline = true),
            )
            FeatureMode.FORM_PROFILE -> listOf(
                Field("nickname", "昵称"),
                Field("phone", "手机号"),
                Field("avatar", "头像地址"),
            )
            FeatureMode.FORM_PASSWORD -> listOf(
                Field("old_password", "旧密码", password = true),
                Field("new_password", "新密码", password = true),
                Field("confirm_password", "确认新密码", password = true),
            )
            FeatureMode.FORM_REGISTER -> listOf(
                Field("email", "邮箱"),
                Field("password", "密码", password = true),
                Field("code", "邮箱验证码"),
            )
            FeatureMode.FORM_FORGOT_PASSWORD -> listOf(Field("email", "邮箱"))
            FeatureMode.FORM_QR_CONFIRM -> listOf(Field("scene_id", "扫码 scene_id"))
            FeatureMode.FORM_EMAIL -> listOf(Field("email", "新邮箱"), Field("code", "验证码"))
            else -> emptyList()
        }
    }

    private fun submitText(mode: FeatureMode): String {
        return when (mode) {
            FeatureMode.FORM_FORGOT_PASSWORD -> "发送验证码"
            FeatureMode.FORM_QR_CONFIRM -> "确认登录"
            else -> "提交"
        }
    }

    data class Field(
        val key: String,
        val label: String,
        val multiline: Boolean = false,
        val password: Boolean = false,
        val defaultValue: String = "",
    )

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ENDPOINT = "extra_endpoint"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_ROUTE = "extra_route"
    }
}
