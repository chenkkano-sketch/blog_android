package cc.kkano.blog.ui.feature

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import cc.kkano.blog.R
import cc.kkano.blog.navigation.FeatureLauncher
import cc.kkano.blog.navigation.NativeRouteRegistry
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyDataBox
import cc.kkano.blog.ui.common.dataBox
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.menuRow
import cc.kkano.blog.ui.common.sectionHeader

class FeatureHubActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val section = intent.getStringExtra(EXTRA_SECTION)
        setContentView(buildContent(section))
    }

    private fun buildContent(sectionFilter: String?): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = sectionFilter ?: "全部功能",
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

        val groups = NativeRouteRegistry.grouped
            .filterKeys { sectionFilter == null || it == sectionFilter }

        groups.forEach { (section, features) ->
            val box = dataBox(marginTop = 0).apply { applyDataBox(14) }
            val column = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(2), 0, dp(4))
            }
            box.addView(column)
            column.addView(sectionHeader(section))
            features.forEach { spec ->
                column.addView(
                    menuRow(
                        title = spec.title,
                        subtitle = spec.description.ifBlank { spec.route },
                        icon = iconForSection(section),
                    ) {
                        FeatureLauncher.open(this@FeatureHubActivity, spec)
                    },
                )
            }
            content.addView(box)
        }

        return root
    }

    private fun iconForSection(section: String): Int {
        return when (section) {
            "创作" -> R.drawable.ic_write
            "内容" -> R.drawable.ic_image
            "账户" -> R.drawable.ic_account
            "管理" -> R.drawable.ic_settings
            else -> R.drawable.ic_tools
        }
    }

    companion object {
        const val EXTRA_SECTION = "extra_section"
    }
}
