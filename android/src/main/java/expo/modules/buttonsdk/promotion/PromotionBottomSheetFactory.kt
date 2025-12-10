package expo.modules.buttonsdk.promotion

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import expo.modules.buttonsdk.ui.PromotionBottomSheet

object PromotionBottomSheetFactory {
    
    fun createBottomSheet(
        context: Context,
        promotionData: Map<String, Any>?,
        listTitle: String,
        onPromotionClick: (String) -> Unit,
        onClose: () -> Unit
    ): View {
        val bottomSheet = PromotionBottomSheet(
            context = context,
            promotionData = promotionData,
            listTitle = listTitle,
            onPromotionClick = onPromotionClick,
            onClose = onClose
        )
        
        // Create a dummy container for the factory method - the real container will be provided when adding to view
        val dummyContainer = android.widget.FrameLayout(context)
        return bottomSheet.createBottomSheetContent(
            promotions = extractPromotionItems(promotionData),
            container = dummyContainer
        )
    }
    
    fun animateBottomSheetEntry(bottomSheetView: View, container: ViewGroup) {
        val sheetChild = (bottomSheetView as RelativeLayout).getChildAt(0)
        
        sheetChild?.translationY = container.height.toFloat()
        
        sheetChild?.animate()
            ?.translationY(0f)
            ?.setDuration(300)
            ?.setInterpolator(android.view.animation.DecelerateInterpolator())
            ?.start()
    }
    
    private fun extractPromotionItems(promotionData: Map<String, Any>?): List<Pair<String, String>> {
        promotionData ?: return emptyList()
        
        val promotionItems = mutableListOf<Pair<String, String>>()
        
        val featuredPromotion = promotionData["featuredPromotion"] as? Map<String, Any>
        featuredPromotion?.let { promo ->
            val title = promo["description"] as? String ?: "Promotion"
            val id = promo["id"] as? String ?: ""
            val startsAt = promo["startsAt"] as? String
            
            var actionTitle = title
            
            if (startsAt != null && PromotionUtils.isPromotionNew(startsAt)) {
                actionTitle = "⭐ $actionTitle"
            }
            
            val code = promo["couponCode"] as? String
            if (!code.isNullOrEmpty()) {
                actionTitle = "$actionTitle"
            }
            
            promotionItems.add(Pair(actionTitle, id))
        }
        
        val promotions = promotionData["promotions"] as? List<Map<String, Any>>
        promotions?.forEach { promo ->
            val title = promo["description"] as? String ?: "Promotion"
            val id = promo["id"] as? String ?: ""
            val startsAt = promo["startsAt"] as? String
            
            var actionTitle = title
            
            if (startsAt != null && PromotionUtils.isPromotionNew(startsAt)) {
                actionTitle = "NEW! $actionTitle"
            }
            
            val code = promo["couponCode"] as? String
            if (!code.isNullOrEmpty()) {
                actionTitle = "$actionTitle"
            }
            
            if (id.isNotEmpty()) {
                promotionItems.add(Pair(actionTitle, id))
            }
        }
        
        return promotionItems
    }
}