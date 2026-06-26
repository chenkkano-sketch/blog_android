# blog_android 原生 Android 重构说明

## 项目定位

`blog_android` 是 `blog_uniapp` 的原生 Android 重构版，只保留博客体系。

历史 RuleApp 能力已经遗弃，不再保留商城、支付、广告、签到、充值、提现、订单、会员、卡密、邀请码等入口。

## 技术栈

- Kotlin
- Material Components
- RecyclerView
- OkHttp
- Gson
- MVVM

## 后端接口

默认接口基址：

```text
https://workapi.kkano.cc/
```

核心接口覆盖：

- 文章：`/api/articles`
- 分类：`/api/article-categories`
- 标签：`/api/tags`
- 动态：`/api/dynamics`
- 评论：`/api/comments`
- 媒体：`/api/media`
- 友链：`/api/friend-links`
- 足迹：`/api/footprint`
- 用户：`/api/user/info`
- 管理：`/api/admin/*`、`/api/dashboard/*`

## 页面完成情况

博客体系页面已全部接入原生路由表 `NativeRouteRegistry`，当前共 55 个页面/入口：

- 主导航：首页、动态、工具、账户
- 创作：发布文章、发布动态、发表评论
- 内容：文章列表、文章详情、搜索、分类、标签、推荐、评论、图库、相册、项目、动态媒体
- 账户：登录、注册、忘记密码、个人信息、头像、邮箱修改、修改密码、媒体资源、扫码确认、评论留言、用户协议、账号绑定
- 管理：管理中心、文章管理、评论管理、用户管理、分类管理、媒体管理、足迹管理、友链管理、操作日志

工具页中的“打开全部原生页面”可以检查完整页面入口。

## 构建验证

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat lintDebug
```

## 版本

当前发布版本：`1.0.0`
