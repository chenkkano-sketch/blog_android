package cc.kkano.blog.ui.login

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
    }

    private fun buildContent(): ScrollView {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.kk_surface))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(54), dp(28), dp(28))
        }
        scroll.addView(root)

        val logo = TextView(this).apply {
            text = "K"
            gravity = Gravity.CENTER
            textSize = 34f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(getColor(R.color.kk_surface))
            setBackgroundResource(R.drawable.bg_profile)
            layoutParams = LinearLayout.LayoutParams(dp(82), dp(82)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        root.addView(logo)

        root.addView(TextView(this).apply {
            text = "欢迎回来"
            applyTitleStyle(26f)
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(6))
        })

        root.addView(TextView(this).apply {
            text = "登录您的账号继续"
            applyBodyStyle(14f)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(32))
        })

        emailInput = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            hint = "请输入邮箱"
            setSingleLine(true)
        }
        root.addView(inputLayout("邮箱", emailInput))

        passwordInput = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "请输入密码"
            setSingleLine(true)
        }
        root.addView(inputLayout("密码", passwordInput))

        loginButton = MaterialButton(this).apply {
            text = "立即登录"
            setTextColor(getColor(R.color.kk_black))
            setBackgroundColor(getColor(R.color.kk_orange))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54),
            ).apply {
                topMargin = dp(16)
            }
            setOnClickListener { login() }
        }
        root.addView(loginButton)

        val back = MaterialButton(this).apply {
            text = "返回"
            setTextColor(getColor(R.color.kk_text_muted))
            setBackgroundColor(getColor(R.color.kk_surface))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48),
            ).apply {
                topMargin = dp(10)
            }
            setOnClickListener { finish() }
        }
        root.addView(back)

        return scroll
    }

    private fun inputLayout(label: String, editText: TextInputEditText): TextInputLayout {
        return TextInputLayout(this).apply {
            hint = label
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(dp(14).toFloat(), dp(14).toFloat(), dp(14).toFloat(), dp(14).toFloat())
            setPadding(0, 0, 0, dp(14))
            addView(editText)
        }
    }

    private fun login() {
        val email = emailInput.text?.toString().orEmpty().trim()
        val password = passwordInput.text?.toString().orEmpty()
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "请输入邮箱和密码", Toast.LENGTH_SHORT).show()
            return
        }

        loginButton.isEnabled = false
        loginButton.text = "登录中..."
        lifecycleScope.launch {
            runCatching { AppGraph.repository.login(email, password) }
                .onSuccess {
                    Toast.makeText(this@LoginActivity, "登录成功", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .onFailure {
                    Toast.makeText(
                        this@LoginActivity,
                        it.message ?: "登录失败",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            loginButton.isEnabled = true
            loginButton.text = "立即登录"
        }
    }
}
