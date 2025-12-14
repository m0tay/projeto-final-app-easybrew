package pm.easybrew

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pm.easybrew.api.RetrofitClient
import pm.easybrew.api.RetrofitClient.getMessage
import pm.easybrew.objects.ValidateTokenRequest

class SplashScreen : AppCompatActivity() {

    companion object {
        private val TAG = SplashScreen::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val sharedPref = getSharedPreferences("easybrew_session", MODE_PRIVATE)
        val token = sharedPref.getString("token", null)

        if (token != null) {
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.validateToken(ValidateTokenRequest(token))
                    val intent = if (response.isSuccessful) {
                        Log.i(TAG, getMessage(response))
                        Intent(applicationContext, MainActivity::class.java)
                    } else {
                        Log.w(TAG, getMessage(response))
                        Intent(applicationContext, RegisterLoginActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, e.message ?: "Unknown error", e)
                    startActivity(Intent(applicationContext, RegisterLoginActivity::class.java))
                    finish()
                }
            }
        } else {
            Log.i(TAG, "No token found")
            startActivity(Intent(applicationContext, RegisterLoginActivity::class.java))
            finish()
        }

    }
}
