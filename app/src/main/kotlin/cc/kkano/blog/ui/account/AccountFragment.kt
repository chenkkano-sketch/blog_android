package cc.kkano.blog.ui.account

import android.content.Intent
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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.data.model.User
import cc.kkano.blog.navigation.FeatureLauncher
import cc.kkano.blog.navigation.NativeRouteRegistry
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.UiState
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyDataBox
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dataBox
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.margin
import cc.kkano.blog.ui.common.menuRow
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.common.setRoundedBackground
import cc.kkano.blog.ui.login.LoginActivity
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar

class AccountFragment : Fragment() {
    private lateinit var viewModel: AccountViewModel
    private lateinit var profileCard: MaterialCardView
    private lateinit var avatar: ImageView
    private lateinit var name: TextView
    private lateinit var email: TextView
    private lateinit var role: TextView
    private lateinit var adminBox: View
    private lateinit var logoutBox: View

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
                title = "账户",
                leftIcon = null,
                rightIcon = R.drawable.ic_scan,
                onRightClick = { openRoute("pages/user/scan") },
            ),
        )

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
            setPadding(0, context.dp(12), 0, context.dp(18))
        }
        scroll.addView(content)

        profileCard = buildProfileCard()
        content.addView(profileCard)
        content.addView(buildActionGrid())
        adminBox = buildAdminBox()
        content.addView(adminBox)
        content.addView(buildMenuBox())
        logoutBox = buildLogoutBox()
        content.addView(logoutBox)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[AccountViewModel::class.java]
        viewModel.user.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Idle, UiState.Loading -> Unit
                is UiState.Success -> renderUser(state.data)
                is UiState.Error -> Snackbar.make(view, state.message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUser()
    }

    private fun buildProfileCard(): MaterialCardView {
        val context = requireContext()
        val card = MaterialCardView(context).apply {
            radius = context.dp(17).toFloat()
            cardElevation = context.dp(11).toFloat()
            setCardBackgroundColor(KkColors.black)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(context.dp(11), 0, context.dp(11), context.dp(8))
            }
            setOnClickListener { startActivity(Intent(requireContext(), LoginActivity::class.java)) }
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dp(17), context.dp(21), context.dp(17), context.dp(21))
        }
        card.addView(row)

        avatar = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dp(66), context.dp(66))
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(R.drawable.ic_account)
            setColorFilter(Color.WHITE)
            setPadding(context.dp(14), context.dp(14), context.dp(14), context.dp(14))
            background = context.roundedDrawable(Color.parseColor("#40FFFFFF"), 999, Color.parseColor("#80FFFFFF"), 2)
        }
        row.addView(avatar)

        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = context.dp(14)
            }
        }
        row.addView(textColumn)

        name = TextView(context).apply {
            text = "点击登录"
            applyTitleStyle(20f)
            setTextColor(Color.WHITE)
        }
        textColumn.addView(name)

        email = TextView(context).apply {
            text = "登录后体验更多精彩内容"
            applyBodyStyle(12.5f)
            setTextColor(Color.parseColor("#C7FFFFFF"))
            setPadding(0, context.dp(8), 0, 0)
        }
        textColumn.addView(email)

        role = TextView(context).apply {
            text = "用户"
            textSize = 11f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            includeFontPadding = false
            setPadding(context.dp(9), context.dp(4), context.dp(9), context.dp(4))
            setRoundedBackground(Color.parseColor("#24FFFFFF"), 999, Color.parseColor("#47FFFFFF"), 1)
            setPadding(context.dp(9), context.dp(4), context.dp(9), context.dp(4))
            margin(top = 9)
        }
        textColumn.addView(role)

        row.addView(TextView(context).apply {
            text = "›"
            textSize = 28f
            includeFontPadding = false
            setTextColor(Color.parseColor("#E6FFFFFF"))
        })
        return card
    }

    private fun buildActionGrid(): View {
        val context = requireContext()
        val box = context.dataBox(marginTop = 12).apply { applyDataBox(14) }
        val grid = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(context.dp(6), context.dp(6), context.dp(6), context.dp(6))
        }
        box.addView(grid)
        listOf(
            Shortcut("个人信息", R.drawable.ic_edit, "pages/user/useredit"),
            Shortcut("评论留言", R.drawable.ic_message, "pages/user/inbox"),
            Shortcut("发布动态", R.drawable.ic_add, "pages/home/dynamicsadd"),
            Shortcut("发布文章", R.drawable.ic_write, "pages/user/post"),
        ).forEach { item ->
            grid.addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(context.dp(4), 0, context.dp(4), 0)

                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    minimumHeight = context.dp(77)
                    setPadding(context.dp(4), context.dp(11), context.dp(4), context.dp(9))
                    setRoundedBackground(Color.parseColor("#F8F9FB"), 12)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { openRoute(item.route) }
                    addView(FrameLayout(context).apply {
                        background = context.roundedDrawable(KkColors.black, 12)
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
        return box
    }

    private fun buildAdminBox(): View {
        val context = requireContext()
        val box = context.dataBox(marginTop = 12).apply { applyDataBox(14) }
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, context.dp(4), 0, context.dp(4))
        }
        box.addView(column)
        column.addView(
            context.menuRow(
                title = "管理中心",
                subtitle = "仅管理员和编辑显示",
                icon = R.drawable.ic_settings,
            ) { openRoute("pages/user/manage") },
        )
        return box
    }

    private fun buildMenuBox(): View {
        val context = requireContext()
        val box = context.dataBox(marginTop = 12).apply { applyDataBox(14) }
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, context.dp(4), 0, context.dp(4))
        }
        box.addView(column)
        column.addView(context.menuRow("系统设置", icon = R.drawable.ic_settings) { openRoute("pages/user/setup") })
        column.addView(context.menuRow("社交媒体", icon = R.drawable.ic_link) { openRoute("pages/user/media") })
        column.addView(context.menuRow("关于我们", icon = R.drawable.ic_comment) { openRoute("pages/user/agreement") })
        return box
    }

    private fun buildLogoutBox(): View {
        val context = requireContext()
        return TextView(context).apply {
            text = "退出登录"
            applyTitleStyle(15f)
            gravity = Gravity.CENTER
            setTextColor(KkColors.danger)
            setRoundedBackground(Color.parseColor("#FFF5F6"), 13, Color.parseColor("#29FF4F5F"), 1)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                context.dp(46),
            ).apply {
                setMargins(context.dp(11), context.dp(12), context.dp(11), 0)
            }
            setOnClickListener { viewModel.logout() }
        }
    }

    private fun renderUser(user: User?) {
        val loggedIn = user != null
        logoutBox.isVisible = loggedIn
        profileCard.setOnClickListener {
            if (loggedIn) openRoute("pages/user/useredit") else startActivity(Intent(requireContext(), LoginActivity::class.java))
        }

        if (user == null) {
            adminBox.isVisible = false
            name.text = "点击登录"
            email.text = "登录后体验更多精彩内容"
            role.text = ""
            role.isVisible = false
            avatar.setImageResource(R.drawable.ic_account)
            avatar.setColorFilter(Color.WHITE)
            avatar.setPadding(requireContext().dp(14), requireContext().dp(14), requireContext().dp(14), requireContext().dp(14))
            Glide.with(avatar).clear(avatar)
            return
        }

        name.text = user.displayName()
        email.text = user.email.orEmpty().ifBlank { "未绑定邮箱" }
        val roleText = when (user.primaryRole()) {
            "R_SUPER", "administrator", "admin" -> "站长"
            "editor" -> "编辑"
            else -> "用户"
        }
        role.text = roleText
        role.isVisible = true
        adminBox.isVisible = roleText == "站长" || roleText == "编辑"

        val avatarUrl = AppGraph.repository.absoluteUrl(user.avatar)
        avatar.clearColorFilter()
        avatar.setPadding(0, 0, 0, 0)
        if (avatarUrl.isNotBlank()) {
            Glide.with(avatar)
                .load(avatarUrl)
                .placeholder(R.drawable.bg_avatar)
                .centerCrop()
                .into(avatar)
        } else {
            avatar.setImageResource(R.drawable.ic_account)
            avatar.setColorFilter(Color.WHITE)
            avatar.setPadding(requireContext().dp(14), requireContext().dp(14), requireContext().dp(14), requireContext().dp(14))
        }
    }

    private fun openRoute(route: String) {
        NativeRouteRegistry.find(route)?.let { FeatureLauncher.open(requireContext(), it) }
    }

    private data class Shortcut(
        val title: String,
        val icon: Int,
        val route: String,
    )
}
