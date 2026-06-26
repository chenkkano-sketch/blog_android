package cc.kkano.blog.ui.account

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import cc.kkano.blog.R
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyDataBox
import cc.kkano.blog.ui.common.dataBox
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.menuRow
import cc.kkano.blog.ui.common.sectionHeader

class SocialMediaActivity : AppCompatActivity() {
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
                title = "社交媒体",
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
            setPadding(0, dp(2), 0, dp(4))
        }
        box.addView(column)
        column.addView(sectionHeader("▌ 社交媒体"))
        column.addView(menuRow("QQ交流群", icon = R.drawable.ic_message) { openUrl(BASE_URL) })
        column.addView(menuRow("官网网站", icon = R.drawable.ic_link) { openUrl(BASE_URL) })
        column.addView(menuRow("Github", icon = R.drawable.ic_tools) { openUrl("https://github.com/") })
        content.addView(box)
        return root
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private companion object {
        const val BASE_URL = "https://workapi.kkano.cc/"
    }
}
