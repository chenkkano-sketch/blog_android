package cc.kkano.blog.navigation

import cc.kkano.blog.data.api.ApiRoutes

object NativeRouteRegistry {
    val features: List<FeatureSpec> = listOf(
        FeatureSpec("pages/home/home", "首页", "主导航", ApiRoutes.ARTICLES, FeatureMode.ARTICLE_LIST, "最新文章、推荐内容与文章详情"),
        FeatureSpec("pages/home/find", "动态", "主导航", ApiRoutes.DYNAMICS, FeatureMode.LIST, "博客动态流"),
        FeatureSpec("pages/home/dynamicsadd", "发布动态", "创作", ApiRoutes.DYNAMICS, FeatureMode.FORM_DYNAMIC, "发布图文动态"),
        FeatureSpec("pages/home/tool", "工具", "主导航", description = "功能入口"),
        FeatureSpec("pages/home/user", "账户", "主导航", description = "用户中心"),
        FeatureSpec("pages/home/splash", "开屏页", "主导航", mode = FeatureMode.LOCAL_SETTINGS, description = "原生启动页由系统 Splash 承担，已完成适配"),

        FeatureSpec("pages/user/login", "登录", "账户", ApiRoutes.LOGIN, FeatureMode.FORM_REGISTER, "账号登录已迁移到独立登录页"),
        FeatureSpec("pages/user/register", "注册", "账户", ApiRoutes.REGISTER, FeatureMode.FORM_REGISTER, "邮箱验证码注册"),
        FeatureSpec("pages/user/foget", "忘记密码", "账户", ApiRoutes.FORGOT_PASSWORD, FeatureMode.FORM_FORGOT_PASSWORD, "找回账号密码"),
        FeatureSpec("pages/user/useredit", "个人信息", "账户", ApiRoutes.USER_PROFILE, FeatureMode.FORM_PROFILE, "修改昵称、手机、头像地址"),
        FeatureSpec("pages/user/avatar", "头像", "账户", ApiRoutes.USER_PROFILE, FeatureMode.FORM_PROFILE, "通过个人信息页更新头像地址"),
        FeatureSpec("pages/user/mailedit", "邮箱修改", "账户", "api/user/email/verify", FeatureMode.FORM_EMAIL, "发送验证码并绑定新邮箱"),
        FeatureSpec("pages/user/password", "修改密码", "账户", ApiRoutes.USER_PASSWORD, FeatureMode.FORM_PASSWORD, "修改登录密码"),
        FeatureSpec("pages/user/media", "社交媒体", "账户", description = "QQ 群、官网和 GitHub"),
        FeatureSpec("pages/user/scan", "扫码确认", "账户", "api/login/qr/confirm", FeatureMode.FORM_QR_CONFIRM, "网页扫码登录确认"),
        FeatureSpec("pages/user/inbox", "评论管理", "账户", ApiRoutes.COMMENTS, FeatureMode.LIST, "文章评论、留言板与动态评论管理"),
        FeatureSpec("pages/user/userpost", "我的文章", "账户", ApiRoutes.ARTICLES, FeatureMode.ARTICLE_LIST, "文章列表"),
        FeatureSpec("pages/user/post", "发布文章", "创作", ApiRoutes.ARTICLES, FeatureMode.FORM_ARTICLE, "发布博客文章"),
        FeatureSpec("pages/user/friendlink-manage", "友链管理", "管理", ApiRoutes.FRIEND_LINKS_MANAGE, FeatureMode.LIST, "友链申请与审核"),
        FeatureSpec("pages/user/footprint-manage", "足迹管理", "管理", ApiRoutes.FOOTPRINT_MANAGE, FeatureMode.LIST, "全部、已发布、草稿、搜索、新增、编辑与删除"),
        FeatureSpec("pages/user/setup", "系统设置", "账户", mode = FeatureMode.LOCAL_SETTINGS, description = "本地设置"),
        FeatureSpec("pages/user/local-library", "本地资源库", "账户", ApiRoutes.MEDIA, FeatureMode.LIST, "媒体库"),
        FeatureSpec("pages/user/password-manager", "密码管理", "账户", ApiRoutes.USER_PASSWORD, FeatureMode.FORM_PASSWORD, "密码修改"),
        FeatureSpec("pages/user/usercomments", "我的评论", "账户", ApiRoutes.COMMENTS, FeatureMode.LIST, "我的评论与回复"),
        FeatureSpec("pages/user/agreement", "用户协议", "账户", mode = FeatureMode.LOCAL_SETTINGS, description = "协议说明"),
        FeatureSpec("pages/user/userbind", "账号绑定", "账户", "api/user/devices", FeatureMode.LIST, "登录设备与绑定状态"),
        FeatureSpec("pages/user/userlist", "用户管理", "管理", ApiRoutes.USERS, FeatureMode.LIST, "用户搜索、编辑与删除"),

        FeatureSpec("pages/contents/comments", "评论管理", "内容", ApiRoutes.COMMENTS, FeatureMode.LIST, "文章评论、留言板与动态评论管理"),
        FeatureSpec("pages/contents/commentsadd", "发表评论", "内容", ApiRoutes.COMMENTS, FeatureMode.FORM_COMMENT, "发布评论或留言"),
        FeatureSpec("pages/contents/contentlist", "文章列表", "内容", ApiRoutes.ARTICLES, FeatureMode.ARTICLE_LIST, "文章列表"),
        FeatureSpec("pages/contents/imagetoday", "图库", "内容", ApiRoutes.MEDIA, FeatureMode.LIST, "博客图库与媒体图片"),
        FeatureSpec("pages/contents/info", "文章详情", "内容", ApiRoutes.ARTICLES, FeatureMode.ARTICLE_LIST, "文章详情由详情页承接"),
        FeatureSpec("pages/contents/alltag", "全部标签", "内容", ApiRoutes.TAGS, FeatureMode.LIST, "标签列表"),
        FeatureSpec("pages/contents/randlist", "随机阅读", "内容", ApiRoutes.ARTICLES, FeatureMode.ARTICLE_LIST, "文章阅读"),
        FeatureSpec("pages/contents/allcategory", "全部分类", "内容", ApiRoutes.ARTICLE_CATEGORIES, FeatureMode.LIST, "分类列表"),
        FeatureSpec("pages/contents/metas", "分类标签", "内容", ApiRoutes.ARTICLE_CATEGORIES, FeatureMode.LIST, "分类列表"),
        FeatureSpec("pages/contents/recommend", "推荐文章", "内容", ApiRoutes.ARTICLES, FeatureMode.ARTICLE_LIST, "推荐文章"),
        FeatureSpec("pages/contents/search", "搜索", "内容", ApiRoutes.SEARCH_UNIAPP, FeatureMode.LIST, "全局搜索"),
        FeatureSpec("pages/contents/userinfo", "用户信息", "内容", ApiRoutes.USERS, FeatureMode.LIST, "用户资料"),
        FeatureSpec("pages/contents/foreverblog", "项目", "内容", ApiRoutes.PROJECTS, FeatureMode.LIST, "博客项目与作品"),
        FeatureSpec("pages/contents/wallpaper", "相册", "内容", "api/albums", FeatureMode.LIST, "博客相册资源"),
        FeatureSpec("pages/contents/wallpaperInfo", "相册详情", "内容", "api/albums", FeatureMode.LIST, "相册详情"),
        FeatureSpec("pages/contents/myWallpaper", "媒体图片", "内容", ApiRoutes.MEDIA, FeatureMode.LIST, "媒体资源"),
        FeatureSpec("pages/contents/video", "动态媒体", "内容", ApiRoutes.DYNAMICS, FeatureMode.LIST, "动态媒体内容"),
        FeatureSpec("pages/contents/videoInfo", "动态详情", "内容", ApiRoutes.DYNAMICS, FeatureMode.LIST, "动态详情"),
        FeatureSpec("pages/contents/myVideo", "我的媒体", "内容", ApiRoutes.MEDIA, FeatureMode.LIST, "媒体资源"),

        FeatureSpec("pages/user/manage", "管理中心", "管理", "api/dashboard/overview", FeatureMode.LIST, "管理入口"),
        FeatureSpec("pages/manage/comments", "评论管理", "管理", ApiRoutes.COMMENTS, FeatureMode.LIST, "文章评论、留言板与动态评论管理"),
        FeatureSpec("pages/manage/contents", "文章管理", "管理", ApiRoutes.ADMIN_ARTICLES, FeatureMode.LIST, "待审核、已发布、搜索、审核、编辑与删除"),
        FeatureSpec("pages/manage/users", "用户管理", "管理", ApiRoutes.USERS, FeatureMode.LIST, "用户搜索、编辑与删除"),
        FeatureSpec("pages/manage/usersedit", "用户编辑", "管理", ApiRoutes.USERS, FeatureMode.LIST, "用户编辑"),
        FeatureSpec("pages/manage/metas", "分类标签", "管理", ApiRoutes.ARTICLE_CATEGORIES, FeatureMode.LIST, "分类与标签管理"),
        FeatureSpec("pages/manage/metasedit", "分类标签编辑", "管理", ApiRoutes.ARTICLE_CATEGORIES, FeatureMode.LIST, "分类与标签编辑"),
        FeatureSpec("pages/manage/media", "图床管理", "管理", ApiRoutes.MEDIA_MANAGE, FeatureMode.LIST, "图床统计、搜索、分类、上传、瀑布流与批量删除"),
        FeatureSpec("pages/manage/clean", "操作日志", "管理", "api/operation-logs", FeatureMode.LIST, "后台操作日志"),
    )

    val grouped: Map<String, List<FeatureSpec>> = features.groupBy { it.section }

    fun find(route: String): FeatureSpec? = features.firstOrNull { it.route == route }
}
