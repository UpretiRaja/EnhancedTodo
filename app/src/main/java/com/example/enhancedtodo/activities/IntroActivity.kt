package com.example.enhancedtodo.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import com.example.enhancedtodo.R
import com.example.enhancedtodo.databinding.ActivityIntroBinding

class IntroActivity : BaseActivity() {

    private lateinit var binding: ActivityIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        binding.btnSignUpIntro.setOnClickListener {
            startActivity(Intent(this@IntroActivity, SignUpActivity::class.java))
        }

        binding.btnSignInIntro.setOnClickListener {
            startActivity(Intent(this@IntroActivity, SignInActivity::class.java))
        }

    }
}