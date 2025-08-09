package com.danono.paws

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val emailField = findViewById<TextInputEditText>(R.id.register_EDT_email)
        val passwordField = findViewById<TextInputEditText>(R.id.register_EDT_password)
        val registerButton = findViewById<MaterialButton>(R.id.register_BTN_register)

        registerButton.setOnClickListener {
            val email = emailField.text?.toString()?.trim() ?: ""
            val password = passwordField.text?.toString()?.trim() ?: ""

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // After a successful registration, navigate to MainActivity and then SetupProfileFragment
                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("navigateToSetupProfile", true)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "Registration failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Optional: back to login
        findViewById<android.widget.TextView>(R.id.register_txt_login).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
