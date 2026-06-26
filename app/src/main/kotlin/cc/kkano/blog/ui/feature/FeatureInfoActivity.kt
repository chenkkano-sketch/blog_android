package cc.kkano.blog.ui.feature

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
import cc.kkano.blog.ui.common.applyDataBox
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dataBox
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar

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

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = title,
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
            setPadding(0, dp(12), 0, dp(18))
        }
        scroll.addView(content)

        val box = dataBox(marginTop = 0).apply { applyDataBox(14) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(16), dp(15), dp(18))
        }
        box.addView(column)
        column.addView(TextView(this).apply {
            text = title
            applyTitleStyle(22f)
        })
        column.addView(TextView(this).apply {
            text = description.ifBlank { "此页面已在原生路由中注册，等待对应后端能力或专用交互继续接入。" }
            applyBodyStyle(15f)
            setPadding(0, dp(12), 0, 0)
        })
        column.addView(TextView(this).apply {
            text = "路由：$route\n模式：$mode"
            applyBodyStyle(12f)
            setPadding(0, dp(18), 0, 0)
        })
        content.addView(box)
        return root
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_ROUTE = "extra_route"
        const val EXTRA_MODE = "extra_mode"
    }
}
