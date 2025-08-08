package com.danono.paws

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

/**
 * A simple registration screen that allows a new user to sign up with a name,
 * email and password. After successfully creating the account the user's
 * display name is updated and the user is redirected into the main flow
 * with an intent flag instructing MainActivity to navigate to the profile
 * setup screen. Should registration fail an error message is shown.
 */
class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        // Obtain views from the layout
        val nameField = findViewById<TextInputEditText>(R.id.register_EDT_name)
        val emailField = findViewById<TextInputEditText>(R.id.register_EDT_email)
        val passwordField = findViewById<TextInputEditText>(R.id.register_EDT_password)
        val registerButton = findViewById<MaterialButton>(R.id.register_BTN_register)

        registerButton.setOnClickListener {
            val name = nameField.text?.toString()?.trim() ?: ""
            val email = emailField.text?.toString()?.trim() ?: ""
            val password = passwordField.text?.toString()?.trim() ?: ""

            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            // Update the Firebase user with the provided display name
                            user?.updateProfile(
                                UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build()
                            )?.addOnCompleteListener {
                                // Launch MainActivity and request navigation to the setup profile screen
                                val intent = Intent(this, MainActivity::class.java)
                                intent.putExtra("navigateToSetupProfile", true)
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            Toast.makeText(
                                this,
                                "Registration failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
