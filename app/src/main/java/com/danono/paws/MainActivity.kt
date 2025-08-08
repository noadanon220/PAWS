package com.danono.paws

import android.content.Intent
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.danono.paws.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // If the user is not logged in, redirect to LoginActivity
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Hide status bar completely
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 and above
            window.setDecorFitsSystemWindows(false)
            window.statusBarColor = Color.TRANSPARENT
        } else {
            // For older Android versions
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Define top-level destinations
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_reminders,
                R.id.navigation_dog_parks,
                R.id.navigation_settings
            )
        )

        // Connect BottomNavigationView with NavController
        navView.setupWithNavController(navController)

        // If the activity was launched after registration, navigate to the setup profile screen.
        // The intent extra is set by RegisterActivity to trigger this behaviour.
        if (intent.getBooleanExtra("navigateToSetupProfile", false)) {
            navController.navigate(R.id.setupProfileFragment)
        }
    }
}
