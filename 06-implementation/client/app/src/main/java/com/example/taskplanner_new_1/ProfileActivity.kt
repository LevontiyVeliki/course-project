package com.example.taskplanner_new_1

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.taskplanner_new_1.api.ChangePasswordRequest
import com.example.taskplanner_new_1.api.RetrofitClient
import com.example.taskplanner_new_1.api.UpdateProfileRequest
import com.example.taskplanner_new_1.api.UserProfileResponse
import com.example.taskplanner_new_1.auth.SessionManager
import com.example.taskplanner_new_1.data.TaskDatabaseHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : BaseActivity() {

    private lateinit var sessionManager: SessionManager

    // View-ссылки
    private lateinit var tvAvatar:          TextView
    private lateinit var ivAvatar:          ShapeableImageView
    private lateinit var avatarContainer:   FrameLayout
    private lateinit var tvUsername:        TextView
    private lateinit var tvRoleBadge:       TextView
    private lateinit var tvEmail:           TextView
    private lateinit var etFullName:        EditText
    private lateinit var tvCreatedAt:       TextView
    private lateinit var tvTaskCount:       TextView
    private lateinit var btnSaveProfile:     MaterialButton
    private lateinit var btnChangePassword: MaterialButton

    // Gallery picker — no permissions needed with GetContent()
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { copyAndShowAvatar(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        sessionManager = SessionManager(this)

        // Скрываем системный ActionBar — тулбар встроен в лейаут
        supportActionBar?.hide()

        // Настраиваем встроенный тулбар с кнопкой «Назад»
        val toolbar = findViewById<MaterialToolbar>(R.id.profile_toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        tvAvatar        = findViewById(R.id.tv_avatar)
        ivAvatar        = findViewById(R.id.iv_avatar)
        avatarContainer = findViewById(R.id.avatar_container)
        tvUsername      = findViewById(R.id.tv_username)
        tvRoleBadge     = findViewById(R.id.tv_role_badge)
        tvEmail         = findViewById(R.id.tv_email)
        etFullName      = findViewById(R.id.et_full_name)
        tvCreatedAt     = findViewById(R.id.tv_created_at)
        tvTaskCount     = findViewById(R.id.tv_task_count)
        btnSaveProfile     = findViewById(R.id.btn_save_profile)
        btnChangePassword  = findViewById(R.id.btn_change_password)

        // Open gallery on avatar tap
        avatarContainer.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // Restore previously saved avatar from internal storage
        restoreAvatarFromDisk()

        // Кнопка внешнего вида
        findViewById<MaterialButton>(R.id.btn_appearance).setOnClickListener {
            startActivity(Intent(this, AppearanceActivity::class.java))
        }

        // Сразу показываем кэшированные данные из сессии, пока грузится сервер
        showCached()

        // Загружаем полный профиль с сервера
        loadProfileFromServer()

        // Кнопка смены пароля
        btnChangePassword.setOnClickListener { showChangePasswordDialog() }

        // Кнопка сохранения полного имени
        btnSaveProfile.setOnClickListener {
            val newName = etFullName.text.toString().trim()
            saveFullName(newName)
        }

        // Кнопка выхода
        findViewById<MaterialButton>(R.id.btn_logout).setOnClickListener {
            TaskDatabaseHelper(this).clearForUserSwitch()
            sessionManager.clearSession()
            RetrofitClient.token = null
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
        }
    }

    /**
     * Called when the user picks an image from the gallery.
     * Copies the bitmap into app-internal storage (avatar.jpg) so it survives
     * app restarts and revoking of gallery URI permissions.
     */
    private fun copyAndShowAvatar(uri: Uri) {
        try {
            val bitmap = decodeBitmapFromUri(uri)
            // Save to internal storage — this path is always accessible by the app
            val file = File(filesDir, "avatar.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            sessionManager.saveAvatarUri(file.absolutePath)
            displayAvatarBitmap(bitmap)
        } catch (_: Exception) {
            Toast.makeText(this, "Не удалось загрузить изображение", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Loads the previously saved avatar from app-internal storage on Activity start.
     */
    private fun restoreAvatarFromDisk() {
        val path = sessionManager.getAvatarUri() ?: return
        try {
            val file = File(path)
            if (!file.exists()) return
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
            displayAvatarBitmap(bitmap)
        } catch (_: Exception) { /* silently ignore — initials fallback stays visible */ }
    }

    private fun decodeBitmapFromUri(uri: Uri): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }

    private fun displayAvatarBitmap(bitmap: Bitmap) {
        ivAvatar.setImageBitmap(bitmap)
        ivAvatar.visibility = View.VISIBLE
        tvAvatar.visibility = View.GONE
    }

    private fun showCached() {
        val username  = sessionManager.getUsername() ?: "Пользователь"
        val email     = sessionManager.getEmail() ?: ""
        val fullName  = sessionManager.getFullName()
        val createdAt = sessionManager.getCreatedAt()
        val role      = sessionManager.getRole()

        val letter = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        tvAvatar.text    = letter
        tvUsername.text  = username
        tvEmail.text     = email.ifEmpty { "—" }

        if (!fullName.isNullOrBlank())  etFullName.setText(fullName)
        if (!createdAt.isNullOrBlank()) tvCreatedAt.text = createdAt
        if (!role.isNullOrBlank())
            tvRoleBadge.text = if (role == "ADMIN") "Администратор" else "Пользователь"

        // Количество задач — всегда из локальной БД
        val localCount = TaskDatabaseHelper(this).getAllTasks().size
        tvTaskCount.text = localCount.toString()
    }

    private fun loadProfileFromServer() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getProfile()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        response.body()?.let { applyProfile(it) }
                    }
                    // При ошибке просто остаются кэшированные данные
                }
            } catch (_: Exception) {
                // Нет сети — показываем то, что есть в кэше
            }
        }
    }

    private fun saveFullName(fullName: String) {
        btnSaveProfile.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.updateMyProfile(
                    UpdateProfileRequest(fullName)
                )
                withContext(Dispatchers.Main) {
                    btnSaveProfile.isEnabled = true
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Имя сохранено",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Кэшируем новое имя
                        sessionManager.saveFullName(fullName)
                        // Обновляем только аватар — username (логин) не меняем!
                        if (fullName.isNotEmpty()) {
                            tvAvatar.text = fullName.first().uppercaseChar().toString()
                        }
                    } else {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Ошибка сохранения (${response.code()})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnSaveProfile.isEnabled = true
                    Toast.makeText(
                        this@ProfileActivity,
                        "Нет подключения к серверу",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun applyProfile(p: UserProfileResponse) {
        val letter = p.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        tvAvatar.text    = letter
        tvUsername.text  = p.username
        tvRoleBadge.text = if (p.role == "ADMIN") "Администратор" else "Пользователь"
        tvEmail.text     = p.email.ifEmpty { "—" }
        etFullName.setText(if (!p.fullName.isNullOrBlank()) p.fullName else "")
        tvCreatedAt.text = p.createdAt.ifEmpty { "—" }

        // Количество задач — из локальной БД (актуальнее, чем серверный счётчик)
        val localCount = TaskDatabaseHelper(this).getAllTasks().size
        tvTaskCount.text = localCount.toString()

        // Кэшируем данные профиля на случай офлайна
        if (!p.fullName.isNullOrBlank())  sessionManager.saveFullName(p.fullName!!)
        if (p.createdAt.isNotBlank())     sessionManager.saveCreatedAt(p.createdAt)
        sessionManager.saveRole(p.role)
    }

    private fun showChangePasswordDialog() {
        // Поле для нового пароля
        val etPassword = EditText(this).apply {
            hint = "Новый пароль"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val etConfirm = EditText(this).apply {
            hint = "Повторите пароль"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val px = (20 * resources.displayMetrics.density).toInt()
            setPadding(px, 0, px, 0)
            addView(etPassword)
            addView(etConfirm)
        }

        AlertDialog.Builder(this)
            .setTitle("Изменить пароль")
            .setView(container)
            .setPositiveButton("Сохранить") { _, _ ->
                val pass    = etPassword.text.toString().trim()
                val confirm = etConfirm.text.toString().trim()
                when {
                    pass.length < 6 ->
                        Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
                    pass != confirm ->
                        Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                    else -> doChangePassword(pass)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun doChangePassword(newPassword: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.changePassword(
                    ChangePasswordRequest(newPassword)
                )
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ProfileActivity, "Пароль изменён", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileActivity,
                            "Ошибка (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Нет подключения к серверу", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Возвращаем системный ActionBar при закрытии экрана
        supportActionBar?.show()
    }
}
