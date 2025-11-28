package com.example.enhancedtodo.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowManager
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.example.enhancedtodo.R
import com.example.enhancedtodo.databinding.ActivitySignInBinding
import com.example.enhancedtodo.firebase.FirestoreClass
import com.example.enhancedtodo.models.User

class SignInActivity : BaseActivity() {

    private lateinit var binding: ActivitySignInBinding

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setUpActionBar()

    }

    fun signInSuccess(loggedInUser: User){
        hideProgressDialog()
        startActivity(Intent(this@SignInActivity, MainActivity::class.java))
        finish()
    }

    private fun setUpActionBar() {
        setSupportActionBar(binding.toolbarSignInActivity)

        val actionbar = supportActionBar
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true)
            actionbar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
        }

        binding.toolbarSignInActivity.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.btnSignInSignin.setOnClickListener {
            signInRegisteredUser()
        }
    }

    private fun signInRegisteredUser() {
        val email: String = binding.etEmail.text.toString().trim { it <= ' ' }
        val password: String = binding.etPassword.text.toString().trim { it <= ' ' }

        if (validateForm(email, password)) {
            showProgressDialog(resources.getString(R.string.please_wait))
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this@SignInActivity) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        FirestoreClass().loadUserData(this@SignInActivity)
                    } else {
                        Toast.makeText(
                            baseContext,
                            "Authentication Failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

    }

    private fun validateForm(email: String, password: String): Boolean {
        return when {

            TextUtils.isEmpty(email) -> {
                showErrorSnackBar("Please enter a email")
                false
            }

            TextUtils.isEmpty(password) -> {
                showErrorSnackBar("Please enter a password")
                false
            }
            else -> true
        }
    }

}