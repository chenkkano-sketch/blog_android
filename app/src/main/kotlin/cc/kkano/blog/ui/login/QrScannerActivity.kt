package cc.kkano.blog.ui.login

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import cc.kkano.blog.AppGraph
import cc.kkano.blog.R
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.roundedDrawable
import cc.kkano.blog.ui.common.setRoundedBackground
import com.google.gson.JsonParser
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

class QrScannerActivity : AppCompatActivity() {
    private lateinit var status: TextView

    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchScanner()
        } else {
            status.text = "没有相机权限，无法扫码"
            Toast.makeText(this, "请允许相机权限后再扫码", Toast.LENGTH_SHORT).show()
        }
    }

    private val scanner = registerForActivityResult(ScanContract()) { result ->
        val contents = result.contents.orEmpty()
        if (contents.isBlank()) {
            status.text = "扫码已取消"
            return@registerForActivityResult
        }
        handleScanResult(contents)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        startCameraFlow()
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = "扫码授权",
                leftIcon = R.drawable.ic_back,
                onLeftClick = { finish() },
            ),
        )
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(22), dp(28), dp(22), dp(28))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        }
        root.addView(content)
        content.addView(TextView(this).apply {
            text = "正在打开相机"
            applyTitleStyle(22f)
            gravity = Gravity.CENTER
        })
        status = TextView(this).apply {
            text = "请扫描网页端登录二维码"
            applyBodyStyle(14f)
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(14), dp(18), dp(14))
            setRoundedBackground(KkColors.surface, 14)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(18)
            }
        }
        content.addView(status)
        content.addView(TextView(this).apply {
            text = "重新扫码"
            applyTitleStyle(15f)
            setTextColor(KkColors.black)
            gravity = Gravity.CENTER
            background = roundedDrawable(KkColors.orange, 999)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48),
            ).apply {
                topMargin = dp(20)
            }
            setOnClickListener { startCameraFlow() }
        })
        return root
    }

    private fun startCameraFlow() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchScanner()
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchScanner() {
        status.text = "请扫描网页端登录二维码"
        scanner.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("扫描网页登录二维码")
                .setBeepEnabled(false)
                .setOrientationLocked(false),
        )
    }

    private fun handleScanResult(raw: String) {
        val sceneId = parseSceneId(raw)
        if (sceneId.isBlank()) {
            status.text = "二维码信息无效"
            Toast.makeText(this, "二维码信息无效", Toast.LENGTH_SHORT).show()
            return
        }
        status.text = "已识别二维码，正在进入确认页"
        lifecycleScope.launch {
            runCatching { AppGraph.repository.markQrScanned(sceneId) }
            startActivity(
                Intent(this@QrScannerActivity, QrConfirmActivity::class.java)
                    .putExtra(QrConfirmActivity.EXTRA_SCENE_ID, sceneId),
            )
            finish()
        }
    }

    private fun parseSceneId(raw: String): String {
        val value = raw.trim()
        if (value.startsWith("{")) {
            return runCatching {
                val obj = JsonParser.parseString(value).asJsonObject
                obj["scene_id"]?.asString
                    ?: obj["sceneId"]?.asString
                    ?: obj["data"]?.asString
                    ?: ""
            }.getOrDefault("")
        }
        return runCatching {
            val uri = Uri.parse(value)
            uri.getQueryParameter("scene_id")
                ?: uri.getQueryParameter("sceneId")
                ?: uri.getQueryParameter("data")
                ?: uri.lastPathSegment.orEmpty().takeIf { it.isNotBlank() && it != "scan" }
                ?: value
        }.getOrDefault(value)
    }
}
