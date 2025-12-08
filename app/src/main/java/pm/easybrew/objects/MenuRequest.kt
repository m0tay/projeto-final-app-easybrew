package pm.easybrew.objects

data class MenuRequest(
    val jwt: String,
    val machine_id: String,
)