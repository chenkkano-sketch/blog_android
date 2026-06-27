package cc.kkano.blog.ui.account

import android.content.Intent
import android.graphics.Color
import android.net.Uri
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
import cc.kkano.blog.BuildConfig
import cc.kkano.blog.R
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.common.setRoundedBackground
import com.google.android.material.card.MaterialCardView

class AboutActivity : AppCompatActivity() {
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
                title = "关于我们",
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
            addView(heroCard())
            addView(infoCard())
            addView(linkCard())
        })
        return root
    }

    private fun heroCard(): View {
        return MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(5).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(LinearLayout(this@AboutActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(15), dp(16), dp(15), dp(16))
                addView(FrameLayout(this@AboutActivity).apply {
                    background = roundedDrawable(KkColors.black, 999, KkColors.orange, 2)
                    elevation = dp(5).toFloat()
                    layoutParams = LinearLayout.LayoutParams(dp(58), dp(58))
                    addView(ImageView(this@AboutActivity).apply {
                        setImageResource(R.drawable.ic_account)
                        setColorFilter(Color.WHITE)
                        setPadding(dp(14), dp(14), dp(14), dp(14))
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    })
                })
                addView(LinearLayout(this@AboutActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        leftMargin = dp(13)
                    }
                    addView(TextView(this@AboutActivity).apply {
                        text = "盔盔的小世界"
                        applyTitleStyle(19f)
                        maxLines = 1
                    })
                    addView(TextView(this@AboutActivity).apply {
                        text = "blog_android ${BuildConfig.VERSION_NAME}"
                        applyBodyStyle(12.5f)
                        setPadding(0, dp(7), 0, 0)
                    })
                })
            })
        }
    }

    private fun infoCard(): View {
        return MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(4).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
            }
            addView(LinearLayout(this@AboutActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                addView(TextView(this@AboutActivity).apply {
                    text = "博客体系原生客户端"
                    applyTitleStyle(16f)
                })
                addView(TextView(this@AboutActivity).apply {
                    text = "围绕文章、动态、评论、友链、足迹、图床和后台管理重构的 Android 原生版本。"
                    applyBodyStyle(13.5f)
                    setTextColor(KkColors.text)
                    setLineSpacing(dp(4).toFloat(), 1.05f)
                    setPadding(0, dp(10), 0, 0)
                })
                addView(metaRow("邮箱", "chenkkano@foxmail.com"))
                addView(metaRow("接口", "workapi.kkano.cc"))
            })
        }
    }

    private fun linkCard(): View {
        return MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(4).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
            }
            addView(LinearLayout(this@AboutActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(4))
                addView(linkRow("官网网站", "https://workapi.kkano.cc/", R.drawable.ic_link))
                addView(linkRow("GitHub 仓库", "https://github.com/chenkkano-sketch/blog_android", R.drawable.ic_tools))
            })
        }
    }

    private fun metaRow(label: String, value: String): View {
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setRoundedBackground(Color.parseColor("#F7F8FA"), 11)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10)
            }
            addView(TextView(this@AboutActivity).apply {
                text = label
                applyTitleStyle(13f)
                layoutParams = LinearLayout.LayoutParams(dp(58), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(TextView(this@AboutActivity).apply {
                text = value
                applyBodyStyle(13f)
                setTextColor(KkColors.text)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
    }

    private fun linkRow(title: String, url: String, icon: Int): View {
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setRoundedBackground(Color.parseColor("#F7F8FA"), 12)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60)).apply {
                bottomMargin = dp(8)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { openUrl(url) }
            addView(ImageView(this@AboutActivity).apply {
                setImageResource(icon)
                setColorFilter(KkColors.black)
                layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply {
                    rightMargin = dp(10)
                }
            })
            addView(LinearLayout(this@AboutActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@AboutActivity).apply {
                    text = title
                    applyTitleStyle(14.5f)
                })
                addView(TextView(this@AboutActivity).apply {
                    text = url
                    applyBodyStyle(11.5f)
                    setPadding(0, dp(5), 0, 0)
                    maxLines = 1
                })
            })
            addView(TextView(this@AboutActivity).apply {
                text = "›"
                textSize = 24f
                setTextColor(Color.parseColor("#BDC1C8"))
                includeFontPadding = false
            })
        }
    }

    private fun openUrl(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(this, "无法打开链接", Toast.LENGTH_SHORT).show()
        }
    }
}
