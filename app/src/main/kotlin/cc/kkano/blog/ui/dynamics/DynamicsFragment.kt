package cc.kkano.blog.ui.dynamics

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cc.kkano.blog.R
import cc.kkano.blog.AppGraph
import cc.kkano.blog.data.api.ApiRoutes
import cc.kkano.blog.data.model.Dynamic
import cc.kkano.blog.navigation.FeatureLauncher
import cc.kkano.blog.navigation.NativeRouteRegistry
import cc.kkano.blog.ui.comments.CommentComposerActivity
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.UiState
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.login.LoginActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

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

        adapter = DynamicAdapter(
            onEdit = { dynamic -> openDynamicEditor(dynamic) },
            onDelete = { dynamic -> confirmDelete(dynamic) },
            onLike = { dynamic -> likeDynamic(dynamic) },
            onComment = { dynamic -> openDynamicComment(dynamic) },
        )
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
                    adapter.setCurrentUserId(AppGraph.repository.cachedUser()?.id ?: 0L)
                    adapter.submitList(state.data)
                    emptyText.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                }
                is UiState.Error -> {
                    refreshLayout.isRefreshing = false
                    Snackbar.make(view, state.message, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        if (savedInstanceState == null) {
            viewModel.loadDynamics()
            loadEmojiMap()
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.setCurrentUserId(AppGraph.repository.cachedUser()?.id ?: 0L)
    }

    private fun openRoute(route: String) {
        NativeRouteRegistry.find(route)?.let { FeatureLauncher.open(requireContext(), it) }
    }

    private fun openDynamicEditor(dynamic: Dynamic) {
        startActivity(
            Intent(requireContext(), DynamicEditorActivity::class.java)
                .putExtra(DynamicEditorActivity.EXTRA_DYNAMIC_ID, dynamic.id),
        )
    }

    private fun confirmDelete(dynamic: Dynamic) {
        AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("确定要删除这条动态吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    runCatching { AppGraph.repository.deleteDynamic(dynamic.id) }
                        .onSuccess {
                            Snackbar.make(requireView(), "删除成功", Snackbar.LENGTH_SHORT).show()
                            viewModel.loadDynamics()
                        }
                        .onFailure {
                            Snackbar.make(requireView(), it.message ?: "删除失败", Snackbar.LENGTH_SHORT).show()
                        }
                }
            }
            .show()
    }

    private fun likeDynamic(dynamic: Dynamic) {
        if (AppGraph.repository.cachedUser() == null) {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            return
        }
        lifecycleScope.launch {
            runCatching { AppGraph.repository.likeDynamic(dynamic.id) }
                .onSuccess { viewModel.loadDynamics() }
                .onFailure { Snackbar.make(requireView(), it.message ?: "点赞失败", Snackbar.LENGTH_SHORT).show() }
        }
    }

    private fun openDynamicComment(dynamic: Dynamic) {
        startActivity(
            Intent(requireContext(), CommentComposerActivity::class.java)
                .putExtra(CommentComposerActivity.EXTRA_TITLE, "评论动态")
                .putExtra(CommentComposerActivity.EXTRA_TYPE, 3)
                .putExtra(CommentComposerActivity.EXTRA_TARGET_ID, dynamic.id),
        )
    }

    private fun loadEmojiMap() {
        lifecycleScope.launch {
            runCatching { AppGraph.repository.genericList(ApiRoutes.EMOJI, limit = 100) }
                .onSuccess { groups ->
                    val map = buildMap {
                        groups.forEach { group ->
                            val groupName = AppGraph.repository.displayValue(group["name"])
                            val items = group["items"]?.takeIf { it.isJsonArray }?.asJsonArray ?: return@forEach
                            items.forEach { item ->
                                if (item.isJsonObject) {
                                    val obj = item.asJsonObject
                                    val name = AppGraph.repository.displayValue(obj["name"])
                                    val url = AppGraph.repository.displayValue(obj["url"])
                                    if (groupName.isNotBlank() && name.isNotBlank() && url.isNotBlank()) {
                                        put("[${groupName}_${name}]", AppGraph.repository.absoluteUrl(url))
                                    }
                                }
                            }
                        }
                    }
                    adapter.setEmojiMap(map)
                }
        }
    }
}
