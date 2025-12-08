package pm.easybrew.api

import pm.easybrew.objects.JWTResponse
import pm.easybrew.objects.LoginRequest
import pm.easybrew.objects.MenuRequest
import pm.easybrew.objects.MenuResponse
import pm.easybrew.objects.RegisterRequest
import pm.easybrew.objects.ValidateTokenRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("auth/login.php")
    suspend fun login(@Body request: LoginRequest): Response<JWTResponse>

    @POST("auth/register.php")
    suspend fun register(@Body request: RegisterRequest): Response<JWTResponse>

    @POST("auth/validate_token.php")
    suspend fun validateToken(@Body request: ValidateTokenRequest): Response<JWTResponse>

    @POST("machines/menu.php")
    suspend fun menu(@Body request: MenuRequest): Response<MenuResponse>
}