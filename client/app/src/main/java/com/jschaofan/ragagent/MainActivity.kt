package com.jschaofan.ragagent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.MutableSharedFlow
import androidx.lifecycle.ViewModelProvider
import com.jschaofan.ragagent.core.network.PersistentTokenProvider
import com.jschaofan.ragagent.core.network.NetworkModule
import com.jschaofan.ragagent.data.repository.RemoteCartRepository
import com.jschaofan.ragagent.ui.admin.AdminScreen
import com.jschaofan.ragagent.ui.admin.AdminViewModel
import com.jschaofan.ragagent.ui.auth.LoginScreen
import com.jschaofan.ragagent.ui.auth.LoginViewModel
import com.jschaofan.ragagent.ui.cart.CartScreen
import com.jschaofan.ragagent.ui.cart.CartViewModel
import com.jschaofan.ragagent.ui.cart.CheckoutScreen
import com.jschaofan.ragagent.ui.cart.OrderSuccessScreen
import com.jschaofan.ragagent.ui.cart.toAddCartProduct
import com.jschaofan.ragagent.ui.chat.ChatScreen
import com.jschaofan.ragagent.ui.chat.ChatViewModel
import com.jschaofan.ragagent.ui.chat.media.ImageAttachmentProcessor
import com.jschaofan.ragagent.ui.product.detail.ProductDetailScreen
import com.jschaofan.ragagent.ui.product.detail.ProductDetailViewModel
import com.jschaofan.ragagent.ui.product.list.ProductListScreen
import com.jschaofan.ragagent.ui.product.list.ProductListViewModel
import com.jschaofan.ragagent.ui.profile.ProfileScreen
import com.jschaofan.ragagent.ui.theme.RAGGuideAgentTheme

class MainActivity : ComponentActivity() {
    private val unauthorizedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val tokenProvider by lazy {
        PersistentTokenProvider(
            context = applicationContext,
            fallbackToken = BuildConfig.API_TOKEN.takeIf(String::isNotBlank),
        )
    }

