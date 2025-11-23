package pm.easybrew.api

import pm.easybrew.objects.JWTResponse
import pm.easybrew.objects.LoginRequest
import pm.easybrew.objects.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("auth/login.php")
    suspend fun login(@Body request: LoginRequest): Response<JWTResponse>

    @POST("auth/register.php")
    suspend fun register(@Body request: RegisterRequest): Response<JWTResponse>
}