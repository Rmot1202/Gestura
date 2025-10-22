package com.example.gestura

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.gestura.util.ThemeHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1) Apply saved theme & language BEFORE UI inflates
        val prefs = getSharedPreferences("gestura_settings", MODE_PRIVATE)
        val savedTheme = prefs.getString("theme", "auto") ?: "auto"   // "light" | "dark" | "auto"
        ThemeHelper.apply(savedTheme)


        super.onCreate(savedInstanceState)

        // 2) NavHost + controller
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHostFragment.navController

        // 3) Start at Home if signed in; else Login
        navController.graph = navController.navInflater.inflate(R.navigation.nav_graph).apply {
            setStartDestination(
                if (auth.currentUser != null) R.id.aslFragment else R.id.loginFragment
            )
        }

        // 4) Bottom nav wiring
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        // Hide bottom bar on Login (so buttons aren't covered)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.visibility = if (destination.id == R.id.loginFragment) View.GONE else View.VISIBLE
        }

        bottomNav.setOnItemReselectedListener { /* no-op */ }
    }
}
