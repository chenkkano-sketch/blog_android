package cc.kkano.blog.ui.manage

import android.graphics.Color
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
import androidx.lifecycle.lifecycleScope
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.data.api.ApiRoutes
import cc.kkano.blog.navigation.FeatureLauncher
import cc.kkano.blog.navigation.NativeRouteRegistry
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.common.setRoundedBackground
import com.google.android.material.card.MaterialCardView
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class ManageDashboardActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val stats = mutableListOf(
        DashboardStat("文章", "同步中", "待审与已发布", R.drawable.ic_write, Color.parseColor("#2F80ED")),
        DashboardStat("评论", "同步中", "文章/留言/动态", R.drawable.ic_comment, KkColors.orange),
        DashboardStat("图床", "同步中", "图片资源", R.drawable.ic_image, Color.parseColor("#07A66A")),
        DashboardStat("用户", "同步中", "账号资料", R.drawable.ic_account, Color.parseColor("#8E5CF7")),
    )

    private lateinit var statsGrid: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        loadStats()
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = "管理中心",
                leftIcon = R.drawable.ic_back,
                rightIcon = R.drawable.ic_search,
                onLeftClick = { finish() },
                onRightClick = { openRoute("pages/contents/search") },
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
            statsGrid = LinearLayout(this@ManageDashboardActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(12)
                }
            }
            addView(statsGrid)
            renderStats()
            addView(actionSection("内容", listOf(
                DashboardAction("文章管理", "审核、推荐、置顶、编辑与删除", R.drawable.ic_write, "pages/manage/contents"),
                DashboardAction("分类标签", "分类、标签、新增、编辑与删除", R.drawable.ic_tools, "pages/manage/metas"),
                DashboardAction("动态媒体", "动态内容、图片、视频和外链", R.drawable.ic_dynamic, "pages/contents/video"),
            )))
            addView(actionSection("互动", listOf(
                DashboardAction("评论管理", "文章评论、留言板、动态评论", R.drawable.ic_comment, "pages/manage/comments"),
                DashboardAction("友链管理", "审核、通过、拒绝和编辑友链", R.drawable.ic_link, "pages/user/friendlink-manage"),
                DashboardAction("足迹管理", "发布、草稿、新增、编辑和删除", R.drawable.ic_location, "pages/user/footprint-manage"),
            )))
            addView(actionSection("系统", listOf(
                DashboardAction("图床管理", "统计、搜索、上传、编辑、批量删除", R.drawable.ic_image, "pages/manage/media"),
                DashboardAction("用户管理", "搜索、资料编辑、权限和删除", R.drawable.ic_account, "pages/manage/users"),
                DashboardAction("操作日志", "搜索、级别筛选和详情查看", R.drawable.ic_settings, "pages/manage/clean"),
            )))
        })
        return root
    }

    private fun heroCard(): View {
        val user = repository.cachedUser()
        return MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(5).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(LinearLayout(this@ManageDashboardActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                addView(FrameLayout(this@ManageDashboardActivity).apply {
                    background = roundedDrawable(KkColors.black, 999, KkColors.orange, 2)
                    elevation = dp(4).toFloat()
                    layoutParams = LinearLayout.LayoutParams(dp(54), dp(54))
                    addView(ImageView(this@ManageDashboardActivity).apply {
                        setImageResource(R.drawable.ic_settings)
                        setColorFilter(Color.WHITE)
                        setPadding(dp(13), dp(13), dp(13), dp(13))
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    })
                })
                addView(LinearLayout(this@ManageDashboardActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        leftMargin = dp(12)
                    }
                    addView(TextView(this@ManageDashboardActivity).apply {
                        text = "站点后台"
                        applyTitleStyle(18f)
                    })
                    addView(TextView(this@ManageDashboardActivity).apply {
                        text = listOf(
                            user?.displayName().orEmpty(),
                            user?.primaryRole().orEmpty(),
                        ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "博客管理" }
                        applyBodyStyle(12.5f)
                        setPadding(0, dp(6), 0, 0)
                    })
                })
                addView(TextView(this@ManageDashboardActivity).apply {
                    text = "管理"
                    applyTitleStyle(11.5f)
                    setTextColor(KkColors.orange)
                    gravity = Gravity.CENTER
                    setRoundedBackground(Color.parseColor("#FFF1E8"), 999)
                    setPadding(dp(10), dp(5), dp(10), dp(5))
                })
            })
        }
    }

    private fun renderStats() {
        if (!::statsGrid.isInitialized) return
        statsGrid.removeAllViews()
        stats.chunked(2).forEach { rowItems ->
            statsGrid.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                rowItems.forEach { stat ->
                    addView(statCard(stat), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(dp(4), dp(4), dp(4), dp(6))
                    })
                }
            })
        }
    }

    private fun statCard(stat: DashboardStat): View {
        return MaterialCardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            addView(LinearLayout(this@ManageDashboardActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                addView(ImageView(this@ManageDashboardActivity).apply {
                    setImageResource(stat.icon)
                    setColorFilter(stat.color)
                    background = roundedDrawable(lightColor(stat.color), 999)
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
                })
                addView(TextView(this@ManageDashboardActivity).apply {
                    text = stat.value
                    applyTitleStyle(20f)
                    setPadding(0, dp(12), 0, 0)
                    maxLines = 1
                })
                addView(TextView(this@ManageDashboardActivity).apply {
                    text = stat.label
                    applyTitleStyle(13f)
                    setPadding(0, dp(7), 0, 0)
                })
                addView(TextView(this@ManageDashboardActivity).apply {
                    text = stat.subtitle
                    applyBodyStyle(11.5f)
                    setPadding(0, dp(5), 0, 0)
                    maxLines = 1
                })
            })
        }
    }

    private fun actionSection(title: String, actions: List<DashboardAction>): View {
        return MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(4).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#0F15171C")
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10)
            }
            addView(LinearLayout(this@ManageDashboardActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(4))
                addView(TextView(this@ManageDashboardActivity).apply {
                    text = title
                    applyTitleStyle(16f)
                    setPadding(dp(4), dp(2), dp(4), dp(10))
                })
                actions.forEach { action -> addView(actionRow(action)) }
            })
        }
    }

    private fun actionRow(action: DashboardAction): View {
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setRoundedBackground(Color.parseColor("#F7F8FA"), 12)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(62)).apply {
                bottomMargin = dp(8)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { openRoute(action.route) }
            addView(ImageView(this@ManageDashboardActivity).apply {
                setImageResource(action.icon)
                setColorFilter(KkColors.black)
                layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply {
                    rightMargin = dp(10)
                }
            })
            addView(LinearLayout(this@ManageDashboardActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@ManageDashboardActivity).apply {
                    text = action.title
                    applyTitleStyle(14.5f)
                })
                addView(TextView(this@ManageDashboardActivity).apply {
                    text = action.subtitle
                    applyBodyStyle(11.5f)
                    setPadding(0, dp(5), 0, 0)
                    maxLines = 1
                })
            })
            addView(TextView(this@ManageDashboardActivity).apply {
                text = "›"
                textSize = 24f
                setTextColor(Color.parseColor("#BDC1C8"))
                includeFontPadding = false
            })
        }
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val overview = runCatching { repository.dashboardOverview() }.getOrNull()
            val overviewData = dataObject(overview)
            stats[0] = stats[0].copy(value = overviewValue(overviewData, "articles", "article_count", "contents")
                .ifBlank { totalFrom(runCatching { repository.adminArticlesRoot("", limit = 1) }.getOrNull()) }
                .ifBlank { "同步" })
            val articleComments = totalFrom(runCatching { repository.commentsRoot(1, limit = 1) }.getOrNull()).toIntOrNull() ?: 0
            val boardComments = totalFrom(runCatching { repository.commentsRoot(2, limit = 1) }.getOrNull()).toIntOrNull() ?: 0
            val dynamicComments = totalFrom(runCatching { repository.commentsRoot(3, limit = 1) }.getOrNull()).toIntOrNull() ?: 0
            stats[1] = stats[1].copy(value = overviewValue(overviewData, "comments", "comment_count")
                .ifBlank { (articleComments + boardComments + dynamicComments).takeIf { it > 0 }?.toString().orEmpty() }
                .ifBlank { "同步" })
            val mediaStats = runCatching { repository.mediaStatistics() }.getOrNull()
            stats[2] = stats[2].copy(value = overviewValue(overviewData, "media", "media_count")
                .ifBlank { overviewValue(mediaStats, "total_count", "count", "total") }
                .ifBlank { "同步" })
            stats[3] = stats[3].copy(value = overviewValue(overviewData, "users", "user_count")
                .ifBlank { totalFrom(runCatching { repository.genericRoot(ApiRoutes.USERS, limit = 1) }.getOrNull()) }
                .ifBlank { "同步" })
            renderStats()
        }
    }

    private fun dataObject(root: JsonObject?): JsonObject? {
        val data = root?.get("data")
        return when {
            data != null && data.isJsonObject -> data.asJsonObject
            root != null -> root
            else -> null
        }
    }

    private fun totalFrom(root: JsonObject?): String {
        val data = dataObject(root) ?: return ""
        overviewValue(data, "total", "count", "total_count").takeIf { it.isNotBlank() }?.let { return it }
        val pagination = data["pagination"]?.takeIf { it.isJsonObject }?.asJsonObject
        return overviewValue(pagination, "total", "count", "total_count")
    }

    private fun overviewValue(data: JsonObject?, vararg keys: String): String {
        if (data == null) return ""
        keys.forEach { key ->
            val value = repository.displayValue(data[key])
            if (value.isNotBlank()) return value
        }
        return ""
    }

    private fun openRoute(route: String) {
        val spec = NativeRouteRegistry.find(route)
        if (spec == null) {
            Toast.makeText(this, "入口暂不可用", Toast.LENGTH_SHORT).show()
            return
        }
        FeatureLauncher.open(this, spec)
    }

    private fun lightColor(color: Int): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.rgb(
            (red + (255 - red) * 0.88f).toInt(),
            (green + (255 - green) * 0.88f).toInt(),
            (blue + (255 - blue) * 0.88f).toInt(),
        )
    }

    private data class DashboardStat(
        val label: String,
        val value: String,
        val subtitle: String,
        val icon: Int,
        val color: Int,
    )

    private data class DashboardAction(
        val title: String,
        val subtitle: String,
        val icon: Int,
        val route: String,
    )
}
