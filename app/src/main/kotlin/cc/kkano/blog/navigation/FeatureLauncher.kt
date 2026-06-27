package cc.kkano.blog.navigation

import android.content.Context
import android.content.Intent
import cc.kkano.blog.ui.feature.FeatureFormActivity
import cc.kkano.blog.ui.feature.FeatureHubActivity
import cc.kkano.blog.ui.feature.FeatureInfoActivity
import cc.kkano.blog.ui.feature.GenericListActivity
import cc.kkano.blog.ui.account.SocialMediaActivity
import cc.kkano.blog.ui.article.ArticleEditorActivity
import cc.kkano.blog.ui.comments.CommentManagerActivity
import cc.kkano.blog.ui.dynamics.DynamicEditorActivity
import cc.kkano.blog.ui.login.LoginActivity
import cc.kkano.blog.ui.login.QrScannerActivity
import cc.kkano.blog.ui.local.LocalLibraryActivity
import cc.kkano.blog.ui.media.MediaGalleryActivity
import cc.kkano.blog.ui.media.MediaManagerActivity
import cc.kkano.blog.ui.friend.FriendLinkManagerActivity
import cc.kkano.blog.ui.manage.ArticleManagerActivity
import cc.kkano.blog.ui.manage.FootprintManagerActivity
import cc.kkano.blog.ui.manage.MetaManagerActivity
import cc.kkano.blog.ui.manage.UserManagerActivity
import cc.kkano.blog.ui.search.SearchActivity

object FeatureLauncher {
    fun openHub(context: Context, section: String? = null) {
        context.startActivity(
            Intent(context, FeatureHubActivity::class.java)
                .putExtra(FeatureHubActivity.EXTRA_SECTION, section),
        )
    }

    fun open(context: Context, spec: FeatureSpec) {
        when {
            spec.route == "pages/user/login" -> context.startActivity(Intent(context, LoginActivity::class.java))
            spec.route == "pages/home/dynamicsadd" -> context.startActivity(Intent(context, DynamicEditorActivity::class.java))
            spec.route == "pages/user/post" -> context.startActivity(Intent(context, ArticleEditorActivity::class.java))
            spec.route == "pages/user/scan" -> context.startActivity(Intent(context, QrScannerActivity::class.java))
            spec.route == "pages/user/media" -> context.startActivity(Intent(context, SocialMediaActivity::class.java))
            spec.route == "pages/user/inbox" || spec.route == "pages/user/usercomments" || spec.route == "pages/contents/comments" || spec.route == "pages/manage/comments" -> {
                context.startActivity(Intent(context, CommentManagerActivity::class.java))
            }
            spec.route == "pages/user/friendlink-manage" -> context.startActivity(Intent(context, FriendLinkManagerActivity::class.java))
            spec.route == "pages/user/footprint-manage" -> context.startActivity(Intent(context, FootprintManagerActivity::class.java))
            spec.route == "pages/user/local-library" -> context.startActivity(Intent(context, LocalLibraryActivity::class.java))
            spec.route == "pages/manage/users" || spec.route == "pages/manage/usersedit" || spec.route == "pages/user/userlist" -> {
                context.startActivity(Intent(context, UserManagerActivity::class.java))
            }
            spec.route == "pages/manage/media" -> context.startActivity(Intent(context, MediaManagerActivity::class.java))
            spec.route == "pages/contents/imagetoday" || spec.route == "pages/contents/myWallpaper" || spec.route == "pages/contents/myVideo" -> {
                context.startActivity(Intent(context, MediaGalleryActivity::class.java).putExtra(MediaGalleryActivity.EXTRA_TITLE, spec.title))
            }
            spec.route == "pages/manage/contents" -> context.startActivity(Intent(context, ArticleManagerActivity::class.java))
            spec.route == "pages/manage/metas" || spec.route == "pages/manage/metasedit" -> {
                context.startActivity(Intent(context, MetaManagerActivity::class.java))
            }
            spec.route == "pages/contents/search" -> context.startActivity(Intent(context, SearchActivity::class.java))
            spec.mode.name.startsWith("FORM_") -> context.startActivity(formIntent(context, spec))
            spec.endpoint != null -> context.startActivity(listIntent(context, spec))
            else -> context.startActivity(infoIntent(context, spec))
        }
    }

    private fun listIntent(context: Context, spec: FeatureSpec): Intent {
        return Intent(context, GenericListActivity::class.java)
            .putExtra(GenericListActivity.EXTRA_TITLE, spec.title)
            .putExtra(GenericListActivity.EXTRA_ENDPOINT, spec.endpoint)
            .putExtra(GenericListActivity.EXTRA_MODE, spec.mode.name)
            .putExtra(GenericListActivity.EXTRA_ROUTE, spec.route)
    }

    private fun formIntent(context: Context, spec: FeatureSpec): Intent {
        return Intent(context, FeatureFormActivity::class.java)
            .putExtra(FeatureFormActivity.EXTRA_TITLE, spec.title)
            .putExtra(FeatureFormActivity.EXTRA_ENDPOINT, spec.endpoint)
            .putExtra(FeatureFormActivity.EXTRA_MODE, spec.mode.name)
            .putExtra(FeatureFormActivity.EXTRA_ROUTE, spec.route)
    }

    private fun infoIntent(context: Context, spec: FeatureSpec): Intent {
        return Intent(context, FeatureInfoActivity::class.java)
            .putExtra(FeatureInfoActivity.EXTRA_TITLE, spec.title)
            .putExtra(FeatureInfoActivity.EXTRA_DESCRIPTION, spec.description)
            .putExtra(FeatureInfoActivity.EXTRA_ROUTE, spec.route)
            .putExtra(FeatureInfoActivity.EXTRA_MODE, spec.mode.name)
    }
}
