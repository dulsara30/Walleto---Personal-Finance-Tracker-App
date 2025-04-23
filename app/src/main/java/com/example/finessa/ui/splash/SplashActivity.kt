package com.example.finessa.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.view.animation.Animation
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.finessa.MainActivity
import com.example.finessa.R
import com.example.finessa.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val SPLASH_DELAY: Long = 3000 // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Keep the splash screen visible for this Activity
        splashScreen.setKeepOnScreenCondition { true }
        
        // Start animations
        startAnimations()
        
        // Delay the transition to MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, SPLASH_DELAY)
    }

    private fun startAnimations() {
        // Logo animation
        val logoAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_up)
        binding.splashLogo.startAnimation(logoAnimation)

        // App name animation
        val appNameAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        appNameAnimation.startOffset = 500 // Start after logo animation
        appNameAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                binding.splashAppName.alpha = 1f
            }
            override fun onAnimationEnd(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
        })
        binding.splashAppName.startAnimation(appNameAnimation)

        // Slogan animation
        val sloganAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        sloganAnimation.startOffset = 1000 // Start after app name animation
        sloganAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                binding.splashSlogan.alpha = 1f
            }
            override fun onAnimationEnd(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
        })
        binding.splashSlogan.startAnimation(sloganAnimation)
    }
}