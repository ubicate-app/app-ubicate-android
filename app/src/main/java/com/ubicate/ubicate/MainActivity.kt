package com.ubicate.ubicate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ubicate.ubicate.databinding.ActivityMainBinding
import com.ubicate.ubicate.repository.LocationSettingsChecker
import com.ubicate.ubicate.view.SatisfactionDialogFragment
import com.ubicate.ubicate.view.IncertidumbreDialogFragment
import com.ubicate.ubicate.view.MapaFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val sharedPreferences by lazy {
        getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        if (!hasSubmittedIncertidumbre()) {
            showIncertidumbreForm()
        }
    }

    @Deprecated("onBackPressed is deprecated", ReplaceWith("onBackPressedDispatcher.onBackPressed()"))
    override fun onBackPressed() {
        val dialog = SatisfactionDialogFragment()
        dialog.isCancelable = false
        dialog.show(supportFragmentManager, "SatisfactionDialog")

    }

    private fun showIncertidumbreForm() {
        val dialog = IncertidumbreDialogFragment()
        dialog.isCancelable = false
        dialog.show(supportFragmentManager, "IncertidumbreDialog")
    }

    private fun hasSubmittedIncertidumbre(): Boolean {
        return sharedPreferences.getBoolean("incertidumbreSubmitted", false)
    }

    fun markIncertidumbreAsSubmitted() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("incertidumbreSubmitted", true)
        editor.apply()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LocationSettingsChecker.REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                supportFragmentManager.fragments.forEach { fragment ->
                    if (fragment is MapaFragment) {
                        fragment.onLocationEnabled()
                    }
                }
            }
        }
    }

}
