package com.example.nimons360

import android.content.Intent
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
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import android.widget.TextView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var tokenPreference: TokenPreference

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    private var noConnectionDialog: NoConnectionDialog? = null
    private var wasDisconnected = false

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
        bottomNav.setupWithNavController(navController)

        // network sensing
        observeNetwork()

        val title = findViewById<TextView>(R.id.tv_top_bar_title)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            title.text = destination.label?.toString() ?: "Nimons360"
        }

        val avatar = findViewById<TextView>(R.id.tv_top_bar_avatar)
        avatar.setOnClickListener {
            // TODO: navigate ke profileFragment
            // navContoller.navigate(R.id.profileFragment)
        }
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
