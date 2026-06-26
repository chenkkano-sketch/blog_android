package cc.kkano.blog.navigation

import android.content.Context
import android.content.Intent
import cc.kkano.blog.ui.feature.FeatureFormActivity
import cc.kkano.blog.ui.feature.FeatureHubActivity
import cc.kkano.blog.ui.feature.FeatureInfoActivity
import cc.kkano.blog.ui.feature.GenericListActivity
import cc.kkano.blog.ui.login.LoginActivity

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
