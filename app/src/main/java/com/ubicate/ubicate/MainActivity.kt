package com.ubicate.ubicate

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ubicate.ubicate.databinding.ActivityMainBinding
import com.ubicate.ubicate.view.SatisfactionDialogFragment
import com.ubicate.ubicate.view.IncertidumbreDialogFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Crear una variable para manejar SharedPreferences
    private val sharedPreferences by lazy {
        getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Mostrar el formulario de incertidumbre si aún no se ha enviado
        if (!hasSubmittedIncertidumbre()) {
            showIncertidumbreForm()
        }
    }

    @Deprecated("onBackPressed is deprecated", ReplaceWith("onBackPressedDispatcher.onBackPressed()"))
    override fun onBackPressed() {
        // Mostrar el formulario de satisfacción cuando el usuario intente retroceder
        val dialog = SatisfactionDialogFragment()
        dialog.isCancelable = false  // No permitir cerrar el diálogo hasta enviar la respuesta
        dialog.show(supportFragmentManager, "SatisfactionDialog")

        // Evitar que la aplicación se cierre automáticamente
        // Aquí no llamamos a super.onBackPressed() para evitar que la actividad se cierre
    }

    // Método para mostrar el formulario de incertidumbre
    private fun showIncertidumbreForm() {
        val dialog = IncertidumbreDialogFragment()
        dialog.isCancelable = false  // No permitir cerrar el diálogo hasta que el usuario lo envíe
        dialog.show(supportFragmentManager, "IncertidumbreDialog")
    }

    // Verifica si el usuario ya envió el formulario de incertidumbre
    private fun hasSubmittedIncertidumbre(): Boolean {
        return sharedPreferences.getBoolean("incertidumbreSubmitted", false)
    }

    // Marcar que el formulario de incertidumbre fue enviado
    fun markIncertidumbreAsSubmitted() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("incertidumbreSubmitted", true)
        editor.apply()
    }
}
