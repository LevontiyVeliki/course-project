package com.example.taskplanner_new_1

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity  // kept for imports
import androidx.lifecycle.lifecycleScope
import com.example.taskplanner_new_1.api.LoginRequest
import com.example.taskplanner_new_1.api.RetrofitClient
import com.example.taskplanner_new_1.auth.SessionManager
import com.example.taskplanner_new_1.data.TaskDatabaseHelper
import com.example.taskplanner_new_1.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        sessionManager = SessionManager(this)

        // Если уже авторизован — сразу в главный экран
        if (sessionManager.isLoggedIn()) {
            RetrofitClient.token = sessionManager.getToken()
            goToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { login() }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun login() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            showError("Введите имя пользователя и пароль")
            return
        }

        setLoading(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.login(LoginRequest(username, password))
                if (response.isSuccessful) {
                    val jwt = response.body()
                    if (jwt != null) {
                        // Running on IO — safe to clear SQLite here before switching to Main.
                        // This wipes any stale data from a previous session so syncFromServer()
                        // can fill the DB from scratch after the user lands on MainActivity.
                        TaskDatabaseHelper(this@LoginActivity).clearForUserSwitch()
                        withContext(Dispatchers.Main) {
                            sessionManager.saveSession(jwt.token, jwt.id, jwt.username, jwt.email)
                            RetrofitClient.token = jwt.token
                            goToMain()
                        }
                    } else {
                        withContext(Dispatchers.Main) { showError("Пустой ответ от сервера") }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val msg = try {
                        JSONObject(errorBody ?: "").getString("error")
                    } catch (e: Exception) {
                        "Неверное имя пользователя или пароль"
                    }
                    withContext(Dispatchers.Main) { showError(msg) }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    showError("Нет подключения к серверу\n(${RetrofitClient.BASE_URL})\n${e.localizedMessage}")
                }
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    showError("Ошибка сервера: ${e.message()}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Неизвестная ошибка: ${e.localizedMessage}")
                }
            } finally {
                withContext(Dispatchers.Main) { setLoading(false) }
            }
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.tvError.visibility = View.GONE
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
