// kotlin
package pm.easybrew

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pm.easybrew.api.RetrofitClient
import pm.easybrew.api.RetrofitClient.getMessage
import pm.easybrew.databinding.ActivityRegisterLoginBinding
import pm.easybrew.objects.LoginRequest
import pm.easybrew.objects.RegisterRequest
import java.io.IOException

class RegisterLoginActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityRegisterLoginBinding.inflate(layoutInflater)
    }

    companion object {
        private val TAG = RegisterLoginActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val email = binding.editEmail.text.toString()
            val password = binding.editPassword.text.toString()
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.register(RegisterRequest(email, password))
                    Log.i(TAG, getMessage(response))
                } catch (e: IOException) {
                    Log.e(TAG, "Network error", e)
                } catch (e: Exception) {
                    Log.e(TAG, e.message ?: "Unknown error", e)
                }
            }
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.editEmail.text.toString()
            val password = binding.editPassword.text.toString()
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.login(LoginRequest(email, password))
                    if (response.isSuccessful) {
                        Log.i(TAG, getMessage(response))

                        val intent = Intent(applicationContext, MainActivity::class.java)
                        val sharedPref = getSharedPreferences("easybrew_session", MODE_PRIVATE)
                        sharedPref.edit {
                            putString("token", response.body()?.jwt.toString())
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Log.w(TAG, getMessage(response))
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Network error", e)
                } catch (e: Exception) {
                    Log.e(TAG, e.message ?: "Unknown error", e)
                }
            }
        }
    }
}
