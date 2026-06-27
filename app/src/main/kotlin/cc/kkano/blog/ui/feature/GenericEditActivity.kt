package cc.kkano.blog.ui.feature

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyDataBox
import cc.kkano.blog.ui.common.dataBox
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.sectionHeader
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.launch

class GenericEditActivity : AppCompatActivity() {
    private val repository = AppGraph.repository
    private val inputs = linkedMapOf<String, TextInputEditText>()
    private var endpoint = ""
    private var itemId = 0L
    private lateinit var submitButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        endpoint = intent.getStringExtra(EXTRA_ENDPOINT).orEmpty()
        val item = parseItem(intent.getStringExtra(EXTRA_ITEM_JSON).orEmpty())
        itemId = idOf(item)
        setContentView(buildContent(intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "编辑" }, item))
    }

    private fun buildContent(titleText: String, item: JsonObject): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = if (itemId > 0L) titleText else "新增$titleText",
                leftIcon = R.drawable.ic_back,
                onLeftClick = { finish() },
            ),
        )

        val scroll = ScrollView(this).apply {
            setBackgroundColor(KkColors.background)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(scroll)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(20))
        }
        scroll.addView(content)

        val box = dataBox(marginTop = 0).apply { applyDataBox(14) }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(14))
        }
        box.addView(form)
        form.addView(sectionHeader("▌ 可编辑字段"))

        fieldsFor(item).forEach { (key, value) ->
            val input = TextInputEditText(this).apply {
                setText(value)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines = if (value.length > 80) 3 else 1
            }
            inputs[key] = input
            form.addView(TextInputLayout(this).apply {
                hint = key
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                addView(input)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(12)
                }
            })
        }

        submitButton = MaterialButton(this).apply {
            text = if (itemId > 0L) "保存修改" else "新增"
            setTextColor(KkColors.black)
            setBackgroundColor(KkColors.orange)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply {
                topMargin = dp(8)
            }
            setOnClickListener { submit() }
        }
        form.addView(submitButton)
        content.addView(box)
        return root
    }

    private fun submit() {
        if (endpoint.isBlank()) {
            Toast.makeText(this, "缺少接口地址", Toast.LENGTH_SHORT).show()
            return
        }
        val body = inputs.mapValues { (_, input) ->
            val text = input.text?.toString().orEmpty().trim()
            parseScalar(text)
        }.filterValues { value ->
            value != null
        }
        submitButton.isEnabled = false
        submitButton.text = "提交中..."
        lifecycleScope.launch {
            val target = if (itemId > 0L) "${endpoint.trimEnd('/')}/$itemId" else endpoint
            val result = if (itemId > 0L) {
                runCatching { repository.put(target, body) }
            } else {
                runCatching { repository.post(target, body) }
            }
            result
                .onSuccess {
                    Toast.makeText(this@GenericEditActivity, "保存成功", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .onFailure {
                    Toast.makeText(this@GenericEditActivity, it.message ?: "保存失败", Toast.LENGTH_SHORT).show()
                }
            submitButton.isEnabled = true
            submitButton.text = if (itemId > 0L) "保存修改" else "新增"
        }
    }

    private fun fieldsFor(item: JsonObject): List<Pair<String, String>> {
        val excluded = setOf(
            "id", "uid", "coid", "created_at", "updated_at", "deleted_at", "create_time",
            "user", "author", "roles", "permissions", "pivot", "password", "token",
        )
        if (item.entrySet().isEmpty()) {
            return listOf("name" to "", "title" to "", "description" to "")
        }
        return item.entrySet()
            .filterNot { it.key in excluded }
            .filterNot { it.value.isJsonNull }
            .filter { it.value.isJsonPrimitive || it.value.isJsonArray }
            .map { it.key to if (it.value.isJsonPrimitive) it.value.asString else it.value.toString() }
    }

    private fun parseScalar(text: String): Any? {
        if (text.isBlank()) return ""
        if (text == "true") return true
        if (text == "false") return false
        text.toLongOrNull()?.let { return it }
        text.toDoubleOrNull()?.let { return it }
        if ((text.startsWith("[") && text.endsWith("]")) || (text.startsWith("{") && text.endsWith("}"))) {
            return runCatching { JsonParser.parseString(text) }.getOrDefault(text)
        }
        return text
    }

    private fun parseItem(json: String): JsonObject {
        return runCatching { JsonParser.parseString(json).asJsonObject }.getOrDefault(JsonObject())
    }

    private fun idOf(item: JsonObject): Long {
        return runCatching { item["id"]?.asLong }.getOrNull()
            ?: runCatching { item["uid"]?.asLong }.getOrNull()
            ?: runCatching { item["coid"]?.asLong }.getOrNull()
            ?: 0L
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ENDPOINT = "extra_endpoint"
        const val EXTRA_ITEM_JSON = "extra_item_json"
    }
}
