package pm.easybrew.objects

data class AddBalanceRequest(
    val jwt: String,
    val type: String,
    val amount: Double,
    val status: String
)
