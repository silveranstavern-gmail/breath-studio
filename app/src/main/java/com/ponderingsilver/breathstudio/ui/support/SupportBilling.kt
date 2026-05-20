package com.ponderingsilver.breathstudio.ui.support

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SupportProduct(
    val id: String,
    val title: String,
    val fallbackPrice: String,
    val productType: String,
    val productDetails: ProductDetails? = null,
) {
    val displayPrice: String
        get() = productDetails?.displayPrice() ?: fallbackPrice
}

data class SupportBillingState(
    val connected: Boolean = false,
    val products: List<SupportProduct> = SupportProducts,
    val message: String? = null,
)

class SupportBilling(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val billingClient = runCatching {
        BillingClient.newBuilder(appContext)
            .setListener { billingResult, purchases ->
                SupportDiagnostics.log(
                    title = "Billing callback",
                    detail = "response=${billingResult.responseCode} purchases=${purchases.orEmpty().size}",
                )
                handlePurchases(billingResult, purchases.orEmpty())
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            )
            .build()
    }.onSuccess {
        SupportDiagnostics.log(
            title = "Billing client",
            detail = "BillingClient created successfully.",
        )
    }.onFailure { error ->
        SupportDiagnostics.log(
            title = "Billing client failure",
            detail = "${error::class.java.simpleName}: ${error.message.orEmpty()}",
        )
    }.getOrNull()
    private val _state = MutableStateFlow(
        SupportBillingState(
            message = if (billingClient == null) {
                "Google Play support options are unavailable in this build."
            } else {
                null
            },
        ),
    )
    val state: StateFlow<SupportBillingState> = _state.asStateFlow()

    fun connect() {
        val client = billingClient
        if (client == null) {
            SupportDiagnostics.log(
                title = "Connect skipped",
                detail = "BillingClient is null.",
            )
            _state.value = _state.value.copy(
                connected = false,
                message = "Google Play support options are unavailable in this build.",
            )
            return
        }
        if (client.isReady) {
            SupportDiagnostics.log(
                title = "Connect reused",
                detail = "BillingClient already ready.",
            )
            queryProducts()
            return
        }

        SupportDiagnostics.log(
            title = "Connect start",
            detail = "Starting Play Billing connection.",
        )
        runCatching {
            client.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        SupportDiagnostics.log(
                            title = "Connect finished",
                            detail = "response=${billingResult.responseCode} message=${billingResult.debugMessage}",
                        )
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            _state.value = _state.value.copy(connected = true, message = null)
                            queryProducts()
                        } else {
                            _state.value = _state.value.copy(
                                connected = false,
                                message = "Google Play support options are unavailable right now.",
                            )
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        SupportDiagnostics.log(
                            title = "Billing disconnected",
                            detail = "Billing service disconnected.",
                        )
                        _state.value = _state.value.copy(connected = false)
                    }
                },
            )
        }.onFailure {
            SupportDiagnostics.log(
                title = "Connect failure",
                detail = "${it::class.java.simpleName}: ${it.message.orEmpty()}",
            )
            _state.value = _state.value.copy(
                connected = false,
                message = "Google Play support options are unavailable right now.",
            )
        }
    }

    fun launchPurchase(context: Context, product: SupportProduct) {
        val client = billingClient
        val activity = context.findActivity()
        val details = product.productDetails
        if (client == null || activity == null || details == null || !client.isReady) {
            SupportDiagnostics.log(
                title = "Launch blocked",
                detail = buildString {
                    append("product=${product.id}")
                    append(" clientReady=${client?.isReady == true}")
                    append(" activity=${activity != null}")
                    append(" details=${details != null}")
                },
            )
            _state.value = _state.value.copy(
                message = "Google Play support options are still loading.",
            )
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply {
                details.offerToken()?.let(::setOfferToken)
            }
            .build()

        SupportDiagnostics.log(
            title = "Launch purchase",
            detail = "product=${product.id} price=${product.displayPrice}",
        )
        val result = client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build(),
        )
        SupportDiagnostics.log(
            title = "Launch result",
            detail = "response=${result.responseCode} message=${result.debugMessage}",
        )
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.value = _state.value.copy(message = "Google Play could not start checkout.")
        }
    }

    fun release() {
        val client = billingClient ?: return
        if (client.isReady) {
            SupportDiagnostics.log(
                title = "Billing release",
                detail = "Ending BillingClient connection.",
            )
            client.endConnection()
        }
    }

    private fun queryProducts() {
        queryProductsByType(BillingClient.ProductType.INAPP)
        queryProductsByType(BillingClient.ProductType.SUBS)
    }

    private fun queryProductsByType(productType: String) {
        val client = billingClient ?: return
        val sourceProducts = SupportProducts
            .filter { product -> product.productType == productType }
        val products = sourceProducts
            .map { product ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(product.id)
                    .setProductType(product.productType)
                    .build()
            }

        if (products.isEmpty()) return

        SupportDiagnostics.log(
            title = "Query start",
            detail = "type=$productType ids=${sourceProducts.joinToString { it.id }}",
        )
        runCatching {
            client.queryProductDetailsAsync(
                QueryProductDetailsParams.newBuilder()
                    .setProductList(products)
                    .build(),
            ) { billingResult, productDetailsResult ->
                SupportDiagnostics.log(
                    title = "Query result",
                    detail = "type=$productType response=${billingResult.responseCode} returned=${productDetailsResult.productDetailsList.size}",
                )
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    return@queryProductDetailsAsync
                }
                val detailsById = productDetailsResult.productDetailsList.associateBy { details ->
                    details.productId
                }
                detailsById.values.forEach { details ->
                    SupportDiagnostics.log(
                        title = "Product detail",
                        detail = "id=${details.productId} price=${details.displayPrice().orEmpty()}",
                    )
                }
                _state.value = _state.value.copy(
                    products = _state.value.products.map { product ->
                        detailsById[product.id]?.let { details ->
                            product.copy(productDetails = details)
                        } ?: product
                    },
                    connected = true,
                    message = null,
                )
            }
        }.onFailure {
            SupportDiagnostics.log(
                title = "Query failure",
                detail = "type=$productType ${it::class.java.simpleName}: ${it.message.orEmpty()}",
            )
            _state.value = _state.value.copy(
                connected = false,
                message = "Google Play support options are unavailable right now.",
            )
        }
    }

    private fun handlePurchases(
        billingResult: BillingResult,
        purchases: List<Purchase>,
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.forEach(::finalizePurchase)
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> {
                SupportDiagnostics.log(
                    title = "Purchase callback failure",
                    detail = "response=${billingResult.responseCode} message=${billingResult.debugMessage}",
                )
                _state.value = _state.value.copy(message = "Google Play purchase was not completed.")
            }
        }
    }

    private fun finalizePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        SupportDiagnostics.log(
            title = "Purchase received",
            detail = "products=${purchase.products.joinToString()} acknowledged=${purchase.isAcknowledged}",
        )

        if (MonthlySupportProductId in purchase.products) {
            if (!purchase.isAcknowledged) {
                billingClient?.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build(),
                ) { billingResult ->
                    SupportDiagnostics.log(
                        title = "Subscription acknowledge",
                        detail = "response=${billingResult.responseCode} message=${billingResult.debugMessage}",
                    )
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _state.value = _state.value.copy(message = "Thank you for supporting Breath Studio.")
                    }
                }
            }
            return
        }

        billingClient?.consumeAsync(
            ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build(),
        ) { billingResult, _ ->
            SupportDiagnostics.log(
                title = "Consume result",
                detail = "response=${billingResult.responseCode} message=${billingResult.debugMessage}",
            )
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(message = "Thank you for supporting Breath Studio.")
            }
        }
    }
}

