package cc.kkano.blog.ui.tools

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import cc.kkano.blog.R
import cc.kkano.blog.navigation.FeatureLauncher
import cc.kkano.blog.navigation.NativeRouteRegistry
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyDataBox
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.roundedDrawable
import com.google.android.material.card.MaterialCardView

class ToolsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(context.kkTopBar(title = "工具"))

        val scroll = ScrollView(context).apply {
            setBackgroundColor(KkColors.background)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        }
        root.addView(scroll)

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, context.dp(13), 0, context.dp(18))
        }
        scroll.addView(content)
        content.addView(buildHero())

        listOf(
            ToolItem("密码管理", "修改和维护博客账号安全", R.drawable.ic_lock, "pages/user/password-manager"),
            ToolItem("媒体资源", "查看博客图片与媒体文件", R.drawable.ic_image, "pages/user/media"),
            ToolItem("内容搜索", "搜索文章、动态和评论", R.drawable.ic_search, "pages/contents/search"),
            ToolItem("全部功能", "查看已迁移的博客原生页面", R.drawable.ic_tools, null),
        ).forEach { item ->
            content.addView(toolCard(item))
        }

        return root
    }

    private fun buildHero(): View {
        val context = requireContext()
        val card = MaterialCardView(context).apply {
            applyDataBox(17)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(context.dp(11), 0, context.dp(11), context.dp(4))
            }
        }
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(17), context.dp(21), context.dp(17), context.dp(21))
        }
        card.addView(column)
        column.addView(TextView(context).apply {
            text = "PRIVATE CONSOLE"
            applyTitleStyle(10.5f)
            setTextColor(KkColors.orange)
        })
        column.addView(TextView(context).apply {
            text = "高频工具"
            applyTitleStyle(23f)
            setPadding(0, context.dp(9), 0, 0)
        })
        column.addView(TextView(context).apply {
            text = "只保留博客体系里每天真正会用到的入口。"
            applyBodyStyle(13f)
            setPadding(0, context.dp(7), 0, 0)
        })
        return card
    }

    private fun toolCard(item: ToolItem): View {
        val context = requireContext()
        val card = MaterialCardView(context).apply {
            applyDataBox(14)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(context.dp(11), context.dp(9), context.dp(11), 0)
            }
            setOnClickListener {
                if (item.route == null) {
                    FeatureLauncher.openHub(requireContext())
                } else {
                    NativeRouteRegistry.find(item.route)?.let { spec ->
                        FeatureLauncher.open(requireContext(), spec)
                    }
                }
            }
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = context.dp(66)
            setPadding(context.dp(12), context.dp(12), context.dp(12), context.dp(12))
        }
        card.addView(row)
        row.addView(FrameLayout(context).apply {
            background = context.roundedDrawable(KkColors.black, 13)
            elevation = context.dp(7).toFloat()
            layoutParams = LinearLayout.LayoutParams(context.dp(43), context.dp(43))
            addView(ImageView(context).apply {
                setImageResource(item.icon)
                setColorFilter(Color.WHITE)
                layoutParams = FrameLayout.LayoutParams(
                    context.dp(24),
                    context.dp(24),
                    Gravity.CENTER,
                )
            })
        })
        row.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = context.dp(12)
            }
            addView(TextView(context).apply {
                text = item.name
                applyTitleStyle(15.5f)
            })
            addView(TextView(context).apply {
                text = item.summary
                applyBodyStyle(12f)
                setPadding(0, context.dp(5), 0, 0)
            })
        })
        row.addView(TextView(context).apply {
            text = "›"
            textSize = 25f
            includeFontPadding = false
            setTextColor(Color.parseColor("#BDC1C8"))
        })
        return card
    }

    private data class ToolItem(
        val name: String,
        val summary: String,
        val icon: Int,
        val route: String?,
    )
}
