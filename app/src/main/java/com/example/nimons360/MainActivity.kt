package com.example.nimons360

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.nimons360.data.local.preference.TokenPreference
import com.example.nimons360.ui.auth.login.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var tokenPreference: TokenPreference

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
}
