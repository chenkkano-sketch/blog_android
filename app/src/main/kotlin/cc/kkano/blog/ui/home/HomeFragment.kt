package cc.kkano.blog.ui.home

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.core.widget.NestedScrollView
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.data.model.Article
import cc.kkano.blog.navigation.FeatureLauncher
import cc.kkano.blog.navigation.NativeRouteRegistry
import cc.kkano.blog.ui.article.ArticleDetailActivity
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.UiState
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyDataBox
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dataBox
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.gradientDrawable
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.common.sectionHeader
import cc.kkano.blog.ui.common.setRoundedBackground
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment() {
    private lateinit var viewModel: HomeViewModel
    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var adapter: ArticleAdapter
    private lateinit var emptyText: TextView
    private lateinit var heroStrip: LinearLayout

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

        root.addView(
            context.kkTopBar(
                title = "首页",
                leftIcon = R.drawable.ic_search,
                rightIcon = R.drawable.ic_add,
                onLeftClick = { openRoute("pages/contents/search") },
                onRightClick = { openRoute("pages/user/post") },
            ),
        )

        refreshLayout = SwipeRefreshLayout(context).apply {
            setColorSchemeResources(R.color.kk_orange, R.color.kk_black)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        }
        root.addView(refreshLayout)

        val scroller = NestedScrollView(context).apply {
            isFillViewport = false
            setBackgroundColor(KkColors.background)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        refreshLayout.addView(scroller)

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, context.dp(11), 0, context.dp(18))
        }
        scroller.addView(content)

        heroStrip = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(context.dp(11), 0, context.dp(11), 0)
        }
        content.addView(HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(heroStrip)
        })

        content.addView(buildShortcutGrid())

        val box = context.dataBox(marginTop = 12).apply {
            applyDataBox(14)
        }
        val boxColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        box.addView(boxColumn)
        boxColumn.addView(
            context.sectionHeader("▌ 推荐文章", "阅读更多 ›") {
                openRoute("pages/contents/recommend")
            },
        )

        emptyText = TextView(context).apply {
            text = "暂时没有数据"
            applyBodyStyle(14f)
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                context.dp(72),
            )
        }
        boxColumn.addView(emptyText)

        val recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            isNestedScrollingEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        boxColumn.addView(recyclerView)
        content.addView(box)

        adapter = ArticleAdapter { article ->
            startActivity(
                Intent(requireContext(), ArticleDetailActivity::class.java)
                    .putExtra(ArticleDetailActivity.EXTRA_ARTICLE_ID, article.id),
            )
        }
        recyclerView.adapter = adapter

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        refreshLayout.setOnRefreshListener { viewModel.loadArticles() }
        viewModel.articles.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Idle -> Unit
                UiState.Loading -> refreshLayout.isRefreshing = true
                is UiState.Success -> {
                    refreshLayout.isRefreshing = false
                    renderArticles(state.data)
                }
                is UiState.Error -> {
                    refreshLayout.isRefreshing = false
                    Snackbar.make(view, state.message, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        if (savedInstanceState == null) viewModel.loadArticles()
    }

    private fun renderArticles(articles: List<Article>) {
        buildHero(articles.take(4))
        adapter.submitList(articles)
        emptyText.visibility = if (articles.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun buildHero(articles: List<Article>) {
        val context = requireContext()
        heroStrip.removeAllViews()
        val heroItems = articles.ifEmpty {
            listOf(Article(title = "欢迎回来", summary = "博客内容正在加载中"))
        }
        heroItems.forEach { article ->
            val width = context.resources.displayMetrics.widthPixels - context.dp(44)
            val card = MaterialCardView(context).apply {
                radius = context.dp(17).toFloat()
                cardElevation = context.dp(9).toFloat()
                setCardBackgroundColor(Color.parseColor("#171A20"))
                layoutParams = LinearLayout.LayoutParams(width, context.dp(185)).apply {
                    rightMargin = context.dp(11)
                }
                setOnClickListener {
                    if (article.id > 0) {
                        startActivity(
                            Intent(requireContext(), ArticleDetailActivity::class.java)
                                .putExtra(ArticleDetailActivity.EXTRA_ARTICLE_ID, article.id),
                        )
                    }
                }
            }
            val frame = FrameLayout(context)
            card.addView(frame)

            val coverUrl = AppGraph.repository.absoluteUrl(article.coverPath())
            frame.addView(ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundResource(R.drawable.bg_image_placeholder)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                if (coverUrl.isNotBlank()) {
                    Glide.with(this)
                        .load(coverUrl)
                        .placeholder(R.drawable.bg_image_placeholder)
                        .centerCrop()
                        .into(this)
                }
            })

            frame.addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM
                setPadding(context.dp(21), 0, context.dp(21), context.dp(22))
                background = context.gradientDrawable(
                    intArrayOf(Color.parseColor("#110C0E12"), Color.parseColor("#CC0C0E12")),
                    17,
                    GradientDrawable.Orientation.TOP_BOTTOM,
                )
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                addView(TextView(context).apply {
                    text = article.title.ifBlank { "未命名文章" }
                    applyTitleStyle(19f)
                    setTextColor(Color.WHITE)
                    maxLines = 2
                    setLineSpacing(context.dp(3).toFloat(), 1f)
                })
                addView(TextView(context).apply {
                    text = article.summary?.takeIf { it.isNotBlank() } ?: "暂无摘要"
                    applyBodyStyle(12.5f)
                    setTextColor(Color.parseColor("#CFFFFFFF"))
                    maxLines = 1
                    setPadding(0, context.dp(8), 0, 0)
                })
            })
            heroStrip.addView(card)
        }
    }

    private fun buildShortcutGrid(): View {
        val context = requireContext()
        val grid = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(context.dp(5), context.dp(10), context.dp(5), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        listOf(
            Shortcut("图床管理", R.drawable.ic_image, Color.parseColor("#FF007F"), "pages/manage/media"),
            Shortcut("评论留言", R.drawable.ic_comment, KkColors.black, "pages/contents/comments"),
            Shortcut("友链管理", R.drawable.ic_link, Color.parseColor("#FF69B4"), "pages/user/friendlink-manage"),
            Shortcut("足迹管理", R.drawable.ic_location, Color.parseColor("#36CFC9"), "pages/user/footprint-manage"),
        ).forEach { item ->
            grid.addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(context.dp(4), 0, context.dp(4), 0)

                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    minimumHeight = context.dp(74)
                    setPadding(context.dp(4), context.dp(11), context.dp(4), context.dp(9))
                    setRoundedBackground(KkColors.soft, 12)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { openRoute(item.route) }

                    addView(FrameLayout(context).apply {
                        background = context.roundedDrawable(item.color, 12)
                        elevation = context.dp(7).toFloat()
                        layoutParams = LinearLayout.LayoutParams(context.dp(41), context.dp(41))
                        addView(ImageView(context).apply {
                            setImageResource(item.icon)
                            setColorFilter(Color.WHITE)
                            layoutParams = FrameLayout.LayoutParams(
                                context.dp(23),
                                context.dp(23),
                                Gravity.CENTER,
                            )
                        })
                    })
                    addView(TextView(context).apply {
                        text = item.title
                        applyTitleStyle(12.5f)
                        gravity = Gravity.CENTER
                        maxLines = 1
                        setPadding(0, context.dp(7), 0, 0)
                    })
                })
            })
        }
        return grid
    }

    private fun openRoute(route: String) {
        NativeRouteRegistry.find(route)?.let { FeatureLauncher.open(requireContext(), it) }
    }

    private data class Shortcut(
        val title: String,
        val icon: Int,
        val color: Int,
        val route: String,
    )
}
