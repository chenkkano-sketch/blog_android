package cc.kkano.blog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import cc.kkano.blog.ui.account.AccountFragment
import cc.kkano.blog.ui.dynamics.DynamicsFragment
import cc.kkano.blog.ui.home.HomeFragment
import cc.kkano.blog.ui.tools.ToolsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> show(HomeFragment())
                R.id.navDynamics -> show(DynamicsFragment())
                R.id.navTools -> show(ToolsFragment())
                R.id.navAccount -> show(AccountFragment())
                else -> false
            }
        }

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.navHome
        }
    }

    private fun show(fragment: Fragment): Boolean {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        return true
    }
}
