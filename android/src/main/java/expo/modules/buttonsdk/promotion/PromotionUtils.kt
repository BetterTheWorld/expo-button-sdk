package expo.modules.buttonsdk.promotion

import java.text.SimpleDateFormat
import java.util.*

object PromotionUtils {
    
    fun isPromotionNew(startsAt: String): Boolean {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            val startDate = formatter.parse(startsAt) ?: return false
            
            val twoDaysAgo = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -2)
            }.time
            
            startDate >= twoDaysAgo
        } catch (e: Exception) {
            false
        }
    }
}