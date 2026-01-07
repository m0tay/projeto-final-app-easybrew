package pm.easybrew

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
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
                        
                        // Atualizar token se retornou um novo
                        val newToken = response.body()?.jwt
                        if (newToken != null && newToken != token) {
                            sharedPref.edit { putString("token", newToken) }
                        }
                        
                        // Atualizar dados do usuário do novo token
                        val tokenToUse = newToken ?: token
                        val jwtPayload = RetrofitClient.getJWTPayload(tokenToUse)
                        if (jwtPayload != null) {
                            sharedPref.edit {
                                putString("user_id", jwtPayload["id"] as? String)
                                putString("balance", jwtPayload["balance"] as? String)
                                putString("first_name", jwtPayload["first_name"] as? String)
                            }
                        }
                        
                        Intent(applicationContext, MainActivity::class.java)
                    } else {
                        Log.w(TAG, getMessage(response))
                        // Limpar dados da sessão
                        sharedPref.edit { 
                            remove("token")
                            remove("user_id")
                            remove("balance")
                            remove("first_name")
                        }
                        Intent(applicationContext, RegisterLoginActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, e.message ?: "Unknown error", e)
                    // Limpar dados da sessão em caso de erro
                    sharedPref.edit { 
                        remove("token")
                        remove("user_id")
                        remove("balance")
                        remove("first_name")
                    }
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
