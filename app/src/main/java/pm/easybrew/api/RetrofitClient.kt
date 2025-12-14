package pm.easybrew.api

import com.google.gson.Gson
import pm.easybrew.objects.JWTResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
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
}