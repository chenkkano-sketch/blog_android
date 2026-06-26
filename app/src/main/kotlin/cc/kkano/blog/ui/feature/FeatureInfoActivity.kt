package cc.kkano.blog.ui.feature

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cc.kkano.blog.R
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import com.google.android.material.button.MaterialButton

class FeatureInfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
    }

    private fun buildContent(): View {
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "功能" }
        val description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()
        val route = intent.getStringExtra(EXTRA_ROUTE).orEmpty()
        val mode = intent.getStringExtra(EXTRA_MODE).orEmpty()

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundColor(getColor(R.color.kk_background))
            addView(MaterialButton(this@FeatureInfoActivity).apply {
                text = "返回"
                setTextColor(getColor(R.color.kk_text))
                setBackgroundColor(getColor(R.color.kk_surface))
                layoutParams = LinearLayout.LayoutParams(dp(84), dp(44))
                setOnClickListener { finish() }
            })
            addView(TextView(this@FeatureInfoActivity).apply {
                text = title
                applyTitleStyle(24f)
                setPadding(0, dp(22), 0, dp(8))
            })
            addView(TextView(this@FeatureInfoActivity).apply {
                text = description.ifBlank { "此页面已在原生路由中注册，等待对应后端能力或专用交互继续接入。" }
                applyBodyStyle(15f)
            })
            addView(TextView(this@FeatureInfoActivity).apply {
                text = "路由：$route\n模式：$mode"
                applyBodyStyle(12f)
                setPadding(0, dp(18), 0, 0)
            })
        }
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_ROUTE = "extra_route"
        const val EXTRA_MODE = "extra_mode"
    }
}
