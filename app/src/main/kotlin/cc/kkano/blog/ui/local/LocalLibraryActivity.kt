package cc.kkano.blog.ui.local

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cc.kkano.blog.R
import cc.kkano.blog.ui.common.KkColors
import cc.kkano.blog.ui.common.applyBodyStyle
import cc.kkano.blog.ui.common.applyTitleStyle
import cc.kkano.blog.ui.common.dp
import cc.kkano.blog.ui.common.kkTopBar
import cc.kkano.blog.ui.common.setRoundedBackground
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalLibraryActivity : AppCompatActivity() {
    private lateinit var rootPathValue: TextView
    private lateinit var wallpaperPathValue: TextView
    private lateinit var videoPathValue: TextView
    private lateinit var folderCountValue: TextView
    private lateinit var wallpaperCountValue: TextView
    private lateinit var videoCountValue: TextView
    private lateinit var sizeValue: TextView
    private lateinit var folderList: LinearLayout
    private lateinit var summaryText: TextView

    private val imageExt = setOf("jpg", "jpeg", "png", "webp", "gif")
    private val videoExt = setOf("mp4", "mkv", "mov", "avi", "webm")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        ensureLayout()
        refreshData("目录已准备好")
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KkColors.background)
        }
        root.addView(
            kkTopBar(
                title = "本地资源库",
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
            setPadding(dp(10), dp(10), dp(10), dp(22))
        }
        scroll.addView(content)

        content.addView(section("资源库目录").apply {
            rootPathValue = pathRow("固定根目录", rootDir().absolutePath)
            addView(rootPathValue)
            addView(LinearLayout(this@LocalLibraryActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(12), 0, 0)
                addView(actionButton("复制根目录", KkColors.orange) { copyPath(rootDir().absolutePath) })
                addView(actionButton("重新创建目录", KkColors.black) {
                    ensureLayout()
                    refreshData("目录已重新创建")
                })
            })
        })

        content.addView(section("目录规则").apply {
            wallpaperPathValue = pathRow("壁纸", wallpaperDir().absolutePath)
            videoPathValue = pathRow("视频", videoDir().absolutePath)
            addView(wallpaperPathValue)
            addView(ruleText("一级子文件夹就是专辑名，例如：风景、动漫、人物。只解析这些专辑文件夹里的图片。"))
            addView(videoPathValue)
            addView(ruleText("一级子文件夹就是分类名，例如：电影、短片、演唱会。只解析这些分类文件夹里的视频。"))
        })

        content.addView(section("资源统计").apply {
            addView(LinearLayout(this@LocalLibraryActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                folderCountValue = statBox("0", "目录").also { addView(it) }
                wallpaperCountValue = statBox("0", "壁纸").also { addView(it) }
                videoCountValue = statBox("0", "视频").also { addView(it) }
                sizeValue = statBox("0 B", "总大小").also { addView(it) }
            })
        })

        content.addView(section("目录状态").apply {
            folderList = LinearLayout(this@LocalLibraryActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            addView(folderList)
        })

        content.addView(section("开始解析").apply {
            addView(ruleText("把文件放进固定目录后，点一次开始解析即可。当前原生版会扫描 app 专属资源目录，统计目录、图片、视频和总大小。"))
            addView(LinearLayout(this@LocalLibraryActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(12), 0, 0)
                addView(actionButton("开始解析", KkColors.orange) { refreshData("解析完成") })
            })
        })

        summaryText = TextView(this).apply {
            applyBodyStyle(13f)
            setTextColor(KkColors.text)
            setRoundedBackground(Color.WHITE, 14)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10)
            }
        }
        content.addView(summaryText)
        return root
    }

    private fun section(title: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setRoundedBackground(Color.WHITE, 14)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            addView(TextView(this@LocalLibraryActivity).apply {
                text = title
                applyTitleStyle(16f)
                setPadding(0, 0, 0, dp(12))
            })
        }
    }

    private fun pathRow(label: String, path: String): TextView {
        return TextView(this).apply {
            text = "$label\n$path"
            applyBodyStyle(12.5f)
            setTextColor(KkColors.text)
            setRoundedBackground(Color.parseColor("#F7F8FA"), 11)
            setPadding(dp(10), dp(9), dp(10), dp(9))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(8)
            }
            setOnClickListener { copyPath(path) }
        }
    }

    private fun ruleText(value: String): TextView {
        return TextView(this).apply {
            text = value
            applyBodyStyle(12.5f)
            setLineSpacing(dp(2).toFloat(), 1f)
            setPadding(0, 0, 0, dp(8))
        }
    }

    private fun statBox(value: String, label: String): TextView {
        return TextView(this).apply {
            text = "$value\n$label"
            applyTitleStyle(13f)
            gravity = Gravity.CENTER
            setRoundedBackground(Color.parseColor("#F7F8FA"), 12)
            layoutParams = LinearLayout.LayoutParams(0, dp(72), 1f).apply {
                setMargins(dp(3), 0, dp(3), 0)
            }
        }
    }

    private fun actionButton(label: String, color: Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            applyTitleStyle(13f)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setRoundedBackground(color, 999)
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                setMargins(dp(3), 0, dp(3), 0)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun ensureLayout() {
        listOf(rootDir(), wallpaperDir(), videoDir()).forEach { it.mkdirs() }
        listOf(File(wallpaperDir(), "风景"), File(wallpaperDir(), "人物"), File(videoDir(), "短片")).forEach { it.mkdirs() }
    }

    private fun refreshData(message: String) {
        val wallpaperFiles = wallpaperDir().walkFiles().filter { it.extension.lowercase(Locale.ROOT) in imageExt }
        val videoFiles = videoDir().walkFiles().filter { it.extension.lowercase(Locale.ROOT) in videoExt }
        val folders = rootDir().walkTopDown().filter { it.isDirectory }.toList()
        val totalSize = (wallpaperFiles + videoFiles).sumOf { it.length() }

        folderCountValue.text = "${folders.size}\n目录"
        wallpaperCountValue.text = "${wallpaperFiles.size}\n壁纸"
        videoCountValue.text = "${videoFiles.size}\n视频"
        sizeValue.text = "${formatSize(totalSize)}\n总大小"
        rootPathValue.text = "固定根目录\n${rootDir().absolutePath}"
        wallpaperPathValue.text = "壁纸\n${wallpaperDir().absolutePath}"
        videoPathValue.text = "视频\n${videoDir().absolutePath}"

        folderList.removeAllViews()
        folders.take(8).forEach { folder ->
            folderList.addView(folderRow(folder))
        }
        summaryText.text = "$message：共 ${wallpaperFiles.size + videoFiles.size} 个资源，${folders.size} 个目录，${formatSize(totalSize)}。"
    }

    private fun folderRow(folder: File): View {
        val files = folder.listFiles()?.filter { it.isFile }.orEmpty()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setRoundedBackground(Color.parseColor("#F7F8FA"), 11)
            setPadding(dp(10), dp(9), dp(10), dp(9))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(8)
            }
            addView(TextView(this@LocalLibraryActivity).apply {
                text = folder.name.ifBlank { "资源库" }
                applyTitleStyle(13.5f)
            })
            addView(TextView(this@LocalLibraryActivity).apply {
                text = "${folder.absolutePath}\n上次扫描：${nowText()} 成功 · 文件 ${files.size}"
                applyBodyStyle(11.5f)
                setPadding(0, dp(6), 0, 0)
            })
        }
    }

    private fun copyPath(path: String) {
        val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("local_library_path", path))
        Toast.makeText(this, "路径已复制", Toast.LENGTH_SHORT).show()
    }

    private fun rootDir(): File = File(getExternalFilesDir(null), "LocalLibrary")
    private fun wallpaperDir(): File = File(rootDir(), "wallpaper")
    private fun videoDir(): File = File(rootDir(), "video")

    private fun File.walkFiles(): List<File> = walkTopDown().filter { it.isFile }.toList()

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = listOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var index = 0
        while (size >= 1024 && index < units.lastIndex) {
            size /= 1024.0
            index++
        }
        return if (index == 0) "${size.toInt()} ${units[index]}" else String.format(Locale.ROOT, "%.1f %s", size, units[index])
    }

    private fun nowText(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date())
}
