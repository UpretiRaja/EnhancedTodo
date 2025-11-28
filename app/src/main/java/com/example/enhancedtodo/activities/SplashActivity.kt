package com.example.enhancedtodo.activities

import android.content.Intent
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import com.example.enhancedtodo.R
import com.example.enhancedtodo.databinding.ActivitySplashBinding
import com.example.enhancedtodo.firebase.FirestoreClass

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val typeFace: Typeface = Typeface.createFromAsset(assets,"carbon bl.ttf")
        binding.tvAppName.typeface = typeFace

        Handler().postDelayed({

            var currentUserID = FirestoreClass().getCurrentUserId()

            if(currentUserID.isNotEmpty()){
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            }else{
                startActivity(Intent(this@SplashActivity, IntroActivity::class.java))
            }

            finish()
        },2500)

    }
}