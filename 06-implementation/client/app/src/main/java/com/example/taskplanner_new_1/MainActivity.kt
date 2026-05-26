package com.example.taskplanner_new_1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity  // kept for imports
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.content.ContextCompat
import com.example.taskplanner_new_1.api.RetrofitClient
import com.example.taskplanner_new_1.auth.SessionManager
import com.example.taskplanner_new_1.data.TaskDatabaseHelper
import com.example.taskplanner_new_1.ui.FolderListFragment

class MainActivity : BaseActivity() {

    private lateinit var sessionManager: SessionManager

    // Launcher for POST_NOTIFICATIONS permission (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission granted/denied — notifications work either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // Если не авторизован — отправляем на логин
        if (!sessionManager.isLoggedIn()) {
            goToLogin()
            return
        }

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Восстанавливаем токен в Retrofit (на случай перезапуска)
        RetrofitClient.token = sessionManager.getToken()

        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FolderListFragment())
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /** Показываем иконки в выпадающем меню (overflow). */
    @Suppress("RestrictedApi")
    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        (menu as? MenuBuilder)?.setOptionalIconsVisible(true)
        return super.onMenuOpened(featureId, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /** Нажатие на стрелку «Назад» в ActionBar — возвращаемся к предыдущему фрагменту. */
    override fun onSupportNavigateUp(): Boolean {
        return supportFragmentManager.popBackStackImmediate() || super.onSupportNavigateUp()
    }

    private fun logout() {
        // Wipe local SQLite without creating a default folder — the next login's
        // syncFromServer() will repopulate everything from the server.
        TaskDatabaseHelper(this).clearForUserSwitch()
        sessionManager.clearSession()
        RetrofitClient.token = null
        goToLogin()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
