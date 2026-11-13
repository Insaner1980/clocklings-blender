package fi.clocklings.billing

import android.content.Context

/** Release builds grant paid features only through a verified Google Play purchase. */
fun createAppPurchases(context: Context): Purchases = createPlayPurchases(context)
