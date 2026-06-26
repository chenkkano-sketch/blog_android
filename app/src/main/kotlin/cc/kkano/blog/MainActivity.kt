package cc.kkano.blog

import android.os.Bundle
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import cc.kkano.blog.navigation.FeatureLauncher
import cc.kkano.blog.navigation.NativeRouteRegistry
import cc.kkano.blog.ui.account.AccountFragment
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.dynamics.DynamicsFragment
import cc.kkano.blog.ui.home.HomeFragment
import cc.kkano.blog.ui.login.QrConfirmActivity
import cc.kkano.blog.ui.tools.ToolsFragment

class MainActivity : AppCompatActivity() {
    private lateinit var bottomTabbar: LinearLayout
    private val tabViews = mutableListOf<TabHolder>()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomTabbar = findViewById(R.id.bottomTabbar)
        buildBottomTabbar()

        if (savedInstanceState == null) {
            show(HomeFragment(), 0)
            handleDeepLink(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun buildBottomTabbar() {
        val tabs = listOf(
            TabConfig("首页", R.drawable.ic_home) { show(HomeFragment(), 0) },
            TabConfig("动态", R.drawable.ic_dynamic) { show(DynamicsFragment(), 1) },
            TabConfig("", R.drawable.ic_add, publish = true) {
                NativeRouteRegistry.find("pages/home/dynamicsadd")?.let {
                    FeatureLauncher.open(this, it)
                }
            },
            TabConfig("工具", R.drawable.ic_tools) { show(ToolsFragment(), 3) },
            TabConfig("我的", R.drawable.ic_account) { show(AccountFragment(), 4) },
        )

        bottomTabbar.removeAllViews()
        tabs.forEachIndexed { index, config ->
            val item = createTab(config)
            item.root.setOnClickListener { config.onClick() }
            bottomTabbar.addView(item.root)
            if (!config.publish) {
                tabViews.add(item.copy(index = index))
            }
        }
        updateTabState(0)
    }

    private fun createTab(config: TabConfig): TabHolder {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            setPadding(dp(3), dp(3), dp(3), dp(3))
        }

        val iconWrapSize = if (config.publish) 46 else 34
        val iconSize = if (config.publish) 28 else 23
        val iconWrap = FrameLayout(this).apply {
            background = if (config.publish) {
                roundedDrawable(KkColors.orange, 13)
            } else {
                roundedDrawable(0x00000000, 10)
            }
            elevation = if (config.publish) dp(8).toFloat() else 0f
            layoutParams = LinearLayout.LayoutParams(dp(iconWrapSize), dp(iconWrapSize))
        }
        val icon = ImageView(this).apply {
            setImageResource(config.icon)
            setColorFilter(if (config.publish) KkColors.surface else KkColors.softMuted)
            layoutParams = FrameLayout.LayoutParams(dp(iconSize), dp(iconSize), Gravity.CENTER)
        }
        iconWrap.addView(icon)
        root.addView(iconWrap)

        val label = TextView(this).apply {
            text = config.label
            textSize = 11f
            includeFontPadding = false
            gravity = Gravity.CENTER
            setTextColor(KkColors.softMuted)
            visibility = if (config.publish) View.GONE else View.VISIBLE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(3)
            }
        }
        root.addView(label)
        return TabHolder(-1, root, iconWrap, icon, label)
    }

    private fun show(fragment: Fragment, index: Int): Boolean {
        currentIndex = index
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        updateTabState(index)
        return true
    }

    private fun updateTabState(index: Int) {
        tabViews.forEach { holder ->
            val active = holder.index == index
            holder.root.background = if (active) roundedDrawable(KkColors.soft, 11) else null
            holder.iconWrap.background = roundedDrawable(
                if (active) KkColors.black else 0x00000000,
                10,
            )
            holder.iconWrap.elevation = if (active) dp(5).toFloat() else 0f
            holder.icon.setColorFilter(if (active) KkColors.surface else KkColors.softMuted)
            holder.label.setTextColor(if (active) KkColors.black else KkColors.softMuted)
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "blogandroid") return
        val sceneId = uri.getQueryParameter("scene_id")
            ?: uri.getQueryParameter("sceneId")
            ?: uri.getQueryParameter("scan")
            ?: uri.lastPathSegment.orEmpty().takeIf { it.isNotBlank() && it != "scan" }
        startActivity(
            Intent(this, QrConfirmActivity::class.java)
                .putExtra(QrConfirmActivity.EXTRA_SCENE_ID, sceneId.orEmpty()),
        )
    }

    private data class TabConfig(
        val label: String,
        val icon: Int,
        val publish: Boolean = false,
        val onClick: () -> Unit,
    )

    private data class TabHolder(
        val index: Int,
        val root: LinearLayout,
        val iconWrap: FrameLayout,
        val icon: ImageView,
        val label: TextView,
    )
}
