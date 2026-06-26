package cc.kkano.blog.ui.dynamics

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cc.kkano.blog.R
import cc.kkano.blog.navigation.FeatureLauncher
import cc.kkano.blog.navigation.NativeRouteRegistry
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.UiState
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import com.google.android.material.snackbar.Snackbar

class DynamicsFragment : Fragment() {
    private lateinit var viewModel: DynamicsViewModel
    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var adapter: DynamicAdapter
    private lateinit var emptyText: TextView

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
                title = "动态",
                leftIcon = R.drawable.ic_search,
                rightIcon = R.drawable.ic_add,
                onLeftClick = { openRoute("pages/contents/search") },
                onRightClick = { openRoute("pages/home/dynamicsadd") },
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

        val listContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, context.dp(12), 0, context.dp(12))
        }
        refreshLayout.addView(listContainer)

        emptyText = TextView(context).apply {
            text = "暂时没有数据"
            applyBodyStyle(15f)
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                context.dp(90),
            )
        }
        listContainer.addView(emptyText)

        val recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        listContainer.addView(recyclerView)

        adapter = DynamicAdapter()
        recyclerView.adapter = adapter
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[DynamicsViewModel::class.java]
        refreshLayout.setOnRefreshListener { viewModel.loadDynamics() }
        viewModel.dynamics.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Idle -> Unit
                UiState.Loading -> refreshLayout.isRefreshing = true
                is UiState.Success -> {
                    refreshLayout.isRefreshing = false
                    adapter.submitList(state.data)
                    emptyText.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                }
                is UiState.Error -> {
                    refreshLayout.isRefreshing = false
                    Snackbar.make(view, state.message, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        if (savedInstanceState == null) viewModel.loadDynamics()
    }

    private fun openRoute(route: String) {
        NativeRouteRegistry.find(route)?.let { FeatureLauncher.open(requireContext(), it) }
    }
}
