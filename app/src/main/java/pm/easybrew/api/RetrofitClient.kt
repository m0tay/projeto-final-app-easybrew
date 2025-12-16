package pm.easybrew.api

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import pm.easybrew.objects.JWTResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private val TAG = RetrofitClient::class.java.simpleName

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(
                "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g24/projeto/api/"
            )
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(ApiService::class.java)
    }

    fun getMessage(response: Response<JWTResponse>): String {
        return if (response.isSuccessful) {
            response.body()?.message?.takeIf { it.isNotBlank() } ?: "OK"
        } else {
            val raw = response.errorBody()?.string().orEmpty()
            val parsed = runCatching {
                Gson().fromJson(
                    raw, JWTResponse::class.java
                )
            }.getOrNull()
            parsed?.message?.takeIf { it.isNotBlank() }
                ?: raw.ifBlank { "${response.code()} ${response.message()}" }
        }
    }

    fun getJWTPayload(token: String): Map<*, *>? {
        return try {
            val parts = token.split(".")
            if (parts.size == 3) {
                val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
                val json = Gson().fromJson(payload, Map::class.java)
                val data = json["data"] as? Map<*, *>
                data
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract user ID from token", e)
            null
        }
    }
}