package cc.kkano.blog.ui.account

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cc.kkano.blog.R
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.setRoundedBackground
import com.google.android.material.card.MaterialCardView

class AgreementActivity : AppCompatActivity() {
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
                title = "用户协议",
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
            addView(introCard())
            addView(sectionCard("服务说明", "本应用用于浏览、发布和管理博客内容。登录后可使用文章、动态、评论、友链、图床等与账号权限匹配的功能。"))
            addView(sectionCard("账号安全", "请妥善保管登录凭证、邮箱验证码和扫码授权。发现异常登录设备时，可在账号绑定页及时解绑。"))
            addView(sectionCard("内容规范", "发布文章、动态、评论和友链时，应保持内容真实、友好，不上传侵犯他人权益、破坏服务安全或违反法律法规的内容。"))
            addView(sectionCard("隐私说明", "应用会按接口需要读取账号资料、媒体资源、评论和管理数据；本机仅保存登录状态、必要缓存和用户主动选择的本地资源目录。"))
        })
        return root
    }

    private fun introCard(): View {
        return MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(5).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(LinearLayout(this@AgreementActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
                addView(TextView(this@AgreementActivity).apply {
                    text = "blog_android"
                    applyTitleStyle(20f)
                })
                addView(TextView(this@AgreementActivity).apply {
                    text = "博客体系原生客户端使用约定"
                    applyBodyStyle(13f)
                    setPadding(0, dp(8), 0, 0)
                })
            })
        }
    }

    private fun sectionCard(title: String, body: String): View {
        return MaterialCardView(this).apply {
            radius = dp(15).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10)
            }
            addView(LinearLayout(this@AgreementActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                addView(TextView(this@AgreementActivity).apply {
                    text = title
                    applyTitleStyle(16f)
                })
                addView(TextView(this@AgreementActivity).apply {
                    text = body
                    applyBodyStyle(13.5f)
                    setTextColor(KkColors.text)
                    setLineSpacing(dp(4).toFloat(), 1.05f)
                    setPadding(0, dp(10), 0, 0)
                })
            })
        }
    }
}
