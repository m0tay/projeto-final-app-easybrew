package pm.easybrew

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import pm.easybrew.api.RetrofitClient
import pm.easybrew.objects.JWTResponse
import pm.easybrew.objects.LoginRequest
import pm.easybrew.objects.RegisterRequest
import pm.easybrew.objects.ValidateTokenRequest
import retrofit2.Response
import java.io.IOException

class RegisterLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_login)

        val sharedPref = getSharedPreferences("easybrew_session", MODE_PRIVATE)
        val token = sharedPref.getString("token", null)

        if (token != null) {
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.validateToken(ValidateTokenRequest(token))
                    if (response.isSuccessful) {
                        Toast.makeText(
                            applicationContext, getMessage(response), Toast.LENGTH_LONG
                        ).show()
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(applicationContext, getMessage(response), Toast.LENGTH_LONG)
                            .show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        applicationContext, e.message ?: "Unknown error", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            val email = findViewById<EditText>(R.id.editEmail).text.toString()
            val password = findViewById<EditText>(R.id.editPassword).text.toString()
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.register(RegisterRequest(email, password))
                    Toast.makeText(applicationContext, getMessage(response), Toast.LENGTH_LONG)
                        .show()
                } catch (e: IOException) {
                    Toast.makeText(applicationContext, "Network error", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        applicationContext, e.message ?: "Unknown error", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val email = findViewById<EditText>(R.id.editEmail).text.toString()
            val password = findViewById<EditText>(R.id.editPassword).text.toString()
            var jwt: String = ""
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.login(LoginRequest(email, password))
                    if (response.isSuccessful) {
                        Toast.makeText(applicationContext, getMessage(response), Toast.LENGTH_LONG)
                            .show()

                        val intent = Intent(applicationContext, MainActivity::class.java)
                        // intent.putExtra("jwt", response.body()?.jwt.toString())
                        val sharedPref = getSharedPreferences("easybrew_session", MODE_PRIVATE)
                        sharedPref.edit {
                            putString("token", response.body()?.jwt.toString())
                       }
                        startActivity(intent)
                    } else {
                        Toast.makeText(applicationContext, getMessage(response), Toast.LENGTH_LONG)
                            .show()
                    }
                } catch (e: IOException) {
                    Toast.makeText(applicationContext, "Network error", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        applicationContext, e.message ?: "Unknown error", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun getMessage(response: Response<JWTResponse>): String {
        return if (response.isSuccessful) {
            response.body()?.message?.takeIf { !it.isNullOrBlank() } ?: "OK"
        } else {
            val raw = response.errorBody()?.string().orEmpty()
            val parsed = runCatching {
                Gson().fromJson(
                    raw, JWTResponse::class.java
                )
            }.getOrNull()
            parsed?.message?.takeIf { !it.isNullOrBlank() }
                ?: raw.ifBlank { "${response.code()} ${response.message()}" }
        }
    }
}
