package com.xavi.imageia.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.xavi.imageia.MainActivity
import com.xavi.imageia.R
import com.xavi.imageia.databinding.FragmentVerificacionBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL




class verificacion : Fragment() {
    private var _binding: FragmentVerificacionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerificacionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editTextCode: EditText = binding.verificationCode
        val buttonVerify: Button = binding.confButton

        buttonVerify.setOnClickListener {
            val code = editTextCode.text.toString()
            if (code.isEmpty()) {
                Toast.makeText(requireContext(), "Introduce el código", Toast.LENGTH_SHORT).show()
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    val respuesta = JSONObject(verificar(code)).getString("status")
                    Log.i("mensaje", respuesta)
                    if (respuesta.equals("OK")){
                        saveUserData()
                    }
                }
            }
        }
    }
    private fun saveUserData() {
        val sharedPref = requireContext().getSharedPreferences("UserData", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("verified", true)
            apply()
        }

        Log.i("Verificacion", "Usuario verificado guardado en SharedPreferences")

        // Redirigir a MainActivity para que se actualice
        requireActivity().finish()
        requireActivity().startActivity(requireActivity().intent)
    }


    private suspend fun verificar(codigo: String): String {
        val sharedPref = requireContext().getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val phone = sharedPref.getString("phone", "") ?: ""
        val postUrl = "https://imagia1.ieti.site/api/usuaris/validar"
//        put("telefon", phone)
        val requestBody = JSONObject().apply {
            put("codi", codigo)
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = URL(postUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")

                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(requestBody.toString())
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                val responseMessage = connection.inputStream.bufferedReader().use { it.readText() }

                withContext(Dispatchers.Main) {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        Toast.makeText(requireContext(), "Verificado", Toast.LENGTH_SHORT).show()
                        Log.d("validar",responseCode.toString() +  responseMessage)
                    } else {
                        Toast.makeText(requireContext(), "Error: $responseMessage", Toast.LENGTH_SHORT).show()
                    }
                }
                responseMessage
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show()
                }
                Log.e("registerUser", "Error: ${e.message}", e)
                "Error"
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}