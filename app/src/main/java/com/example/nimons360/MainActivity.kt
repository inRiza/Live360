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
import com.example.nimons360.data.local.preference.NotificationPreference
import com.example.nimons360.data.repository.NotificationRepository
import com.google.firebase.messaging.FirebaseMessaging
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
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.forEach
import androidx.navigation.NavController
import com.bumptech.glide.Glide
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

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var notificationPreference: NotificationPreference

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

        registerFcmToken()

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

        // load user avatar & observe profile updates
        loadUserAvatar()
        observeProfileUpdates()

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

        val avatar = findViewById<android.view.View>(R.id.layout_top_bar_avatar)
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

    private fun observeProfileUpdates() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                com.example.nimons360.utils.ProfileUpdateBus.events.collect { updatedAt ->
                    loadUserAvatar(cacheBustKey = updatedAt)
                }
            }
        }
    }

    private fun loadUserAvatar(cacheBustKey: Long = 0L) {
        lifecycleScope.launch {
            when (val res = userRepository.getProfile()) {
                is com.example.nimons360.utils.Result.Success -> {
                    val name = res.data?.fullName
                    val initials = getInitials(name)
                    val tvAvatar = findViewById<TextView>(R.id.tv_top_bar_avatar)
                    val ivAvatar = findViewById<ImageView>(R.id.iv_top_bar_avatar)

                    val imageUrl = if (res.data?.profileImageUrl?.startsWith("http") == true) {
                        res.data.profileImageUrl
                    } else if (!res.data?.profileImageUrl.isNullOrBlank()) {
                        "${com.example.nimons360.utils.Constants.BASE_URL}${res.data.profileImageUrl}"
                    } else {
                        null
                    }

                    if (imageUrl != null) {
                        tvAvatar.visibility = android.view.View.GONE
                        ivAvatar.visibility = android.view.View.VISIBLE
                        Glide.with(this@MainActivity)
                            .load(imageUrl)
                            .circleCrop()
                            .skipMemoryCache(true)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                            .signature(com.bumptech.glide.signature.ObjectKey("$imageUrl-$cacheBustKey"))
                            .placeholder(R.drawable.bg_avatar_profile)
                            .error(R.drawable.bg_avatar_profile)
                            .into(ivAvatar)
                    } else {
                        tvAvatar.visibility = android.view.View.VISIBLE
                        ivAvatar.visibility = android.view.View.GONE
                        tvAvatar.text = initials
                    }

                    res.data?.id?.let { userId ->
                        tokenPreference.saveUserId(userId)
                    }
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

    private fun registerFcmToken() {
        // no subscribe jika user sudah matikan notifikasi
        if (!notificationPreference.isNotificationEnabled()) return

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                if (!token.isNullOrEmpty()) {
                    lifecycleScope.launch {
                        notificationRepository.subscribeToken(token)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        noConnectionDialog?.dismiss()
        noConnectionDialog = null
    }
}
