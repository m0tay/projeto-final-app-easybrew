package pm.easybrew.objects

data class MakeRequest(
    val jwt: String,
    val machine_id: String,
    val user_id: String,
    val beverage_id: String,
    val preparation: String
)
