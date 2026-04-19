package com.example.nimons360

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.nimons360.data.local.preference.TokenPreference
import com.example.nimons360.ui.auth.login.LoginActivity
import com.example.nimons360.utils.NetworkMonitor
import com.example.nimons360.utils.NoConnectionDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.FloatingWindow
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.ui.NavigationUI
import android.view.MenuItem
import android.widget.TextView
import androidx.core.view.forEach
import androidx.navigation.NavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var tokenPreference: TokenPreference

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var userRepository: com.example.nimons360.data.repository.UserRepository

    private var noConnectionDialog: NoConnectionDialog? = null
    private var wasDisconnected = false

    private var defaultBottomNavIconTint: ColorStateList? = null
    private var defaultBottomNavTextColor: ColorStateList? = null
    private var defaultBottomNavActiveIndicator = true
    private var suppressBottomNavItemCallback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cek apakah user sudah login
        if (tokenPreference.getToken().isNullOrEmpty()) {
            // Jika belum login, arahkan ke LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav) // menggunakan non-binding karena langsung via layout
        defaultBottomNavIconTint = bottomNav.itemIconTintList
        defaultBottomNavTextColor = bottomNav.itemTextColor
        defaultBottomNavActiveIndicator = bottomNav.isItemActiveIndicatorEnabled

        bottomNav.setOnItemSelectedListener { item ->
            if (suppressBottomNavItemCallback) return@setOnItemSelectedListener true
            if (handleBottomNavWhileOnProfile(navController, item)) {
                true
            } else {
                NavigationUI.onNavDestinationSelected(item, navController)
            }
        }
        bottomNav.setOnItemReselectedListener { item ->
            if (navController.currentDestination?.id == R.id.profileFragment) {
                handleBottomNavWhileOnProfile(navController, item)
            }
        }

        // network sensing
        observeNetwork()

        // load user ava
        loadUserAvatar()

        val title = findViewById<TextView>(R.id.tv_top_bar_title)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            title.text = destination.label?.toString() ?: "Nimons360"
            if (destination is FloatingWindow) return@addOnDestinationChangedListener

            if (destination.id == R.id.profileFragment) {
                bottomNav.isItemActiveIndicatorEnabled = false
                val muted = MaterialColors.getColor(
                    bottomNav,
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                    Color.GRAY
                )
                val mutedList = ColorStateList.valueOf(muted)
                bottomNav.itemIconTintList = mutedList
                bottomNav.itemTextColor = mutedList
            } else {
                bottomNav.isItemActiveIndicatorEnabled = defaultBottomNavActiveIndicator
                bottomNav.itemIconTintList = defaultBottomNavIconTint
                bottomNav.itemTextColor = defaultBottomNavTextColor
                var matchedTabId: Int? = null
                bottomNav.menu.forEach { item ->
                    val checked = destination.hierarchy.any { it.id == item.itemId }
                    item.isChecked = checked
                    if (checked) matchedTabId = item.itemId
                }

                val targetTabId = matchedTabId ?: navController.graph.startDestinationId
                if (bottomNav.selectedItemId != targetTabId) {
                    suppressBottomNavItemCallback = true
                    try {
                        bottomNav.selectedItemId = targetTabId
                    } finally {
                        suppressBottomNavItemCallback = false
                    }
                }
            }
        }

        val avatar = findViewById<TextView>(R.id.tv_top_bar_avatar)
        avatar.setOnClickListener {
            navController.navigate(R.id.profileFragment)
        }
    }

    private fun handleBottomNavWhileOnProfile(navController: NavController, item: MenuItem): Boolean {
        if (navController.currentDestination?.id != R.id.profileFragment) return false
        if (item.itemId == R.id.homeFragment) {
            navController.popBackStack(R.id.homeFragment, false)
        } else {
            navController.popBackStack()
            if (navController.currentDestination?.id != item.itemId) {
                NavigationUI.onNavDestinationSelected(item, navController)
            }
        }
        return true
    }

    private fun observeNetwork() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkMonitor.isConnected.collect { isConnected ->
                    if (isConnected) {
                        noConnectionDialog?.let { dialog ->
                            if (dialog.isShowing) {
                                dialog.dismiss()
                                if (wasDisconnected) {
                                    Snackbar.make(
                                        findViewById(android.R.id.content),
                                        "Connected",
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        wasDisconnected = false
                    } else {
                        wasDisconnected = true
                        showNoConnectionDialog()
                    }
                }
            }
        }
    }

    private fun loadUserAvatar() {
        lifecycleScope.launch {
            when (val res = userRepository.getProfile()) {
                is com.example.nimons360.utils.Result.Success -> {
                    val name = res.data?.fullName
                    val initials = getInitials(name)
                    findViewById<TextView>(R.id.tv_top_bar_avatar).text = initials
                }
                else -> { }
            }
        }
    }

    private fun getInitials(name: String?): String {
        if (name.isNullOrBlank()) return "?"
        return name.trim().split(" ")
            .filter { it.isNotEmpty() }
            .map { it[0].uppercaseChar() }
            .take(2)
            .joinToString("")
    }

    fun showNoConnectionDialog() {
        if (noConnectionDialog?.isShowing == true) return
        if (isFinishing || isDestroyed) return
        noConnectionDialog = NoConnectionDialog(this)
        noConnectionDialog?.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        noConnectionDialog?.dismiss()
        noConnectionDialog = null
    }
}
