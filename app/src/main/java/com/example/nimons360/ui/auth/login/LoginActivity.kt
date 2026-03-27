package com.example.nimons360.ui.auth.login

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.nimons360.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }
}
