package cc.kkano.blog.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.data.model.User
import cc.kkano.blog.navigation.FeatureLauncher
import cc.kkano.blog.navigation.NativeRouteRegistry
import cc.kkano.blog.ui.common.UiState
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.login.LoginActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar

class AccountFragment : Fragment() {
    private lateinit var viewModel: AccountViewModel
    private lateinit var avatar: ImageView
    private lateinit var name: TextView
    private lateinit var email: TextView
    private lateinit var role: TextView
    private lateinit var loginButton: MaterialButton
    private lateinit var logoutButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(18), context.dp(18), context.dp(18), context.dp(18))
            setBackgroundColor(context.getColor(R.color.kk_background))
        }

        val title = TextView(context).apply {
            text = "账户"
            applyTitleStyle(24f)
        }
        root.addView(title)

        val card = MaterialCardView(context).apply {
            radius = context.dp(20).toFloat()
            cardElevation = context.dp(2).toFloat()
            setCardBackgroundColor(context.getColor(R.color.kk_black))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = context.dp(16)
            }
        }
        root.addView(card)

        val profileRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(context.dp(18), context.dp(18), context.dp(18), context.dp(18))
        }
        card.addView(profileRow)

        avatar = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dp(74), context.dp(74))
            setBackgroundResource(R.drawable.bg_avatar)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        profileRow.addView(avatar)

        val profileText = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = context.dp(14)
            }
        }
        profileRow.addView(profileText)

        name = TextView(context).apply {
            text = "点击登录"
            textSize = 20f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(context.getColor(R.color.kk_surface))
        }
        profileText.addView(name)

        email = TextView(context).apply {
            text = "登录后体验更多内容"
            textSize = 13f
            setTextColor(0xCCFFFFFF.toInt())
            setPadding(0, context.dp(5), 0, 0)
        }
        profileText.addView(email)

        role = TextView(context).apply {
            text = ""
            textSize = 12f
            setTextColor(context.getColor(R.color.kk_orange))
            setPadding(0, context.dp(8), 0, 0)
        }
        profileText.addView(role)

        loginButton = MaterialButton(context).apply {
            text = "立即登录"
            setTextColor(context.getColor(R.color.kk_black))
            setBackgroundColor(context.getColor(R.color.kk_orange))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                context.dp(52),
            ).apply {
                topMargin = context.dp(18)
            }
            setOnClickListener {
                startActivity(Intent(requireContext(), LoginActivity::class.java))
            }
        }
        root.addView(loginButton)

        logoutButton = MaterialButton(context).apply {
            text = "退出登录"
            setTextColor(context.getColor(R.color.kk_error))
            setBackgroundColor(context.getColor(R.color.kk_surface))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                context.dp(52),
            ).apply {
                topMargin = context.dp(12)
            }
            setOnClickListener { viewModel.logout() }
        }
        root.addView(logoutButton)

        val shortcuts = listOf(
            "pages/user/useredit",
            "pages/user/password",
            "pages/user/inbox",
            "pages/user/manage",
        )
        shortcuts.forEach { route ->
            val spec = NativeRouteRegistry.find(route) ?: return@forEach
            root.addView(MaterialButton(context).apply {
                text = spec.title
                setTextColor(context.getColor(R.color.kk_text))
                setBackgroundColor(context.getColor(R.color.kk_surface))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    context.dp(48),
                ).apply {
                    topMargin = context.dp(10)
                }
                setOnClickListener { FeatureLauncher.open(requireContext(), spec) }
            })
        }

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

    private fun renderUser(user: User?) {
        val loggedIn = user != null
        loginButton.isVisible = !loggedIn
        logoutButton.isVisible = loggedIn

        if (user == null) {
            name.text = "点击登录"
            email.text = "登录后体验更多内容"
            role.text = ""
            Glide.with(avatar).clear(avatar)
            return
        }

        name.text = user.displayName()
        email.text = user.email.orEmpty().ifBlank { "未绑定邮箱" }
        role.text = when (user.primaryRole()) {
            "R_SUPER", "administrator", "admin" -> "站长"
            "editor" -> "编辑"
            else -> "用户"
        }
        val avatarUrl = AppGraph.repository.absoluteUrl(user.avatar)
        if (avatarUrl.isNotBlank()) {
            Glide.with(avatar)
                .load(avatarUrl)
                .placeholder(R.drawable.bg_avatar)
                .centerCrop()
                .into(avatar)
        }
    }
}
