package cc.kkano.blog.ui.dynamics

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
import androidx.lifecycle.lifecycleScope
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.data.api.ApiRoutes
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
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class DynamicEditorActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val selectedImages = mutableListOf<String>()
    private val typeLabels = listOf("图文", "文字", "B站视频", "外部引用")
    private var type = 1
    private var dynamicId: Long = 0L

    private lateinit var typeRow: LinearLayout
    private lateinit var titleInput: EditText
    private lateinit var contentInput: EditText
    private lateinit var bvidInput: EditText
    private lateinit var externalUrlInput: EditText
    private lateinit var externalTitleInput: EditText
    private lateinit var imageSection: LinearLayout
    private lateinit var imageGrid: GridLayout
    private lateinit var submitButton: TextView
    private lateinit var statusText: TextView
    private lateinit var bvidRow: View
    private lateinit var externalUrlRow: View
    private lateinit var externalTitleRow: View

    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNullOrEmpty()) return@registerForActivityResult
        uploadUris(uris.take(9 - selectedImages.size))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dynamicId = intent.getLongExtra(EXTRA_DYNAMIC_ID, 0L)
        setContentView(buildContent())
        if (dynamicId > 0L) loadDynamic(dynamicId)
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = if (dynamicId > 0L) "编辑动态" else "发布动态",
                leftIcon = R.drawable.ic_back,
                onLeftClick = { finish() },
            ),
        )

        val scroll = ScrollView(this).apply {
            setBackgroundColor(KkColors.background)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        }
        root.addView(scroll)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(20))
        }
        scroll.addView(content)

        val box = dataBox(marginTop = 0).apply { applyDataBox(14) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(14))
        }
        box.addView(column)
        column.addView(sectionHeader("▌ 动态内容"))

        typeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(12))
        }
        column.addView(typeRow)
        buildTypeChips()

        titleInput = formInput("请输入标题", singleLine = true)
        column.addView(formRow("标题", titleInput, emoji = true))

        contentInput = formInput("请输入内容", singleLine = false).apply {
            minLines = 5
            gravity = Gravity.TOP
        }
        column.addView(formRow("内容", contentInput, emoji = true, alignTop = true))

        imageSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(6))
        }
        column.addView(imageSection)
        imageSection.addView(TextView(this).apply {
            text = "图片"
            applyTitleStyle(15f)
            setPadding(0, 0, 0, dp(10))
        })
        imageGrid = GridLayout(this).apply {
            columnCount = 3
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        imageSection.addView(imageGrid)
        imageSection.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
            addView(smallAction("上传本地图片", R.drawable.ic_image) { pickImages.launch("image/*") })
            addView(smallAction("从图床选择", R.drawable.ic_search) { showMediaPicker() })
        })

        bvidInput = formInput("请输入 BV 号", singleLine = true)
        bvidRow = formRow("B站视频", bvidInput)
        column.addView(bvidRow)

        externalUrlInput = formInput("请输入外部链接", singleLine = true)
        externalUrlRow = formRow("外部链接", externalUrlInput)
        column.addView(externalUrlRow)

        externalTitleInput = formInput("请输入链接标题", singleLine = true)
        externalTitleRow = formRow("链接标题", externalTitleInput)
        column.addView(externalTitleRow)

        statusText = TextView(this).apply {
            applyBodyStyle(12f)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
        }
        column.addView(statusText)

        submitButton = TextView(this).apply {
            text = if (dynamicId > 0L) "保存修改" else "发布动态"
            applyTitleStyle(16f)
            setTextColor(KkColors.black)
            gravity = Gravity.CENTER
            setRoundedBackground(KkColors.orange, 24)
            elevation = dp(8).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50),
            ).apply {
                topMargin = dp(6)
            }
            setOnClickListener { submit() }
        }
        column.addView(submitButton)
        content.addView(box)

        updateConditionalFields()
        renderImageGrid()
        return root
    }

    private fun loadDynamic(id: Long) {
        submitButton.isEnabled = false
        statusText.text = "加载中..."
        lifecycleScope.launch {
            runCatching { repository.dynamic(id) }
                .onSuccess { dynamic ->
                    type = dynamic.type ?: dynamic.typeFromContent()
                    titleInput.setText(dynamic.title.orEmpty())
                    contentInput.setText(dynamic.content.orEmpty())
                    bvidInput.setText(dynamic.bvid.orEmpty())
                    externalUrlInput.setText(dynamic.externalUrl.orEmpty())
                    externalTitleInput.setText(dynamic.externalTitle.orEmpty())
                    selectedImages.clear()
                    selectedImages.addAll(dynamic.imagePaths())
                    buildTypeChips()
                    updateConditionalFields()
                    renderImageGrid()
                    statusText.text = ""
                }
                .onFailure {
                    statusText.text = ""
                    Toast.makeText(this@DynamicEditorActivity, it.message ?: "动态加载失败", Toast.LENGTH_SHORT).show()
                }
            submitButton.isEnabled = true
        }
    }

    private fun buildTypeChips() {
        typeRow.removeAllViews()
        typeLabels.forEachIndexed { index, label ->
            val value = index + 1
            typeRow.addView(TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                includeFontPadding = false
                setTextColor(if (type == value) Color.WHITE else KkColors.text)
                setRoundedBackground(
                    if (type == value) KkColors.black else Color.parseColor("#F7F8FA"),
                    999,
                    if (type == value) null else Color.parseColor("#1012151C"),
                    1,
                )
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                }
                setOnClickListener {
                    type = value
                    buildTypeChips()
                    updateConditionalFields()
                }
            })
        }
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

    private fun formRow(
        title: String,
        input: EditText,
        emoji: Boolean = false,
        alignTop: Boolean = false,
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (alignTop) Gravity.TOP else Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(10), dp(8))
            setRoundedBackground(Color.parseColor("#F8F9FB"), 11, Color.parseColor("#1012151C"), 1)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(10)
            }
            addView(TextView(this@DynamicEditorActivity).apply {
                text = title
                applyTitleStyle(14f)
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(dp(72), if (alignTop) dp(42) else ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(input)
            if (emoji) {
                addView(TextView(this@DynamicEditorActivity).apply {
                    text = "☺"
                    textSize = 22f
                    gravity = Gravity.CENTER
                    setTextColor(KkColors.softMuted)
                    layoutParams = LinearLayout.LayoutParams(dp(38), dp(42))
                    setOnClickListener {
                        showEmojiPicker(input)
                    }
                })
            }
        }
    }

    private fun smallAction(label: String, icon: Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)
            compoundDrawablePadding = dp(6)
            gravity = Gravity.CENTER
            applyTitleStyle(13f)
            setTextColor(KkColors.text)
            setRoundedBackground(Color.parseColor("#F7F8FA"), 999, Color.parseColor("#1012151C"), 1)
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                setMargins(dp(3), 0, dp(3), 0)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun updateConditionalFields() {
        imageSection.visibility = if (type == 1 || type == 4) View.VISIBLE else View.GONE
        bvidRow.visibility = if (type == 3) View.VISIBLE else View.GONE
        externalUrlRow.visibility = if (type == 4) View.VISIBLE else View.GONE
        externalTitleRow.visibility = if (type == 4) View.VISIBLE else View.GONE
    }

    private fun renderImageGrid() {
        imageGrid.removeAllViews()
        selectedImages.forEachIndexed { index, path ->
            imageGrid.addView(imageTile(path) { removeImage(index) })
        }
        if (selectedImages.size < 9) {
            imageGrid.addView(addTile())
        }
    }

    private fun imageTile(path: String, onRemove: () -> Unit): View {
        val size = (resources.displayMetrics.widthPixels - dp(70)) / 3
        val card = MaterialCardView(this).apply {
            radius = dp(8).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.parseColor("#EDF0F4"))
            layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                setMargins(dp(3), dp(3), dp(3), dp(7))
            }
        }
        val frame = FrameLayout(this)
        card.addView(frame)
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        frame.addView(image)
        Glide.with(image)
            .load(repository.absoluteUrl(path))
            .placeholder(R.drawable.bg_image_placeholder)
            .centerCrop()
            .into(image)
        frame.addView(TextView(this).apply {
            text = "×"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = roundedDrawable(KkColors.danger, 999)
            layoutParams = FrameLayout.LayoutParams(dp(24), dp(24), Gravity.TOP or Gravity.END).apply {
                setMargins(0, dp(6), dp(6), 0)
            }
            setOnClickListener { onRemove() }
        })
        return card
    }

    private fun addTile(): View {
        val size = (resources.displayMetrics.widthPixels - dp(70)) / 3
        return TextView(this).apply {
            text = "+"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(KkColors.softMuted)
            background = roundedDrawable(Color.parseColor("#F8F8F8"), 8, Color.parseColor("#CCCCCC"), 1)
            layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                setMargins(dp(3), dp(3), dp(3), dp(7))
            }
            setOnClickListener { pickImages.launch("image/*") }
        }
    }

    private fun removeImage(index: Int) {
        if (index in selectedImages.indices) {
            selectedImages.removeAt(index)
            renderImageGrid()
        }
    }

    private fun uploadUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        submitButton.isEnabled = false
        statusText.text = "上传图片中..."
        lifecycleScope.launch {
            uris.forEach { uri ->
                runCatching {
                    val mime = contentResolver.getType(uri) ?: "image/jpeg"
                    val fileName = queryFileName(uri)
                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("无法读取图片")
                    repository.uploadMedia(fileName, bytes, mime)
                }.onSuccess { path ->
                    if (path.isNotBlank() && selectedImages.size < 9) selectedImages.add(path)
                }.onFailure {
                    Toast.makeText(this@DynamicEditorActivity, it.message ?: "图片上传失败", Toast.LENGTH_SHORT).show()
                }
            }
            submitButton.isEnabled = true
            statusText.text = ""
            renderImageGrid()
        }
    }

    private fun queryFileName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index)
        }
        return "image_${System.currentTimeMillis()}.jpg"
    }

    private fun showMediaPicker() {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(18))
        }
        root.addView(TextView(this).apply {
            text = "从图床选择"
            applyTitleStyle(18f)
            setPadding(0, dp(4), 0, dp(12))
        })
        val grid = GridLayout(this).apply {
            columnCount = 3
        }
        root.addView(ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(360),
            )
            addView(grid)
        })
        root.addView(TextView(this).apply {
            text = "加载中..."
            applyBodyStyle(13f)
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, 0)
        })
        dialog.setContentView(root)
        dialog.show()
        lifecycleScope.launch {
            runCatching { repository.genericList(ApiRoutes.MEDIA, limit = 30) }
                .onSuccess { list ->
                    grid.removeAllViews()
                    list.forEach { item -> grid.addView(mediaTile(item, dialog)) }
                }
                .onFailure {
                    Toast.makeText(this@DynamicEditorActivity, it.message ?: "图床加载失败", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showEmojiPicker(target: EditText) {
        val dialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(18))
        }
        root.addView(TextView(this).apply {
            text = "表情"
            applyTitleStyle(18f)
            setPadding(0, dp(4), 0, dp(8))
        })
        root.addView(TextView(this).apply {
            text = "插入到：${if (target === titleInput) "标题" else "内容"}"
            applyBodyStyle(12f)
            setPadding(0, 0, 0, dp(10))
        })
        val listColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val scroller = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(420),
            )
            addView(listColumn)
        }
        root.addView(scroller)
        val loading = TextView(this).apply {
            text = "加载中..."
            applyBodyStyle(13f)
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(24))
        }
        listColumn.addView(loading)
        dialog.setContentView(root)
        dialog.show()

        lifecycleScope.launch {
            runCatching { repository.genericList(ApiRoutes.EMOJI, limit = 100) }
                .onSuccess { groups ->
                    listColumn.removeAllViews()
                    if (groups.isEmpty()) {
                        listColumn.addView(emptyPanelText("暂无表情数据"))
                    } else {
                        groups.forEach { group -> listColumn.addView(emojiGroupView(group, target)) }
                    }
                }
                .onFailure {
                    listColumn.removeAllViews()
                    listColumn.addView(emptyPanelText(it.message ?: "表情加载失败"))
                }
        }
    }

    private fun emojiGroupView(group: JsonObject, target: EditText): View {
        val groupName = displayValue(group, "name").ifBlank { "表情" }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(10))
        }
        root.addView(TextView(this).apply {
            text = groupName
            applyTitleStyle(14f)
            setPadding(0, 0, 0, dp(8))
        })
        val grid = GridLayout(this).apply {
            columnCount = 5
        }
        root.addView(grid)
        val items = group["items"]?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
        if (items.size() == 0) {
            root.addView(emptyPanelText("该分类暂无表情"))
        } else {
            items.forEach { element ->
                if (element.isJsonObject) {
                    grid.addView(emojiItem(groupName, element.asJsonObject, target))
                }
            }
        }
        return root
    }

    private fun emojiItem(groupName: String, item: JsonObject, target: EditText): View {
        val itemName = displayValue(item, "name")
        val url = displayValue(item, "url")
        val size = (resources.displayMetrics.widthPixels - dp(68)) / 5
        return FrameLayout(this).apply {
            layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
            setOnClickListener {
                if (itemName.isNotBlank()) {
                    target.append("[${groupName}_${itemName}]")
                }
            }
            addView(ImageView(this@DynamicEditorActivity).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(dp(6), dp(6), dp(6), dp(6))
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                if (url.isNotBlank()) {
                    Glide.with(this)
                        .load(repository.absoluteUrl(url))
                        .placeholder(R.drawable.bg_image_placeholder)
                        .fitCenter()
                        .into(this)
                }
            })
        }
    }

    private fun emptyPanelText(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            applyBodyStyle(13f)
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(24))
        }
    }

    private fun mediaTile(item: JsonObject, dialog: BottomSheetDialog): View {
        val size = (resources.displayMetrics.widthPixels - dp(58)) / 3
        val url = displayValue(item, "thumbnail_url").ifBlank { displayValue(item, "url") }
        val submitPath = displayValue(item, "raw_url").ifBlank { displayValue(item, "url") }
        val card = MaterialCardView(this).apply {
            radius = dp(8).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.parseColor("#EDF0F4"))
            layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                setMargins(dp(3), dp(3), dp(3), dp(7))
            }
            setOnClickListener {
                if (selectedImages.size >= 9) {
                    Toast.makeText(this@DynamicEditorActivity, "最多选择 9 张图片", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                selectedImages.add(relativePath(submitPath))
                renderImageGrid()
                dialog.dismiss()
            }
        }
        card.addView(ImageView(this).apply {
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
        return card
    }

    private fun displayValue(item: JsonObject, key: String): String {
        return AppGraph.repository.displayValue(item[key])
    }

    private fun relativePath(path: String): String {
        val base = AppGraph.apiClient.baseUrl.trimEnd('/')
        return path.removePrefix(base)
    }

    private fun submit() {
        if (!submitButton.isEnabled) return
        val title = titleInput.text?.toString().orEmpty().trim()
        val content = contentInput.text?.toString().orEmpty().trim()
        if (title.isBlank()) {
            Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show()
            return
        }
        if (type == 3 && bvidInput.text?.toString().orEmpty().isBlank()) {
            Toast.makeText(this, "请输入B站BV号", Toast.LENGTH_SHORT).show()
            return
        }
        if (type == 4 && externalUrlInput.text?.toString().orEmpty().isBlank()) {
            Toast.makeText(this, "请输入外部链接", Toast.LENGTH_SHORT).show()
            return
        }

        val body = buildMap<String, Any?> {
            put("type", type)
            put("title", title)
            put("content", content)
            put("status", 1)
            if ((type == 1 || type == 4) && selectedImages.isNotEmpty()) put("images", selectedImages)
            if (type == 3) put("bvid", bvidInput.text?.toString().orEmpty().trim())
            if (type == 4) {
                put("external_url", externalUrlInput.text?.toString().orEmpty().trim())
                put("external_title", externalTitleInput.text?.toString().orEmpty().trim())
            }
        }
        submitButton.isEnabled = false
        submitButton.text = if (dynamicId > 0L) "保存中..." else "发布中..."
        lifecycleScope.launch {
            val request = if (dynamicId > 0L) {
                runCatching { repository.updateDynamic(dynamicId, body) }
            } else {
                runCatching { repository.publishDynamic(body) }
            }
            request
                .onSuccess {
                    Toast.makeText(
                        this@DynamicEditorActivity,
                        if (dynamicId > 0L) "保存成功" else "发布成功",
                        Toast.LENGTH_SHORT,
                    ).show()
                    finish()
                }
                .onFailure {
                    Toast.makeText(
                        this@DynamicEditorActivity,
                        it.message ?: if (dynamicId > 0L) "保存失败" else "发布失败",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            submitButton.isEnabled = true
            submitButton.text = if (dynamicId > 0L) "保存修改" else "发布动态"
        }
    }

    private fun cc.kkano.blog.data.model.Dynamic.typeFromContent(): Int {
        val hasImages = imagePaths().isNotEmpty()
        val contentValue = content.orEmpty()
        return when {
            contentValue.contains("bvid=", ignoreCase = true) -> 3
            hasImages -> 1
            else -> 2
        }
    }

    companion object {
        const val EXTRA_DYNAMIC_ID = "extra_dynamic_id"
    }
}
