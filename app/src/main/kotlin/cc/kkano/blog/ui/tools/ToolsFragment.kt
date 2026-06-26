package cc.kkano.blog.ui.tools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import cc.kkano.blog.R
import cc.kkano.blog.navigation.FeatureLauncher
import cc.kkano.blog.navigation.NativeRouteRegistry
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class ToolsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val context = requireContext()
        val scroll = ScrollView(context).apply {
            setBackgroundColor(context.getColor(R.color.kk_background))
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(18), context.dp(18), context.dp(18), context.dp(18))
        }
        scroll.addView(root)

        val title = TextView(context).apply {
            text = "工具"
            applyTitleStyle(24f)
        }
        root.addView(title)

        root.addView(MaterialButton(context).apply {
            text = "打开全部原生页面"
            setTextColor(context.getColor(R.color.kk_black))
            setBackgroundColor(context.getColor(R.color.kk_orange))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                context.dp(52),
            ).apply {
                topMargin = context.dp(14)
            }
            setOnClickListener { FeatureLauncher.openHub(requireContext()) }
        })

        listOf("创作", "内容", "账户", "管理").forEach { section ->
            root.addView(toolCard(section, "${NativeRouteRegistry.grouped[section]?.size ?: 0} 个页面") {
                FeatureLauncher.openHub(requireContext(), section)
            })
        }

        return scroll
    }

    private fun toolCard(name: String, desc: String, onClick: () -> Unit): View {
        val context = requireContext()
        val card = MaterialCardView(context).apply {
            radius = context.dp(18).toFloat()
            cardElevation = context.dp(1).toFloat()
            setCardBackgroundColor(context.getColor(R.color.kk_surface))
            strokeColor = context.getColor(R.color.kk_line)
            strokeWidth = context.dp(1)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = context.dp(12)
            }
            setOnClickListener { onClick() }
        }
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(16), context.dp(14), context.dp(16), context.dp(14))
        }
        card.addView(column)
        column.addView(TextView(context).apply {
            text = name
            applyTitleStyle(17f)
        })
        column.addView(TextView(context).apply {
            text = desc
            applyBodyStyle(13f)
            setPadding(0, context.dp(6), 0, 0)
        })
        return card
    }
}