    private val networkModule by lazy {
        NetworkModule(
            tokenProvider = tokenProvider,
            onUnauthorized = { unauthorizedEvents.tryEmit(Unit) },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val chatViewModel = ViewModelProvider(
            this,
            ChatViewModel.Factory(
                repository = networkModule.chatRepository,
                imageAttachmentProcessor = ImageAttachmentProcessor(applicationContext),
            ),
        )[ChatViewModel::class.java]
        val productDetailViewModel = ViewModelProvider(
            this,
            ProductDetailViewModel.Factory(networkModule.productRepository),
        )[ProductDetailViewModel::class.java]
        val cartViewModel = ViewModelProvider(
            this,
            CartViewModel.Factory(
                RemoteCartRepository(
                    api = networkModule.portalApi,
                    productApi = networkModule.productApi,
                ),
            ),
        )[CartViewModel::class.java]
        val productListViewModel = ViewModelProvider(
            this,
            ProductListViewModel.Factory(networkModule.productApi),
        )[ProductListViewModel::class.java]
        val adminViewModel = ViewModelProvider(
            this,
            AdminViewModel.Factory(
                networkModule.portalApi,
                networkModule.productApi,
                applicationContext,
            ),
        )[AdminViewModel::class.java]
        val loginViewModel = ViewModelProvider(
            this,
            LoginViewModel.Factory(networkModule.authRepository),
        )[LoginViewModel::class.java]

        setContent {
            RAGGuideAgentTheme {
                val cartState by cartViewModel.uiState.collectAsState()
                var selectedProductId by remember { mutableStateOf<Long?>(null) }
                var productReturnPage by remember { mutableStateOf(AppPage.PRODUCT_LIST) }
                var cartReturnPage by remember { mutableStateOf(AppPage.PRODUCT_LIST) }
                var page by remember {
                    mutableStateOf(
                        if (tokenProvider.getToken().isNullOrBlank()) AppPage.LOGIN else AppPage.PRODUCT_LIST,
                    )
                }
                val navigate: (AppPage) -> Unit = { target ->
                    page = when {
                        target == AppPage.LOGIN -> AppPage.LOGIN
                        tokenProvider.getToken().isNullOrBlank() -> AppPage.LOGIN
                        target == AppPage.ADMIN && tokenProvider.getUserType() !in setOf(0, 1) -> AppPage.CHAT
                        else -> target
                    }
                }

                LaunchedEffect(Unit) {
                    unauthorizedEvents.collect {
                        tokenProvider.clearSession()
                        navigate(AppPage.LOGIN)
                    }
                }

                when (page) {
                    AppPage.LOGIN -> LoginScreen(
                        viewModel = loginViewModel,
                        onLoginSuccess = { session ->
                            tokenProvider.updateSession(
                                session.token,
                                session.userId,
                                session.userType,
                                session.identifier,
                            )
                            navigate(AppPage.PRODUCT_LIST)
                        },
                    )

                    AppPage.CHAT -> ChatScreen(
                        viewModel = chatViewModel,
                        isAdmin = tokenProvider.getUserType() in setOf(0, 1),
                        cartItemCount = cartState.cart.totalQuantity,
                        onCartClick = {
                            cartReturnPage = AppPage.CHAT
                            navigate(AppPage.CART)
                        },
                        onProductsClick = { navigate(AppPage.PRODUCT_LIST) },
                        onAdminClick = { navigate(AppPage.ADMIN) },
                        onBackClick = { navigate(AppPage.PRODUCT_LIST) },
                        onLogoutClick = {
                            loginViewModel.logout {
                                tokenProvider.clearSession()
                                navigate(AppPage.LOGIN)
                            }
                        },
                        onProductClick = { product ->
                            productDetailViewModel.loadProduct(product.id)
                            selectedProductId = product.id
                            productReturnPage = AppPage.CHAT
                            navigate(AppPage.PRODUCT_DETAIL)
                        },
                        onAddToCart = { product ->
                            val cartProduct = product.toAddCartProduct()
                            if (cartProduct == null) {
                                productDetailViewModel.loadProduct(product.id)
                                selectedProductId = product.id
                                productReturnPage = AppPage.CHAT
                                navigate(AppPage.PRODUCT_DETAIL)
                            } else {
                                cartViewModel.addProduct(cartProduct)
                                cartReturnPage = AppPage.CHAT
                                navigate(AppPage.CART)
                            }
                        },
                    )

                    AppPage.PRODUCT_LIST -> ProductListScreen(
                        viewModel = productListViewModel,
                        onBack = { navigate(AppPage.PRODUCT_LIST) },
                        onChatClick = { navigate(AppPage.CHAT) },
                        onCartClick = {
                            cartReturnPage = AppPage.PRODUCT_LIST
                            navigate(AppPage.CART)
                        },
                        onProfileClick = { navigate(AppPage.PROFILE) },
                        cartItemCount = cartState.cart.totalQuantity,
                        onProductClick = { productId ->
                            productDetailViewModel.loadProduct(productId)
                            selectedProductId = productId
                            productReturnPage = AppPage.PRODUCT_LIST
                            navigate(AppPage.PRODUCT_DETAIL)
                        },
                        onAddToCart = { product ->
                            val cartProduct = product.toAddCartProduct()
                            if (cartProduct == null) {
                                productDetailViewModel.loadProduct(product.id)
                                selectedProductId = product.id
                                productReturnPage = AppPage.PRODUCT_LIST
                                navigate(AppPage.PRODUCT_DETAIL)
                            } else {
                                cartViewModel.addProduct(cartProduct)
                                cartReturnPage = AppPage.PRODUCT_LIST
                                navigate(AppPage.CART)
                            }
                        },
                    )

                    AppPage.ADMIN -> AdminScreen(
                        viewModel = adminViewModel,
                        currentUserId = tokenProvider.getUserId(),
                        currentUserType = tokenProvider.getUserType() ?: 2,
                        onBack = { navigate(AppPage.PROFILE) },
                    )

                    AppPage.PROFILE -> ProfileScreen(
                        identifier = tokenProvider.getIdentifier().orEmpty(),
                        userId = tokenProvider.getUserId(),
                        userType = tokenProvider.getUserType() ?: 2,
                        cartItemCount = cartState.cart.totalQuantity,
                        onHomeClick = { navigate(AppPage.PRODUCT_LIST) },
                        onChatClick = { navigate(AppPage.CHAT) },
                        onCartClick = {
                            cartReturnPage = AppPage.PROFILE
                            navigate(AppPage.CART)
                        },
                        onAdminClick = { navigate(AppPage.ADMIN) },
                        onLogoutClick = {
                            loginViewModel.logout {
                                tokenProvider.clearSession()
                                navigate(AppPage.LOGIN)
                            }
                        },
                    )

                    AppPage.PRODUCT_DETAIL -> ProductDetailScreen(
                        viewModel = productDetailViewModel,
                        cartItemCount = cartState.cart.totalQuantity,
                        onCartClick = {
                            cartReturnPage = AppPage.PRODUCT_DETAIL
                            navigate(AppPage.CART)
                        },
                        onBack = {
                            selectedProductId = null
                            navigate(productReturnPage)
                        },
                        onAddToCart = { product, sku ->
                            product.toAddCartProduct(sku)?.let(cartViewModel::addProduct)
                            cartReturnPage = AppPage.PRODUCT_DETAIL
                            navigate(AppPage.CART)
                        },
                    )

                    AppPage.CART -> CartScreen(
                        viewModel = cartViewModel,
                        onBack = {
                            navigate(cartReturnPage)
                        },
                        onCheckout = { navigate(AppPage.CHECKOUT) },
                    )

                    AppPage.CHECKOUT -> CheckoutScreen(
                        viewModel = cartViewModel,
                        onBack = { navigate(AppPage.CART) },
                        onOrderSubmitted = { navigate(AppPage.ORDER_SUCCESS) },
                    )

                    AppPage.ORDER_SUCCESS -> {
                        cartState.completedOrder?.let { order ->
                            OrderSuccessScreen(
                                order = order,
                                onDone = {
                                    cartViewModel.clearCompletedOrder()
                                    selectedProductId = null
                                    navigate(AppPage.CHAT)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class AppPage {
    LOGIN,
    CHAT,
    PRODUCT_LIST,
    PROFILE,
    ADMIN,
    PRODUCT_DETAIL,
    CART,
    CHECKOUT,
    ORDER_SUCCESS,
}
