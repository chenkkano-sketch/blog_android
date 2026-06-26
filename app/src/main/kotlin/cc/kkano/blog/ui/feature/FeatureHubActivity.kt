package cc.kkano.blog.ui.feature

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cc.kkano.blog.R
import cc.kkano.blog.navigation.FeatureLauncher
import cc.kkano.blog.navigation.NativeRouteRegistry
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class FeatureHubActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val section = intent.getStringExtra(EXTRA_SECTION)
        setContentView(buildContent(section))
    }

    private fun buildContent(sectionFilter: String?): View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.kk_background))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(24))
        }
        scroll.addView(root)

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        root.addView(toolbar)
        toolbar.addView(MaterialButton(this).apply {
            text = "返回"
            setTextColor(getColor(R.color.kk_text))
            setBackgroundColor(getColor(R.color.kk_surface))
            layoutParams = LinearLayout.LayoutParams(dp(84), dp(44))
            setOnClickListener { finish() }
        })
        toolbar.addView(TextView(this).apply {
            text = sectionFilter ?: "全部功能"
            applyTitleStyle(22f)
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                leftMargin = dp(10)
            }
        })

        val groups = NativeRouteRegistry.grouped
            .filterKeys { sectionFilter == null || it == sectionFilter }

        groups.forEach { (section, features) ->
            root.addView(TextView(this).apply {
                text = section
                applyTitleStyle(18f)
                setPadding(0, dp(20), 0, dp(8))
            })
            features.forEach { spec ->
                val card = MaterialCardView(this).apply {
                    radius = dp(16).toFloat()
                    cardElevation = dp(1).toFloat()
                    strokeColor = getColor(R.color.kk_line)
                    strokeWidth = dp(1)
                    setCardBackgroundColor(getColor(R.color.kk_surface))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        bottomMargin = dp(10)
                    }
                    setOnClickListener { FeatureLauncher.open(this@FeatureHubActivity, spec) }
                }
                val column = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                }
                card.addView(column)
                column.addView(TextView(this).apply {
                    text = spec.title
                    applyTitleStyle(16f)
                })
                column.addView(TextView(this).apply {
                    text = spec.description.ifBlank { spec.route }
                    applyBodyStyle(12f)
                    setPadding(0, dp(5), 0, 0)
                })
                root.addView(card)
            }
        }

        return scroll
    }

    companion object {
        const val EXTRA_SECTION = "extra_section"
    }
}
