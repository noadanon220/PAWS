package com.danono.paws

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // If already logged in, go to main screen
        if (auth.currentUser != null) {
            goToMain()
        }

        val emailField = findViewById<TextInputEditText>(R.id.login_EDT_email)
        val passwordField = findViewById<TextInputEditText>(R.id.login_EDT_password)
        val loginButton = findViewById<MaterialButton>(R.id.login_BTN_signIn)

        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            goToMain()
                        } else {
                            Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Optional: go to registration screen
        findViewById<android.widget.TextView>(R.id.login_txt_register).setOnClickListener {
            Toast.makeText(this, "Registration screen coming soon", Toast.LENGTH_SHORT).show()
            // startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
