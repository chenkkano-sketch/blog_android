package cc.kkano.blog.navigation

data class FeatureSpec(
    val route: String,
    val title: String,
    val section: String,
    val endpoint: String? = null,
    val mode: FeatureMode = FeatureMode.LIST,
    val description: String = "",
)

enum class FeatureMode {
    LIST,
    ARTICLE_LIST,
    FORM_DYNAMIC,
    FORM_ARTICLE,
    FORM_COMMENT,
    FORM_FRIEND_LINK,
    FORM_PROFILE,
    FORM_EMAIL,
    FORM_PASSWORD,
    FORM_REGISTER,
    FORM_FORGOT_PASSWORD,
    FORM_QR_CONFIRM,
    LOCAL_SETTINGS,
}
