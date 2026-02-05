package com.testershub.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.testershub.app.R
import com.testershub.app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (auth.currentUser != null) {
            startMainActivity()
            return
        }

        binding.btnGoogleSignIn.setOnClickListener {
            signIn()
        }
    }

    private fun signIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        startActivityForResult(googleSignInClient.signInIntent, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                e.printStackTrace()
                val message = when(e.statusCode) {
                    10 -> "Sign in failed (Error 10): This usually means your SHA-1 fingerprint is not added in Firebase Console or doesn't match this build."
                    else -> "Sign in failed: ${e.statusCode}"
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                saveUserToFirestore(result.user!!)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Firebase Auth Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveUserToFirestore(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        val userRef = db.collection("users").document(firebaseUser.uid)
        userRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val user = hashMapOf(
                    "userId" to firebaseUser.uid,
                    "name" to firebaseUser.displayName,
                    "email" to firebaseUser.email,
                    "profilePhoto" to firebaseUser.photoUrl.toString(),
                    "helpedCount" to 0,
                    "requestedCount" to 0
                )
                userRef.set(user)
                    .addOnSuccessListener { 
                        Toast.makeText(this, "Welcome to TestersHub!", Toast.LENGTH_SHORT).show()
                        startMainActivity() 
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save user: ${e.message}", Toast.LENGTH_LONG).show()
                        startMainActivity() // Still go to main activity even if DB save fails
                    }
            } else {
                startMainActivity()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Database Error: ${e.message}", Toast.LENGTH_LONG).show()
            startMainActivity()
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
