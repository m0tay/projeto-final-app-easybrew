package pm.easybrew.objects

import pm.easybrew.models.Beverage

data class MenuResponse(
    val records: List<Beverage>
)
