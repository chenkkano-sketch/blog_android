package cc.kkano.blog.ui.article

import cc.kkano.blog.AppGraph
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

object MarkdownRenderer {
    private val extensions = listOf(TablesExtension.create(), AutolinkExtension.create())
    private val parser: Parser = Parser.builder().extensions(extensions).build()
    private val renderer: HtmlRenderer = HtmlRenderer.builder().extensions(extensions).escapeHtml(false).build()

    fun toHtml(markdown: String, canReadHidden: Boolean = true): String {
        val normalized = normalizeHidden(markdown, canReadHidden)
        return renderer.render(parser.parse(normalized))
    }

    fun document(markdown: String, canReadHidden: Boolean = true): String {
        val base = AppGraph.apiClient.baseUrl.trimEnd('/')
        val html = toHtml(markdown, canReadHidden)
            .replace(Regex("""src=["'](/[^"']+)["']""")) { match ->
                match.value.replace(match.groupValues[1], base + match.groupValues[1])
            }
        return """
            <!doctype html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body{margin:0;padding:0;color:#333;font-size:16px;line-height:1.82;font-family:-apple-system,BlinkMacSystemFont,'PingFang SC','Microsoft YaHei',sans-serif;word-break:break-word;background:#fff;}
                    h1,h2,h3,h4,h5{color:#111;margin:22px 0 12px;font-weight:700;line-height:1.35;}
                    h1{font-size:26px;} h2{font-size:23px;} h3{font-size:20px;} h4{font-size:18px;} h5{font-size:16px;}
                    p{margin:0 0 14px;}
                    ul,ol{padding-left:22px;margin:0 0 14px;}
                    li{margin:6px 0;}
                    img,video{max-width:100%;height:auto;border-radius:6px;background:#f5f5f5;}
                    a{color:#0081ff;text-decoration:none;}
                    blockquote{margin:14px 0;padding:10px 14px;border-left:4px solid #0081ff;background:#f6f8fa;color:#555;border-radius:4px;}
                    code{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;background:#f6f8fa;color:#d6336c;padding:2px 5px;border-radius:4px;font-size:14px;}
                    pre{background:#282c34;color:#f8f8f2;border-radius:6px;padding:13px;overflow:auto;margin:14px 0;}
                    pre code{background:transparent;color:inherit;padding:0;}
                    table{width:100%;border-collapse:collapse;display:block;overflow-x:auto;margin:14px 0;}
                    th,td{border:1px solid #e5e5e5;padding:8px 10px;}
                    th{background:#f7f7f7;}
                    .hide-ok{width:100%;padding:15px;background:#dff0d8;color:#3c763d;border:1px solid #d6e9c6;box-sizing:border-box;border-radius:5px;word-break:break-all;margin:14px 0;}
                    .hide-lock{width:100%;padding:15px;background:#f2dede;color:#a94442;border:1px solid #ebccd1;box-sizing:border-box;border-radius:5px;margin:14px 0;}
                    .tImg{width:24px;height:24px;vertical-align:middle;}
                </style>
            </head>
            <body>$html</body>
            </html>
        """.trimIndent()
    }

    private fun normalizeHidden(raw: String, canReadHidden: Boolean): String {
        var text = raw
        if (canReadHidden) {
            text = text.replace("[hide]", "<div class=\"hide-ok\">")
                .replace("[/hide]", "</div>")
                .replace("{hide}", "<div class=\"hide-ok\">")
                .replace("{/hide}", "</div>")
        } else {
            text = text.replace(Regex("""\[hide][\s\S]*?\[/hide]"""), "<div class=\"hide-lock\">此内容需要评论后方可阅读！</div>")
                .replace(Regex("""\{hide}[\s\S]*?\{/hide}"""), "<div class=\"hide-lock\">此内容需要评论后方可阅读！</div>")
        }
        return text
    }
}
