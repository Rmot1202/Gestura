package com.example.gestura

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHostFragment.navController

        // Start at Home if already signed in; else Login
        navController.graph = navController.navInflater.inflate(R.navigation.root_nav).apply {
            if (auth.currentUser != null) {
                setStartDestination(R.id.aslFragment)
            } else {
                setStartDestination(R.id.loginFragment)
            }
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        // Hide bottom bar on login
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.visibility = if (destination.id == R.id.loginFragment) View.GONE else View.VISIBLE
        }

        bottomNav.setOnItemReselectedListener { /* no-op */ }
    }
}
