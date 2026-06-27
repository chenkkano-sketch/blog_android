package cc.kkano.blog.ui.comments

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import cc.kkano.blog.ui.common.setRoundedBackground
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class CommentComposerActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private var type = 2
    private var targetId = 0L
    private lateinit var contentInput: EditText
    private lateinit var nicknameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var submitButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = intent.getIntExtra(EXTRA_TYPE, 2)
        targetId = intent.getLongExtra(EXTRA_TARGET_ID, 0L)
        setContentView(buildContent())
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { titleForType() },
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
            setPadding(dp(12), dp(14), dp(12), dp(24))
            addView(headerCard())
            addView(formCard())
        })
        return root
    }

    private fun headerCard(): View {
        return MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(5).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(LinearLayout(this@CommentComposerActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                addView(ImageView(this@CommentComposerActivity).apply {
                    setImageResource(R.drawable.ic_comment)
                    setColorFilter(KkColors.orange)
                    setRoundedBackground(Color.parseColor("#FFF1E8"), 999)
                    setPadding(dp(13), dp(13), dp(13), dp(13))
                    layoutParams = LinearLayout.LayoutParams(dp(52), dp(52)).apply {
                        rightMargin = dp(12)
                    }
                })
                addView(LinearLayout(this@CommentComposerActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    addView(TextView(this@CommentComposerActivity).apply {
                        text = titleForType()
                        applyTitleStyle(17f)
                    })
                    addView(TextView(this@CommentComposerActivity).apply {
                        text = repository.cachedUser()?.displayName().orEmpty().ifBlank { "匿名评论" }
                        applyBodyStyle(12.5f)
                        setPadding(0, dp(6), 0, 0)
                    })
                })
            })
        }
    }

    private fun formCard(): View {
        return MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(4).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
            }
            addView(LinearLayout(this@CommentComposerActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(13), dp(13), dp(13), dp(14))
                contentInput = EditText(this@CommentComposerActivity).apply {
                    hint = "写下你的评论"
                    minLines = 6
                    maxLines = 9
                    gravity = Gravity.TOP
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    textSize = 15f
                    setTextColor(KkColors.text)
                    setHintTextColor(KkColors.softMuted)
                    setRoundedBackground(Color.parseColor("#F7F8FA"), 13, KkColors.line, 1)
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                }
                addView(contentInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(158)))
                if (repository.cachedUser() == null) {
                    nicknameInput = smallInput("昵称")
                    emailInput = smallInput("邮箱")
                    addView(nicknameInput, rowParams())
                    addView(emailInput, rowParams())
                } else {
                    nicknameInput = EditText(this@CommentComposerActivity)
                    emailInput = EditText(this@CommentComposerActivity)
                }
                submitButton = TextView(this@CommentComposerActivity).apply {
                    text = "发布评论"
                    applyTitleStyle(15.5f)
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    setRoundedBackground(KkColors.orange, 999)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply {
                        topMargin = dp(14)
                    }
                    setOnClickListener { submit() }
                }
                addView(submitButton)
            })
        }
    }

    private fun smallInput(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText
            setSingleLine(true)
            textSize = 14f
            background = null
            setTextColor(KkColors.text)
            setHintTextColor(KkColors.softMuted)
            setRoundedBackground(Color.parseColor("#F7F8FA"), 12, KkColors.line, 1)
            setPadding(dp(12), 0, dp(12), 0)
            if (hintText == "邮箱") inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
    }

    private fun rowParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply {
            topMargin = dp(10)
        }
    }

    private fun submit() {
        val content = contentInput.text?.toString().orEmpty().trim()
        if (content.isBlank()) {
            Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show()
            return
        }
        submitButton.isEnabled = false
        submitButton.text = "发布中..."
        lifecycleScope.launch {
            runCatching {
                repository.createComment(
                    buildMap {
                        put("type", type)
                        put("content", content)
                        if (targetId > 0L) put("target_id", targetId)
                        val nickname = nicknameInput.text?.toString().orEmpty().trim()
                        val email = emailInput.text?.toString().orEmpty().trim()
                        if (nickname.isNotBlank()) put("nickname", nickname)
                        if (email.isNotBlank()) put("email", email)
                    },
                )
            }.onSuccess {
                Toast.makeText(this@CommentComposerActivity, "评论已发布", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure {
                Toast.makeText(this@CommentComposerActivity, it.message ?: "评论发布失败", Toast.LENGTH_SHORT).show()
            }
            submitButton.isEnabled = true
            submitButton.text = "发布评论"
        }
    }

    private fun titleForType(): String {
        return when (type) {
            1 -> "评论文章"
            3 -> "评论动态"
            else -> "留言板"
        }
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_TARGET_ID = "extra_target_id"
    }
}
