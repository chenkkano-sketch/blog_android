package cc.kkano.blog.ui.article

import android.app.AlertDialog
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.data.api.ApiRoutes
import cc.kkano.blog.data.model.Article
import cc.kkano.blog.data.model.Tag
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyDataBox
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dataBox
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.common.sectionHeader
import cc.kkano.blog.ui.common.setRoundedBackground
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class ArticleEditorActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private var articleId: Long = 0L
    private var categoryId: Long = 0L
    private var coverPath: String = ""
    private var previewMode = false
    private val categories = mutableListOf<JsonObject>()
    private val availableTags = mutableListOf<JsonObject>()
    private val selectedTagIds = mutableListOf<Long>()
    private val selectedTags = mutableListOf<Tag>()

    private lateinit var titleInput: EditText
    private lateinit var categoryValue: TextView
    private lateinit var coverPreview: FrameLayout
    private lateinit var coverImage: ImageView
    private lateinit var summaryInput: EditText
    private lateinit var tagsRow: LinearLayout
    private lateinit var visibleSwitch: SwitchCompat
    private lateinit var contentInput: EditText
    private lateinit var previewWebView: WebView
    private lateinit var submitButton: TextView
    private lateinit var statusText: TextView

    private val pickCover = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uploadCover(uri)
    }

    private val pickEditorImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (!uris.isNullOrEmpty()) uploadEditorImages(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        articleId = intent.getLongExtra(EXTRA_ARTICLE_ID, 0L)
        setContentView(buildContent())
        loadCategories()
        loadTags()
        if (articleId > 0L) loadArticle(articleId)
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = if (articleId > 0L) "修改文章" else "发布文章",
                leftIcon = R.drawable.ic_back,
                rightIcon = R.drawable.ic_write,
                onLeftClick = { finish() },
                onRightClick = { submit() },
            ),
        )

        val scroll = ScrollView(this).apply {
            setBackgroundColor(KkColors.background)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(scroll)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(22))
        }
        scroll.addView(content)

        val box = dataBox(marginTop = 0).apply { applyDataBox(14) }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(14))
        }
        box.addView(form)
        form.addView(sectionHeader("▌ 文章信息"))

        titleInput = formInput("输入文章标题", singleLine = true)
        form.addView(formRow("文章标题", titleInput))

        categoryValue = TextView(this).apply {
            text = "请选择分类"
            applyBodyStyle(15f)
            setTextColor(KkColors.text)
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f)
        }
        form.addView(clickRow("选择分类", categoryValue) { showCategoryPicker() })

        form.addView(coverRow())

        summaryInput = formInput("输入文章摘要（选填）", singleLine = false).apply {
            minLines = 2
            gravity = Gravity.TOP
        }
        form.addView(formRow("摘要", summaryInput, alignTop = true))

        tagsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        form.addView(clickRow("标签", tagsRow) { showTagPicker() })

        visibleSwitch = SwitchCompat(this).apply {
            isChecked = true
            thumbTintList = android.content.res.ColorStateList.valueOf(KkColors.orange)
        }
        form.addView(switchRow("可见性", visibleSwitch))

        form.addView(sectionHeader("▌ Markdown 编辑器"))
        form.addView(editorToolbar())

        val editorFrame = FrameLayout(this).apply {
            setRoundedBackground(Color.parseColor("#F8F9FB"), 10, Color.parseColor("#1012151C"), 1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(390)).apply {
                bottomMargin = dp(12)
            }
        }
        contentInput = EditText(this).apply {
            hint = "文章内容"
            gravity = Gravity.TOP
            textSize = 15f
            setTextColor(KkColors.text)
            setHintTextColor(Color.parseColor("#AEB4BF"))
            background = null
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setSingleLine(false)
            minLines = 12
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        editorFrame.addView(contentInput)
        previewWebView = WebView(this).apply {
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        editorFrame.addView(previewWebView)
        form.addView(editorFrame)

        statusText = TextView(this).apply {
            applyBodyStyle(12f)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(8))
        }
        form.addView(statusText)

        submitButton = TextView(this).apply {
            text = if (articleId > 0L) "保存修改" else "发布文章"
            applyTitleStyle(16f)
            setTextColor(KkColors.black)
            gravity = Gravity.CENTER
            setRoundedBackground(KkColors.orange, 24)
            elevation = dp(8).toFloat()
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50))
            setOnClickListener { submit() }
        }
        form.addView(submitButton)
        content.addView(box)
        renderCover()
        renderSelectedTags()
        return root
    }

    private fun formInput(hintText: String, singleLine: Boolean): EditText {
        return EditText(this).apply {
            hint = hintText
            setSingleLine(singleLine)
            textSize = 15f
            setTextColor(KkColors.text)
            setHintTextColor(Color.parseColor("#B9BEC7"))
            background = null
            includeFontPadding = false
            setPadding(0, dp(10), 0, dp(10))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    private fun formRow(title: String, input: EditText, alignTop: Boolean = false): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (alignTop) Gravity.TOP else Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(10), dp(8))
            setRoundedBackground(Color.parseColor("#F8F9FB"), 11, Color.parseColor("#1012151C"), 1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            addView(label(title, alignTop))
            addView(input)
        }
    }

    private fun clickRow(title: String, valueView: View, onClick: () -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(10), dp(8))
            setRoundedBackground(Color.parseColor("#F8F9FB"), 11, Color.parseColor("#1012151C"), 1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            isClickable = true
            setOnClickListener { onClick() }
            addView(label(title))
            addView(valueView)
            addView(TextView(this@ArticleEditorActivity).apply {
                text = "›"
                textSize = 24f
                setTextColor(Color.parseColor("#BDC1C8"))
            })
        }
    }

    private fun switchRow(title: String, switchView: SwitchCompat): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(10), dp(8))
            setRoundedBackground(Color.parseColor("#F8F9FB"), 11, Color.parseColor("#1012151C"), 1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(62)).apply {
                bottomMargin = dp(10)
            }
            addView(label(title))
            addView(TextView(this@ArticleEditorActivity).apply {
                text = if (switchView.isChecked) "公开" else "隐藏"
                applyBodyStyle(14f)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(switchView)
        }
    }

    private fun label(textValue: String, alignTop: Boolean = false): TextView {
        return TextView(this).apply {
            text = textValue
            applyTitleStyle(14f)
            gravity = if (alignTop) Gravity.TOP else Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(dp(82), if (alignTop) dp(42) else ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun coverRow(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(dp(12), dp(8), dp(10), dp(8))
            setRoundedBackground(Color.parseColor("#F8F9FB"), 11, Color.parseColor("#1012151C"), 1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            addView(label("封面图", alignTop = true))
            coverPreview = FrameLayout(this@ArticleEditorActivity).apply {
                setRoundedBackground(Color.parseColor("#F8F8F8"), 10, Color.parseColor("#CCCCCC"), 1)
                layoutParams = LinearLayout.LayoutParams(0, dp(126), 1f)
                setOnClickListener { showCoverOptions() }
            }
            coverImage = ImageView(this@ArticleEditorActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
            coverPreview.addView(coverImage)
            coverPreview.addView(TextView(this@ArticleEditorActivity).apply {
                text = "点击上传封面"
                applyBodyStyle(13f)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            })
            addView(coverPreview)
        }
    }

    private fun editorToolbar(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(6), dp(6), dp(8))
            addView(toolButton("☺") { showEmojiPicker() })
            addView(toolButton("H") { showHeadingPicker() })
            addView(toolButton("B") { insertText("**加粗文字**") })
            addView(toolButton("I") { insertText("*斜体文字* ") })
            addView(toolButton("图") { pickEditorImages.launch("image/*") })
            addView(toolButton("库") { showMediaPicker(target = "editor") })
            addView(toolButton("{}") { insertText("\n```javascript\n代码片段\n```") })
            addView(toolButton("链") { showLinkDialog() })
            addView(toolButton("隐") { insertText("[hide]这是回复可见的内容[/hide]") })
            addView(toolButton("签") { showTagPicker() })
            addView(toolButton("阅") { togglePreview() })
        }
    }

    private fun toolButton(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            applyTitleStyle(13f)
            gravity = Gravity.CENTER
            setTextColor(KkColors.text)
            setRoundedBackground(Color.parseColor("#F7F8FA"), 9, Color.parseColor("#1012151C"), 1)
            layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                setMargins(dp(2), 0, dp(2), 0)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun loadArticle(id: Long) {
        submitButton.isEnabled = false
        statusText.text = "加载中..."
        lifecycleScope.launch {
            runCatching { repository.article(id) }
                .onSuccess { renderArticle(it) }
                .onFailure { Toast.makeText(this@ArticleEditorActivity, it.message ?: "文章加载失败", Toast.LENGTH_SHORT).show() }
            statusText.text = ""
            submitButton.isEnabled = true
        }
    }

    private fun renderArticle(article: Article) {
        titleInput.setText(article.title)
        summaryInput.setText(article.summary.orEmpty())
        contentInput.setText(article.content.orEmpty())
        categoryId = article.categoryId ?: 0L
        coverPath = article.cover.orEmpty().ifBlank { article.homeImg.orEmpty() }
        selectedTagIds.clear()
        selectedTags.clear()
        article.tags.forEach {
            selectedTagIds.add(it.id)
            selectedTags.add(it)
        }
        updateCategoryText()
        renderCover()
        renderSelectedTags()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            runCatching { repository.genericList(ApiRoutes.ARTICLE_CATEGORIES, limit = 100) }
                .onSuccess {
                    categories.clear()
                    categories.addAll(it)
                    if (categoryId <= 0L && categories.isNotEmpty()) {
                        categoryId = idOf(categories.first())
                    }
                    updateCategoryText()
                }
        }
    }

    private fun loadTags() {
        lifecycleScope.launch {
            runCatching { repository.genericList(ApiRoutes.TAGS, limit = 200) }
                .onSuccess {
                    availableTags.clear()
                    availableTags.addAll(it)
                }
        }
    }

    private fun showCategoryPicker() {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(18))
            background = roundedDrawable(Color.WHITE, 24)
        }
        root.addView(TextView(this).apply {
            text = "选择分类"
            applyTitleStyle(18f)
            setPadding(0, dp(4), 0, dp(10))
        })
        categories.forEach { category ->
            root.addView(sheetRow(nameOf(category), categoryId == idOf(category)) {
                categoryId = idOf(category)
                updateCategoryText()
                dialog.dismiss()
            })
        }
        dialog.setContentView(root)
        dialog.show()
        styleSheet(dialog)
    }

    private fun showTagPicker() {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(18))
            background = roundedDrawable(Color.WHITE, 24)
        }
        root.addView(sectionHeader("选择标签", "新建") { showCreateTagDialog(dialog) })
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(420))
        }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        availableTags.forEach { tag ->
            val id = idOf(tag)
            list.addView(sheetRow(nameOf(tag), selectedTagIds.contains(id)) {
                if (selectedTagIds.contains(id)) {
                    val index = selectedTagIds.indexOf(id)
                    selectedTagIds.removeAt(index)
                    selectedTags.removeAll { it.id == id }
                } else {
                    selectedTagIds.add(id)
                    selectedTags.add(Tag(id = id, name = nameOf(tag)))
                }
                renderSelectedTags()
                dialog.dismiss()
            })
        }
        root.addView(scroll)
        dialog.setContentView(root)
        dialog.show()
        styleSheet(dialog)
    }

    private fun showCreateTagDialog(parent: BottomSheetDialog) {
        val input = EditText(this).apply { hint = "标签名称" }
        val alert = AlertDialog.Builder(this)
            .setTitle("创建标签")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("创建") { _, _ ->
                val name = input.text?.toString().orEmpty().trim()
                if (name.isBlank()) return@setPositiveButton
                lifecycleScope.launch {
                    runCatching { repository.post(ApiRoutes.TAGS, mapOf("name" to name)) }
                        .onSuccess {
                            parent.dismiss()
                            loadTags()
                        }
                        .onFailure { Toast.makeText(this@ArticleEditorActivity, it.message ?: "创建失败", Toast.LENGTH_SHORT).show() }
                    }
            }
            .show()
        styleAlert(alert)
    }

    private fun showCoverOptions() {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(18))
            background = roundedDrawable(Color.WHITE, 24)
        }
        root.addView(sheetRow("从文件上传", false) {
            dialog.dismiss()
            pickCover.launch("image/*")
        })
        root.addView(sheetRow("从图床选择", false) {
            dialog.dismiss()
            showMediaPicker(target = "cover")
        })
        if (coverPath.isNotBlank()) {
            root.addView(sheetRow("移除封面", false) {
                coverPath = ""
                renderCover()
                dialog.dismiss()
            })
        }
        dialog.setContentView(root)
        dialog.show()
        styleSheet(dialog)
    }

    private fun showMediaPicker(target: String) {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(18))
            background = roundedDrawable(Color.WHITE, 24)
        }
        root.addView(TextView(this).apply {
            text = if (target == "cover") "选择封面" else "插入图片到编辑器"
            applyTitleStyle(18f)
            setPadding(0, dp(4), 0, dp(12))
        })
        val grid = GridLayout(this).apply { columnCount = 3 }
        root.addView(ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(390))
            addView(grid)
        })
        dialog.setContentView(root)
        dialog.show()
        styleSheet(dialog)
        lifecycleScope.launch {
            runCatching { repository.genericList(ApiRoutes.MEDIA, limit = 60) }
                .onSuccess { list ->
                    grid.removeAllViews()
                    list.forEach { item ->
                        grid.addView(mediaTile(item, target) {
                            dialog.dismiss()
                        })
                    }
                }
                .onFailure { Toast.makeText(this@ArticleEditorActivity, it.message ?: "图床加载失败", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun mediaTile(item: JsonObject, target: String, done: () -> Unit): View {
        val size = (resources.displayMetrics.widthPixels - dp(58)) / 3
        val url = display(item, "thumbnail_url").ifBlank { display(item, "url") }
        val raw = display(item, "raw_url").ifBlank { display(item, "url") }
        return MaterialCardView(this).apply {
            radius = dp(8).toFloat()
            cardElevation = 0f
            layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                setMargins(dp(3), dp(3), dp(3), dp(7))
            }
            addView(ImageView(this@ArticleEditorActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                Glide.with(this)
                    .load(repository.absoluteUrl(url))
                    .placeholder(R.drawable.bg_image_placeholder)
                    .centerCrop()
                    .into(this)
            })
            setOnClickListener {
                val relative = relativePath(raw)
                if (target == "cover") {
                    coverPath = relative
                    renderCover()
                } else {
                    insertImage(relative)
                }
                done()
            }
        }
    }

    private fun uploadCover(uri: Uri) {
        statusText.text = "上传封面中..."
        lifecycleScope.launch {
            runCatching { uploadUri(uri) }
                .onSuccess {
                    coverPath = it
                    renderCover()
                    Toast.makeText(this@ArticleEditorActivity, "上传成功", Toast.LENGTH_SHORT).show()
                }
                .onFailure { Toast.makeText(this@ArticleEditorActivity, it.message ?: "上传失败", Toast.LENGTH_SHORT).show() }
            statusText.text = ""
        }
    }

    private fun uploadEditorImages(uris: List<Uri>) {
        statusText.text = "上传图片中..."
        lifecycleScope.launch {
            uris.forEach { uri ->
                runCatching { uploadUri(uri) }
                    .onSuccess { insertImage(it) }
                    .onFailure { Toast.makeText(this@ArticleEditorActivity, it.message ?: "图片上传失败", Toast.LENGTH_SHORT).show() }
            }
            statusText.text = ""
        }
    }

    private suspend fun uploadUri(uri: Uri): String {
        val mime = contentResolver.getType(uri) ?: "image/jpeg"
        val fileName = queryFileName(uri)
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("无法读取文件")
        return repository.uploadMedia(fileName, bytes, mime)
    }

    private fun queryFileName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index)
        }
        return "image_${System.currentTimeMillis()}.jpg"
    }

    private fun showEmojiPicker() {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(18))
            background = roundedDrawable(Color.WHITE, 24)
        }
        root.addView(TextView(this).apply {
            text = "表情"
            applyTitleStyle(18f)
            setPadding(0, dp(4), 0, dp(12))
        })
        val listColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(420))
            addView(listColumn)
        })
        dialog.setContentView(root)
        dialog.show()
        styleSheet(dialog)
        lifecycleScope.launch {
            runCatching { repository.genericList(ApiRoutes.EMOJI, limit = 100) }
                .onSuccess { groups ->
                    listColumn.removeAllViews()
                    groups.forEach { group ->
                        val groupName = display(group, "name").ifBlank { "表情" }
                        listColumn.addView(TextView(this@ArticleEditorActivity).apply {
                            text = groupName
                            applyTitleStyle(14f)
                            setPadding(0, dp(8), 0, dp(8))
                        })
                        val grid = GridLayout(this@ArticleEditorActivity).apply { columnCount = 5 }
                        listColumn.addView(grid)
                        val items = group["items"]?.takeIf { it.isJsonArray }?.asJsonArray ?: return@forEach
                        items.forEach { element ->
                            if (element.isJsonObject) {
                                val item = element.asJsonObject
                                val name = display(item, "name")
                                val url = display(item, "url")
                                grid.addView(emojiTile(groupName, name, url))
                            }
                        }
                    }
                }
        }
    }

    private fun emojiTile(groupName: String, name: String, url: String): View {
        val size = (resources.displayMetrics.widthPixels - dp(68)) / 5
        return FrameLayout(this).apply {
            layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
            setOnClickListener {
                if (name.isNotBlank()) insertText("[${groupName}_${name}]")
            }
            addView(ImageView(this@ArticleEditorActivity).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(dp(6), dp(6), dp(6), dp(6))
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                Glide.with(this).load(repository.absoluteUrl(url)).fitCenter().into(this)
            })
        }
    }

    private fun showHeadingPicker() {
        val levels = arrayOf("标题 1", "标题 2", "标题 3", "标题 4", "标题 5")
        val alert = AlertDialog.Builder(this)
            .setItems(levels) { _, which ->
                insertText("\n${"#".repeat(which + 1)} 标题文字")
            }
            .show()
        styleAlert(alert)
    }

    private fun showLinkDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), 0, dp(20), 0)
        }
        val title = EditText(this).apply { hint = "链接标题" }
        val url = EditText(this).apply { hint = "http(s)://" }
        box.addView(title)
        box.addView(url)
        val alert = AlertDialog.Builder(this)
            .setTitle("插入外部链接")
            .setView(box)
            .setNegativeButton("取消", null)
            .setNeutralButton("远程图片") { _, _ ->
                insertText("![${title.text}](${url.text})")
            }
            .setPositiveButton("超链接") { _, _ ->
                insertText("[${title.text}](${url.text})")
            }
            .show()
        styleAlert(alert)
    }

    private fun togglePreview() {
        previewMode = !previewMode
        contentInput.isVisible = !previewMode
        previewWebView.isVisible = previewMode
        if (previewMode) {
            previewWebView.loadDataWithBaseURL(
                AppGraph.apiClient.baseUrl,
                MarkdownRenderer.document(contentInput.text?.toString().orEmpty()),
                "text/html",
                "utf-8",
                null,
            )
        }
    }

    private fun insertImage(path: String) {
        insertText("\n<img src=\"${repository.absoluteUrl(path)}\" style=\"max-width:100%;\" />")
    }

    private fun insertText(value: String) {
        val editable = contentInput.text ?: return
        val start = contentInput.selectionStart.coerceAtLeast(0)
        editable.insert(start, value)
        if (previewMode) togglePreview()
    }

    private fun submit() {
        val title = titleInput.text?.toString().orEmpty().trim()
        val content = contentInput.text?.toString().orEmpty().trim()
        if (title.isBlank() || categoryId <= 0L || content.isBlank()) {
            Toast.makeText(this, "请输入正确的参数", Toast.LENGTH_SHORT).show()
            return
        }
        val body = mapOf(
            "title" to title,
            "category_id" to categoryId,
            "content" to content,
            "cover" to coverPath,
            "summary" to summaryInput.text?.toString().orEmpty().trim(),
            "is_visible" to visibleSwitch.isChecked,
            "tag_ids" to selectedTagIds,
        )
        submitButton.isEnabled = false
        submitButton.text = if (articleId > 0L) "保存中..." else "发布中..."
        lifecycleScope.launch {
            val result = if (articleId > 0L) {
                runCatching { repository.updateArticle(articleId, body) }
            } else {
                runCatching { repository.publishArticle(body) }
            }
            result
                .onSuccess {
                    Toast.makeText(this@ArticleEditorActivity, if (articleId > 0L) "更新成功" else "发布成功", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .onFailure {
                    Toast.makeText(this@ArticleEditorActivity, it.message ?: if (articleId > 0L) "更新失败" else "发布失败", Toast.LENGTH_SHORT).show()
                }
            submitButton.isEnabled = true
            submitButton.text = if (articleId > 0L) "保存修改" else "发布文章"
        }
    }

    private fun renderCover() {
        coverImage.isVisible = coverPath.isNotBlank()
        if (coverPath.isNotBlank()) {
            Glide.with(coverImage)
                .load(repository.absoluteUrl(coverPath))
                .placeholder(R.drawable.bg_image_placeholder)
                .centerCrop()
                .into(coverImage)
        } else {
            Glide.with(coverImage).clear(coverImage)
        }
    }

    private fun renderSelectedTags() {
        tagsRow.removeAllViews()
        if (selectedTags.isEmpty()) {
            tagsRow.addView(TextView(this).apply {
                text = "点击选择标签"
                applyBodyStyle(14f)
            })
            return
        }
        selectedTags.take(4).forEach { tag ->
            tagsRow.addView(TextView(this).apply {
                text = tag.name
                applyTitleStyle(12f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setRoundedBackground(KkColors.orange, 999)
                setPadding(dp(9), dp(4), dp(9), dp(4))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    rightMargin = dp(6)
                }
            })
        }
    }

    private fun updateCategoryText() {
        val category = categories.firstOrNull { idOf(it) == categoryId }
        categoryValue.text = category?.let { nameOf(it) } ?: if (categoryId > 0L) "分类 $categoryId" else "请选择分类"
    }

    private fun sheetRow(textValue: String, checked: Boolean, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = if (checked) "✓ $textValue" else textValue
            applyTitleStyle(15f)
            setTextColor(if (checked) KkColors.orange else KkColors.text)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52))
            setOnClickListener { onClick() }
        }
    }

    private fun styleSheet(dialog: BottomSheetDialog) {
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun styleAlert(dialog: AlertDialog) {
        dialog.window?.setBackgroundDrawable(roundedDrawable(Color.WHITE, 18))
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(KkColors.orange)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(KkColors.muted)
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(KkColors.black)
    }

    private fun idOf(item: JsonObject): Long {
        return runCatching { item["id"]?.asLong }.getOrNull()
            ?: runCatching { item["mid"]?.asLong }.getOrNull()
            ?: 0L
    }

    private fun nameOf(item: JsonObject): String {
        return display(item, "name").ifBlank { display(item, "title") }.ifBlank { "未命名" }
    }

    private fun display(item: JsonObject, key: String): String {
        return repository.displayValue(item[key])
    }

    private fun relativePath(path: String): String {
        val base = AppGraph.apiClient.baseUrl.trimEnd('/')
        return path.removePrefix(base)
    }

    companion object {
        const val EXTRA_ARTICLE_ID = "extra_article_id"
    }
}
