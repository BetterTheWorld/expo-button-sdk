package expo.modules.buttonsdk

import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.graphics.Color
import com.usebutton.sdk.purchasepath.BrowserInterface
import com.usebutton.sdk.purchasepath.BrowserChromeClient
import java.text.SimpleDateFormat
import java.util.*

class PromotionManager(
    private val context: Context,
    private val promotionData: Map<String, Any>?,
    private var onPromotionClickCallback: ((String, BrowserInterface?) -> Unit)?
) {
    
    private var currentBrowser: BrowserInterface? = null
    
    fun setOnPromotionClickCallback(callback: (String, BrowserInterface?) -> Unit) {
        this.onPromotionClickCallback = callback
    }
    
    fun setupPromotionsBadge(browser: BrowserInterface) {
        promotionData ?: return
        
        // Store browser reference for dismiss functionality
        currentBrowser = browser
        
        val promotions = promotionData["promotions"] as? List<*> ?: listOf<Any>()
        val featuredPromotion = promotionData["featuredPromotion"] as? Map<String, Any>
        val totalCount = promotions.size + if (featuredPromotion != null) 1 else 0
        
        if (totalCount > 0) {
            try {
                // Use the official Button SDK method: setCustomActionView
                val promotionButton = createHeaderPromotionButton(totalCount)
                browser.header.setCustomActionView(promotionButton)
                
                // Set the chrome client to handle clicks (as per documentation)
                browser.setBrowserChromeClient(object : com.usebutton.sdk.purchasepath.BrowserChromeClient() {
                    override fun onCustomActionClick(browser: BrowserInterface, view: View) {
                        android.util.Log.d("PromotionManager", "🎯 Custom action clicked!")
                        showPromotionsList()
                    }
                    
                    override fun onSubtitleClick(browser: BrowserInterface) {
                        // Required override but not used
                    }
                })
                
                android.util.Log.d("PromotionManager", "✅ Successfully added custom action view to header")
                
            } catch (e: Exception) {
                android.util.Log.w("PromotionManager", "❌ Could not add custom action view: ${e.message}")
                // Fallback: modify header title
                addPromotionToTitle(totalCount)
            }
        }
    }
    
    private fun hasMethod(obj: Any, methodName: String): Boolean {
        return try {
            obj.javaClass.getMethod(methodName, View::class.java)
            true
        } catch (e: NoSuchMethodException) {
            false
        }
    }
    
    private fun addPromotionToTitle(totalCount: Int) {
        try {
            val originalTitle = currentBrowser?.header?.title?.text
            val newTitle = if (originalTitle.isNullOrEmpty()) {
                "🏷️ Offers ($totalCount) - Tap here"
            } else {
                "$originalTitle | 🏷️ Offers ($totalCount)"
            }
            currentBrowser?.header?.title?.text = newTitle
            
            // Try to make the title clickeable by accessing the underlying view
            try {
                val titleView = currentBrowser?.header?.title
                // Try to get the actual TextView if possible
                val titleTextField = titleView?.javaClass?.getDeclaredField("textView")
                titleTextField?.isAccessible = true
                val actualTextView = titleTextField?.get(titleView) as? TextView
                
                actualTextView?.setOnClickListener {
                    android.util.Log.d("PromotionManager", "Title clicked - showing promotions!")
                    showPromotionsList()
                }
                
                android.util.Log.d("PromotionManager", "Made title clickeable")
            } catch (e: Exception) {
                android.util.Log.w("PromotionManager", "Could not make title clickeable: ${e.message}")
                
                // Alternative: try to find any clickeable parent view
                try {
                    val headerView = currentBrowser?.header
                    val headerClass = headerView?.javaClass
                    val viewField = headerClass?.getDeclaredField("view") 
                        ?: headerClass?.getDeclaredField("headerView")
                        ?: headerClass?.getDeclaredField("container")
                    
                    viewField?.isAccessible = true
                    val actualView = viewField?.get(headerView) as? View
                    
                    actualView?.setOnClickListener {
                        android.util.Log.d("PromotionManager", "Header clicked - showing promotions!")
                        showPromotionsList()
                    }
                    
                    android.util.Log.d("PromotionManager", "Made header clickeable")
                } catch (e2: Exception) {
                    android.util.Log.w("PromotionManager", "Could not make header clickeable: ${e2.message}")
                }
            }
            
            android.util.Log.d("PromotionManager", "Added promotions to header title")
        } catch (e: Exception) {
            android.util.Log.w("PromotionManager", "Could not modify header title: ${e.message}")
        }
    }
    
    private fun createHeaderPromotionButton(count: Int): View {
        val button = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 8, 12, 8)
            // Don't set click listener - Button SDK will handle this via BrowserChromeClient
        }
        
        // Icon
        val iconView = TextView(context).apply {
            text = "🏷️"
            textSize = 16f
            gravity = Gravity.CENTER
        }
        
        // Text
        val textView = TextView(context).apply {
            text = "Offers"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(4, 0, 4, 0)
        }
        
        // Count badge
        val countView = TextView(context).apply {
            text = count.toString()
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(6, 2, 6, 2)
            setBackgroundColor(Color.RED)
            
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(4, 0, 0, 0)
            }
            layoutParams = params
        }
        
        button.addView(iconView)
        button.addView(textView)
        button.addView(countView)
        
        return button
    }
    
    private fun createPromotionOverlay(count: Int): View {
        val overlay = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isClickable = false // Don't block other interactions
        }
        
        val promotionButton = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 12, 16, 12)
            isClickable = true
            isFocusable = true
            setBackgroundColor(Color.parseColor("#E3F2FD")) // Light blue background
            
            // Position in top-right corner
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(0, 80, 16, 0) // Top margin to avoid header overlap
            }
            layoutParams = params
        }
        
        // Add icon and text
        val iconView = TextView(context).apply {
            text = "🏷️"
            textSize = 16f
            gravity = Gravity.CENTER
        }
        
        val textView = TextView(context).apply {
            text = "Offers"
            textSize = 14f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(8, 0, 8, 0)
        }
        
        val countView = TextView(context).apply {
            text = count.toString()
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(8, 4, 8, 4)
            setBackgroundColor(Color.RED)
            
            // Make it circular-ish
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams = params
        }
        
        promotionButton.addView(iconView)
        promotionButton.addView(textView)
        promotionButton.addView(countView)
        
        // Add click listener
        promotionButton.setOnClickListener {
            android.util.Log.d("PromotionManager", "Promotion button clicked!")
            showPromotionsList()
        }
        
        overlay.addView(promotionButton)
        
        return overlay
    }
    
    private fun createPromotionButton(count: Int): View {
        val button = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 8, 12, 8)
            isClickable = true
            isFocusable = true
            
            // Add a subtle background
            setBackgroundResource(android.R.drawable.btn_default)
        }
        
        // Icon
        val iconView = TextView(context).apply {
            text = "🏷️"
            textSize = 14f
            gravity = Gravity.CENTER
        }
        
        // Count badge
        val countView = TextView(context).apply {
            text = count.toString()
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(6, 2, 6, 2)
            setBackgroundColor(Color.RED)
            
            // Make it circular
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(4, 0, 0, 0)
            }
            layoutParams = params
        }
        
        button.addView(iconView)
        button.addView(countView)
        
        // Add click listener
        button.setOnClickListener {
            showPromotionsList()
        }
        
        return button
    }
    
    private fun showPromotionsList() {
        promotionData ?: return
        currentBrowser ?: return
        
        val merchantName = promotionData["merchantName"] as? String ?: "Store"
        val rewardText = promotionData["rewardText"] as? String
        val featuredPromotion = promotionData["featuredPromotion"] as? Map<String, Any>
        
        val promotionItems = mutableListOf<Pair<String, String>>() // title, id
        
        // Add featured promotion if available
        featuredPromotion?.let { promo ->
            val title = promo["title"] as? String ?: "Promotion"
            val id = promo["id"] as? String ?: ""
            val code = promo["code"] as? String
            val createdAt = promo["createdAt"] as? String
            
            var actionTitle = title
            
            // Add NEW badge if promotion is new
            if (createdAt != null && isPromotionNew(createdAt)) {
                actionTitle = "🆕 $actionTitle"
            }
            
            // Add feature indicator
            actionTitle = "⭐ $actionTitle"
            
            // Add reward text if available
            if (!rewardText.isNullOrEmpty()) {
                actionTitle = "$actionTitle\n$rewardText"
            }
            
            // Add coupon code if available
            if (!code.isNullOrEmpty()) {
                actionTitle = "$actionTitle\nCode: $code"
            }
            
            promotionItems.add(Pair(actionTitle, id))
        }
        
        // Add regular promotions
        val promotions = promotionData["promotions"] as? List<*> ?: listOf<Any>()
        for (promotion in promotions) {
            if (promotion is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val promotionMap = promotion as Map<String, Any>
                val title = promotionMap["title"] as? String ?: "Promotion"
                val id = promotionMap["id"] as? String ?: ""
                val code = promotionMap["code"] as? String
                val createdAt = promotionMap["createdAt"] as? String
                
                var actionTitle = title
                
                // Add NEW badge if promotion is new
                if (createdAt != null && isPromotionNew(createdAt)) {
                    actionTitle = "🆕 $actionTitle"
                }
                
                // Add reward text if available
                if (!rewardText.isNullOrEmpty()) {
                    actionTitle = "$actionTitle\n$rewardText"
                }
                
                // Add coupon code if available
                if (!code.isNullOrEmpty()) {
                    actionTitle = "$actionTitle\nCode: $code"
                }
                
                promotionItems.add(Pair(actionTitle, id))
            }
        }
        
        // Use the browser's view container to show promotions overlay (like ConfirmationDialog)
        showPromotionsOverlay(promotionItems)
    }
    
    private fun showPromotionsOverlay(promotions: List<Pair<String, String>>) {
        val browser = currentBrowser ?: return
        val viewContainer = browser.viewContainer ?: return
        
        try {
            val dialogView = createPromotionsDialogView(promotions, viewContainer)
            viewContainer.addView(dialogView)
            android.util.Log.d("PromotionManager", "✅ Promotions overlay added to browser container")
        } catch (e: Exception) {
            android.util.Log.e("PromotionManager", "❌ Error creating promotions overlay", e)
        }
    }
    
    private fun createPromotionsDialogView(promotions: List<Pair<String, String>>, container: ViewGroup): View {
        // Create semi-transparent background overlay
        val dialogContainer = RelativeLayout(context).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#80000000")) // Semi-transparent black
        }
        
        // Create dialog content
        val dialogContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(40, 30, 40, 30)
            layoutParams = RelativeLayout.LayoutParams(
                (context.resources.displayMetrics.widthPixels * 0.85).toInt(), // 85% of screen width
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }
        }
        
        // Title
        val titleView = TextView(context).apply {
            text = "Available Promotions"
            textSize = 18f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        dialogContent.addView(titleView)
        
        // Scrollable promotion list
        val scrollView = android.widget.ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
            }
        }
        
        val promotionListContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        // Add promotion items
        promotions.forEach { (title, id) ->
            val promotionButton = TextView(context).apply {
                text = title
                textSize = 14f
                setTextColor(Color.BLACK)
                setPadding(20, 15, 20, 15)
                gravity = Gravity.START
                setBackgroundResource(android.R.drawable.list_selector_background)
                isClickable = true
                isFocusable = true
                
                setOnClickListener {
                    android.util.Log.d("PromotionManager", "🎯 Promotion selected: $id")
                    
                    // Remove the overlay
                    container.removeView(dialogContainer)
                    
                    // Call the promotion click callback
                    android.util.Log.d("PromotionManager", "📤 Invoking promotion callback for: $id")
                    onPromotionClickCallback?.invoke(id, currentBrowser)
                    android.util.Log.d("PromotionManager", "✅ Promotion callback completed")
                }
                
                // Add divider
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 2
                }
                layoutParams = params
            }
            promotionListContainer.addView(promotionButton)
        }
        
        scrollView.addView(promotionListContainer)
        dialogContent.addView(scrollView)
        
        // Cancel button
        val cancelButton = TextView(context).apply {
            text = "Cancel"
            textSize = 16f
            setTextColor(Color.parseColor("#666666"))
            gravity = Gravity.CENTER
            setPadding(20, 15, 20, 15)
            setBackgroundResource(android.R.drawable.btn_default)
            isClickable = true
            isFocusable = true
            
            setOnClickListener {
                android.util.Log.d("PromotionManager", "❌ Promotions dialog cancelled")
                container.removeView(dialogContainer)
            }
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
            }
            layoutParams = params
        }
        dialogContent.addView(cancelButton)
        
        dialogContainer.addView(dialogContent)
        
        return dialogContainer
    }
    
    private fun isPromotionNew(createdAt: String): Boolean {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            val createdDate = formatter.parse(createdAt) ?: return false
            
            val twoDaysAgo = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -2)
            }.time
            
            createdDate >= twoDaysAgo
        } catch (e: Exception) {
            false
        }
    }
    
}