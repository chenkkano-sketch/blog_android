# blog_android

原生 Android 博客客户端，重构自 `blog_uniapp`，只保留博客体系。

## 功能范围

- 首页文章列表与文章详情
- 动态流与动态发布
- 评论列表与发表评论
- 分类、标签、搜索、项目、相册、媒体资源
- 登录、注册、忘记密码、个人资料、密码修改、邮箱修改
- 友链管理、足迹管理、媒体管理、文章管理、评论管理、用户管理
- Token 存储、刷新与 401 自动重试

历史 RuleApp 的商城、支付、广告、签到、充值、提现、订单、会员等入口已移除。

## 构建

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
.\gradlew.bat lintDebug
```

## 版本

当前版本：`1.0.0`

## 签名说明

仓库不包含正式 release keystore。GitHub Release 的测试 APK 可使用本机 debug keystore 签名生成；正式分发前请配置自己的正式签名。
