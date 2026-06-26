package cc.kkano.blog.ui.login

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
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
import cc.kkano.blog.navigation.FeatureLauncher
import cc.kkano.blog.navigation.NativeRouteRegistry
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.common.setRoundedBackground
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
    }

    private fun buildContent(): ScrollView {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(30), dp(64), dp(30), dp(30))
        }
        scroll.addView(root)

        root.addView(FrameLayout(this).apply {
            background = roundedDrawable(KkColors.black, 999, KkColors.orange, 2)
            elevation = dp(8).toFloat()
            layoutParams = LinearLayout.LayoutParams(dp(70), dp(70)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            addView(ImageView(this@LoginActivity).apply {
                setImageResource(R.drawable.ic_account)
                setColorFilter(Color.WHITE)
                layoutParams = FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER)
            })
        })

        root.addView(TextView(this).apply {
            text = "欢迎回来"
            applyTitleStyle(22f)
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(7))
        })

        root.addView(TextView(this).apply {
            text = "登录您的账号继续"
            applyBodyStyle(14f)
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#999999"))
            setPadding(0, 0, 0, dp(30))
        })

        emailInput = EditText(this).apply {
            hint = "请输入邮箱"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setSingleLine(true)
        }
        root.addView(inputRow(R.drawable.ic_message, emailInput))

        passwordInput = EditText(this).apply {
            hint = "请输入密码"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }
        root.addView(inputRow(R.drawable.ic_lock, passwordInput))

        loginButton = TextView(this).apply {
            text = "立即登录"
            applyTitleStyle(16f)
            setTextColor(KkColors.black)
            gravity = Gravity.CENTER
            setRoundedBackground(KkColors.orange, 24)
            elevation = dp(8).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48),
            ).apply {
                topMargin = dp(10)
            }
            setOnClickListener { login() }
        }
        root.addView(loginButton)

        root.addView(TextView(this).apply {
            text = "忘记密码？"
            applyBodyStyle(14f)
            gravity = Gravity.CENTER
            setTextColor(KkColors.black)
            setPadding(0, dp(30), 0, 0)
            setOnClickListener {
                NativeRouteRegistry.find("pages/user/foget")?.let { FeatureLauncher.open(this@LoginActivity, it) }
            }
        })

        return scroll
    }

    private fun inputRow(icon: Int, editText: EditText): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            setRoundedBackground(Color.parseColor("#F8F9FB"), 8, Color.parseColor("#E8E8E8"), 1)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50),
            ).apply {
                bottomMargin = dp(16)
            }
        }

        val iconView = ImageView(this).apply {
            setImageResource(icon)
            setColorFilter(Color.parseColor("#BBBBBB"))
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                rightMargin = dp(8)
            }
        }
        row.addView(iconView)

        editText.apply {
            background = null
            textSize = 15f
            setTextColor(Color.parseColor("#333333"))
            setHintTextColor(Color.parseColor("#CCCCCC"))
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            setPadding(0, 0, 0, 0)
            setOnFocusChangeListener { _, focused ->
                row.setRoundedBackground(
                    if (focused) Color.parseColor("#FFF1E8") else Color.parseColor("#F8F9FB"),
                    8,
                    if (focused) KkColors.black else Color.parseColor("#E8E8E8"),
                    if (focused) 2 else 1,
                )
                iconView.setColorFilter(if (focused) KkColors.black else Color.parseColor("#BBBBBB"))
            }
        }
        row.addView(editText)
        return row
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