private fun ProductDetails.displayPrice(): String? {
    oneTimePurchaseOfferDetailsList?.firstOrNull()?.let { offer ->
        return offer.formattedPrice
    }
    subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.let { phase ->
        return phase.formattedPrice
    }
    return null
}

private fun ProductDetails.offerToken(): String? {
    oneTimePurchaseOfferDetailsList?.firstOrNull()?.let { offer ->
        return offer.offerToken
    }
    subscriptionOfferDetails?.firstOrNull()?.let { offer ->
        return offer.offerToken
    }
    return null
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val SupportSmallProductId = "support_small"
private const val SupportMediumProductId = "support_medium"
private const val SupportLargeProductId = "support_large"
private const val MonthlySupportProductId = "monthly_supporter"

val SupportProducts: List<SupportProduct> = listOf(
    SupportProduct(
        id = SupportSmallProductId,
        title = "Support - Small",
        fallbackPrice = "$2.99",
        productType = BillingClient.ProductType.INAPP,
    ),
    SupportProduct(
        id = SupportMediumProductId,
        title = "Support - Medium",
        fallbackPrice = "$6.99",
        productType = BillingClient.ProductType.INAPP,
    ),
    SupportProduct(
        id = SupportLargeProductId,
        title = "Support - Large",
        fallbackPrice = "$14.99",
        productType = BillingClient.ProductType.INAPP,
    ),
    SupportProduct(
        id = MonthlySupportProductId,
        title = "Monthly Supporter",
        fallbackPrice = "$4.99 / month",
        productType = BillingClient.ProductType.SUBS,
    ),
)
