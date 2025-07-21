package com.ubicate.ubicate

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val splashTimeout: Long = 3000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        startEpicAnimations()

        Handler(mainLooper).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, splashTimeout)
    }

    private fun startEpicAnimations() {
        val logo = findViewById<ImageView>(R.id.logoSplash)
        val text = findViewById<TextView>(R.id.textSplash)

        val logoFadeIn = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f)
        val logoScaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0f, 1.3f, 1f)
        val logoScaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0f, 1.3f, 1f)
        val logoRotation = ObjectAnimator.ofFloat(logo, "rotation", 180f, 0f)

        val logoAnimSet = AnimatorSet()
        logoAnimSet.playTogether(logoFadeIn, logoScaleX, logoScaleY, logoRotation)
        logoAnimSet.duration = 1200
        logoAnimSet.interpolator = BounceInterpolator()

        val textFadeIn = ObjectAnimator.ofFloat(text, "alpha", 0f, 1f)
        val textSlideUp = ObjectAnimator.ofFloat(text, "translationY", 200f, 0f)
        val textRotation = ObjectAnimator.ofFloat(text, "rotation", -90f, 0f)
        val textScale = ObjectAnimator.ofFloat(text, "scaleX", 0.5f, 1.1f, 1f)

        val textAnimSet = AnimatorSet()
        textAnimSet.playTogether(textFadeIn, textSlideUp, textRotation, textScale)
        textAnimSet.duration = 1000
        textAnimSet.startDelay = 600
        textAnimSet.interpolator = OvershootInterpolator()

        val logoPulse = ObjectAnimator.ofFloat(logo, "scaleX", 1f, 1.1f, 1f)
        val logoPulseY = ObjectAnimator.ofFloat(logo, "scaleY", 1f, 1.1f, 1f)

        val pulseSet = AnimatorSet()
        pulseSet.playTogether(logoPulse, logoPulseY)
        pulseSet.duration = 400
        pulseSet.startDelay = 2000

        logoAnimSet.start()
        textAnimSet.start()
        pulseSet.start()
    }
}