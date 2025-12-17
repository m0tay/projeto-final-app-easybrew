package pm.easybrew.objects

data class MakeResponse(
    val message: String,
    val beverage: String? = null,
    val preparation: String? = null,
    val price: Double? = null,
    val new_balance: Double? = null,
    val response_time_ms: Double? = null
)

