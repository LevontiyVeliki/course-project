package com.example.taskplanner_new_1

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity  // kept for imports
import androidx.lifecycle.lifecycleScope
import com.example.taskplanner_new_1.api.RegisterRequest
import com.example.taskplanner_new_1.api.RetrofitClient
import com.example.taskplanner_new_1.databinding.ActivityRegisterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener { registerUser() }

        binding.tvGoToLogin.setOnClickListener {
            finish() // просто возвращаемся назад на LoginActivity
        }
    }

    private fun registerUser() {
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Валидация
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Заполните все поля")
            return
        }
        if (username.length < 3) {
            showError("Имя пользователя минимум 3 символа")
            return
        }
        if (password.length < 6) {
            showError("Пароль минимум 6 символов")
            return
        }

        setLoading(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.register(
                    RegisterRequest(username, email, password)
                )
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity, "Регистрация прошла успешно! Войдите в аккаунт.", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@RegisterActivity, LoginActivity::class.java).apply {
                            putExtra("registered_username", username)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val msg = try {
                            // Сервер возвращает {"error": "..."}
                            JSONObject(errorBody ?: "").getString("error")
                        } catch (e: Exception) {
                            when (response.code()) {
                                409 -> "Пользователь с таким именем уже существует"
                                else -> "Ошибка регистрации (код ${response.code()})"
                            }
                        }
                        showError(msg)
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) { showError("Нет подключения к серверу") }
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) { showError("Ошибка сервера: ${e.message()}") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("Ошибка: ${e.localizedMessage}") }
            } finally {
                withContext(Dispatchers.Main) { setLoading(false) }
            }
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.btnRegister.isEnabled = !loading
        binding.tvError.visibility = View.GONE
    }
}
