package com.example.network

import com.example.model.AboutSection
import com.example.model.Address
import com.example.model.BlogPost
import com.example.model.BrandItem
import com.example.model.CartItem
import com.example.model.CartSummary
import com.example.model.Category
import com.example.model.CollectionItem
import com.example.model.ColorVariant
import com.example.model.CouponInfo
import com.example.model.DesignBundle
import com.example.model.FaqItem
import com.example.model.FeatureTile
import com.example.model.HeroSlide
import com.example.model.MarqueeItem
import com.example.model.MilestoneItem
import com.example.model.Order
import com.example.model.OrderItem
import com.example.model.OrderTimelineStep
import com.example.model.Product
import com.example.model.ProductReview
import com.example.model.ProductVariation
import com.example.model.PromoBanner
import com.example.model.RazorpayCheckoutPayload
import com.example.model.SiteSettingsConfig
import com.example.model.SiteStat
import com.example.model.SupportMessage
import com.example.model.SupportTicket
import com.example.model.TeamMember
import com.example.model.TestimonialItem
import com.example.model.TrendingTag
import com.example.model.User

object DtoMappers {

    private const val STOREFRONT_BASE = "https://melvore.co.in"

    fun resolveMediaUrl(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("/") -> "$STOREFRONT_BASE$trimmed"
            else -> "$STOREFRONT_BASE/$trimmed"
        }
    }

    fun SiteSettingsDto.toDomain(): SiteSettingsConfig = SiteSettingsConfig(
        brandName = brandName.ifBlank { "Melvore" },
        siteUrl = siteUrl.ifBlank { STOREFRONT_BASE },
        currency = currency.ifBlank { "₹" },
        shippingCharge = shippingCharge,
        freeShippingMinimum = freeShippingMinimum,
        enableCod = enableCod,
        enableOnlinePayment = enableOnlinePayment,
        contactEmail = contactEmail,
        contactPhone = contactPhone,
        siteAddress = siteAddress,
        workingHours = workingHours,
        footerCopyright = footerCopyright,
        showTrackingIdToCustomer = showTrackingIdToCustomer,
        showCourierNameToCustomer = showCourierNameToCustomer,
        trackingPageSupportText = trackingPageSupportText,
        orderTrackingHeroText = orderTrackingHeroText
    )

    fun DesignBundleResponse.toDomain(): DesignBundle = DesignBundle(
        siteName = siteSettings.brandName.ifBlank { "Melvore" },
        currency = siteSettings.currency.ifBlank { "₹" },
        siteSettings = siteSettings.toDomain(),
        heroSlides = heroSlides
            .filter { it.isActiveBool() }
            .sortedWith(compareBy<HeroSlideDto> { it.displayOrder }.thenBy { it.id })
            .map {
                HeroSlide(
                    id = it.id,
                    eyebrow = it.eyebrow.trim(),
                    title = it.title.trim(),
                    subtitle = it.subtitle.trim(),
                    imageUrl = resolveMediaUrl(it.imageUrl),
                    mobileImageUrl = resolveMediaUrl(it.mobileImageUrl),
                    desktopImageUrl = resolveMediaUrl(it.desktopImageUrl),
                    ctaText = it.ctaText.trim(),
                    ctaLink = it.ctaLink.trim(),
                    displayOrder = it.displayOrder,
                    isActive = it.isActiveBool()
                )
            },
        promoBanners = banners.map {
            PromoBanner(
                id = it.bannerId,
                title = it.title.trim(),
                subtitle = it.subtitle.trim(),
                tag = it.tag.trim(),
                imageUrl = resolveMediaUrl(it.imageUrl),
                ctaText = it.ctaText.trim(),
                ctaLink = it.ctaLink.trim(),
                filterTag = it.filterTag.trim(),
                position = it.position.trim()
            )
        },
        marqueeItems = marquee.map { MarqueeItem(it.id, it.text, it.icon) },
        featureTiles = featureTiles.map { FeatureTile(it.featureId, it.icon, it.title, it.description) },
        testimonials = testimonials.map {
            TestimonialItem(
                id = it.testimonialId,
                customerName = it.customerName,
                customerImage = resolveMediaUrl(it.customerImage),
                customerTitle = it.customerTitle,
                rating = it.rating,
                content = it.content
            )
        },
        siteStats = siteStats.map { SiteStat(it.statId, it.label, it.value) },
        teamMembers = teamMembers.map {
            TeamMember(it.id, it.name, it.position, it.bio, resolveMediaUrl(it.imageUrl))
        },
        milestones = milestones.map { MilestoneItem(it.id, it.year, it.title, it.description) },
        featuredProducts = featured.map { it.toDomain() }
    )

    fun CategoryDto.toDomain(): Category = Category(
        id = categoryId,
        name = name,
        slug = slug,
        description = description.orEmpty(),
        imageUrl = resolveMediaUrl(imageUrl),
        itemCount = productCount,
        isFeatured = isFeatured,
        coverImageUrl = resolveMediaUrl(coverImageUrl) ?: resolveMediaUrl(imageUrl),
        motifKey = motifKey.ifBlank { "editorial_frame" },
        editorialBadge = editorialBadge?.takeIf { it.isNotBlank() },
        editorialSubtitle = editorialSubtitle?.takeIf { it.isNotBlank() },
        accentHex = accentHex.ifBlank { "#141414" },
        bgHex = bgHex.ifBlank { "#F8F7F4" },
        animationKey = animationKey.ifBlank { "zoom_reveal" },
        tileSpan = tileSpan.ifBlank { "regular" },
        displayOrder = displayOrder
    )

    fun BrandDto.toDomain(): BrandItem = BrandItem(
        id = resolvedId(),
        name = resolvedName(),
        slug = slug.ifBlank { resolvedName().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-') },
        description = description.orEmpty().trim(),
        logoUrl = resolveMediaUrl(resolvedLogo()),
        brandUrl = brandUrl?.trim().orEmpty(),
        originCountry = originCountry?.trim().orEmpty(),
        isImported = isImportedBool(),
        productCount = productCount,
        displayOrder = displayOrder,
        isActive = isActiveBrand()
    )

    fun CollectionDto.toDomain(): CollectionItem = CollectionItem(
        id = id,
        name = name,
        subtitle = subtitle,
        slug = slug,
        bannerUrl = resolveMediaUrl(bannerUrl)
    )

    fun TrendingTagDto.toDomain(): TrendingTag = TrendingTag(
        id = tagId,
        name = name,
        icon = icon
    )

    fun ProductDto.toDomain(): Product = Product(
        id = productId,
        sku = sku,
        name = name,
        slug = slug,
        category = categoryName ?: "Apparel",
        categorySlug = categorySlug.orEmpty(),
        categoryId = categoryId,
        brandId = brandId,
        brandName = brandName?.trim().orEmpty(),
        brandSlug = brandSlug?.trim().orEmpty(),
        originCountry = originCountry?.trim().orEmpty(),
        isImported = isImported || !originCountry.isNullOrBlank(),
        regularPrice = regularPrice,
        salePrice = salePrice,
        discountPercentOverride = discountPercent,
        imageUrl = resolveMediaUrl(primaryImage) ?: images.firstOrNull()?.let(::resolveMediaUrl),
        galleryUrls = images.mapNotNull(::resolveMediaUrl),
        shortDescription = shortDescription.orEmpty(),
        description = description ?: shortDescription.orEmpty(),
        weight = weight,
        dimensions = dimensions.orEmpty(),
        colors = availableColors.map { ColorVariant(name = it.name, hex = it.value) },
        sizes = availableSizes,
        variations = variations.map {
            ProductVariation(
                id = it.variationId,
                attributeName = it.attributeName,
                attributeValue = it.attributeValue,
                color = it.color.orEmpty(),
                size = it.size.orEmpty(),
                regularPrice = it.regularPrice,
                salePrice = it.salePrice,
                stockQuantity = it.stockQuantity,
                isActive = it.isActive,
                imageUrl = resolveMediaUrl(it.imageUrl)
            )
        },
        reviews = reviews.map {
            ProductReview(
                id = it.reviewId,
                userName = it.userName,
                rating = it.rating,
                title = it.title.orEmpty(),
                comment = it.comment,
                isVerifiedPurchase = it.isVerifiedPurchase,
                helpfulCount = it.helpfulCount,
                hasVotedHelpful = it.hasVotedHelpful,
                mediaUrls = it.media.mapNotNull { m -> resolveMediaUrl(m.url) },
                createdAt = it.createdAt
            )
        },
        relatedProducts = relatedProducts.map { it.toDomain() },
        badge = badge,
        rating = ratingAverage,
        reviewCount = ratingCount,
        stockQuantity = stockQuantity,
        inStock = inStock,
        isFeatured = isFeatured,
        isSponsored = isSponsored,
        expressDelivery = expressDelivery,
        videoUrls = videos.mapNotNull { resolveMediaUrl(it.url) }
    )

    fun UserDto.toDomain(): User = User(
        id = userId,
        name = name,
        email = email,
        phone = mobilePrimary.orEmpty(),
        alternatePhone = mobileAlternate.orEmpty(),
        avatar = resolveMediaUrl(avatar),
        emailVerified = emailVerified,
        mobileVerified = mobileVerified,
        isSubscribed = isSubscribed,
        socialProvider = socialProvider,
        createdAt = createdAt
    )

    fun CartItemDto.toDomain(): CartItem = CartItem(
        cartId = cartId,
        productId = productId,
        variationId = variationId,
        name = name,
        sku = sku,
        price = unitPrice,
        regularPrice = regularPrice,
        lineTotal = lineTotal,
        imageUrl = resolveMediaUrl(imageUrl),
        color = color ?: attributes?.color.orEmpty(),
        colorHex = "#141414",
        size = size ?: attributes?.size.orEmpty(),
        quantity = quantity,
        availableStock = availableStock,
        inStock = inStock
    )

    fun CartSummaryDto.toDomain(): CartSummary = CartSummary(
        subtotal = subtotal,
        shippingCharge = shippingCharge,
        freeShipMin = freeShipMin,
        taxAmount = taxAmount,
        discountAmount = discountAmount,
        codFee = codFee,
        totalAmount = totalAmount,
        couponCode = couponCode,
        couponMessage = couponMessage,
        currency = currency.ifBlank { "₹" }
    )

    fun AddressDto.toDomain(): Address = Address(
        id = addressId,
        label = addressType.ifBlank { "home" },
        fullName = fullName,
        phone = mobile,
        street = addressLine1,
        addressLine2 = addressLine2.orEmpty(),
        city = city,
        state = state,
        pincode = pincode,
        country = country.ifBlank { "India" },
        isDefault = isDefaultBool()
    )

    fun OrderCreatedDto.toDomain(): Order = Order(
        id = orderId,
        orderNumber = orderNumber,
        status = orderStatus,
        paymentMethod = paymentMethod,
        paymentStatus = paymentStatus,
        subtotal = subtotal,
        shippingFee = shippingCharge,
        taxAmount = taxAmount,
        codFee = codFee,
        discount = discountAmount,
        total = totalAmount,
        couponCode = couponCode?.takeIf { it.isNotBlank() },
        estimatedDelivery = estimatedDeliveryDate,
        items = items.map {
            OrderItem(
                productId = it.productId,
                name = it.productName,
                sku = it.sku,
                price = it.unitPrice,
                totalPrice = it.totalPrice,
                quantity = it.quantity,
                color = it.color.orEmpty(),
                size = it.size.orEmpty()
            )
        }
    )

    fun OrderHistoryDto.toDomain(): Order = Order(
        id = orderId,
        orderNumber = orderNumber,
        status = orderStatus,
        paymentMethod = paymentMethod,
        paymentStatus = paymentStatus,
        subtotal = subtotal,
        shippingFee = shippingCharge,
        taxAmount = taxAmount,
        codFee = codFee,
        discount = discountAmount,
        total = totalAmount,
        couponCode = couponCode?.takeIf { it.isNotBlank() },
        trackingCourier = courierName,
        trackingNumber = trackingNumber,
        estimatedDelivery = estimatedDeliveryDate,
        createdAt = createdAt,
        items = items.map {
            OrderItem(
                orderItemId = it.orderItemId,
                productId = it.productId,
                name = it.productName,
                sku = it.sku,
                imageUrl = resolveMediaUrl(it.imageUrl),
                price = it.unitPrice,
                totalPrice = it.totalPrice,
                quantity = it.quantity,
                color = it.attributes?.color.orEmpty(),
                size = it.attributes?.size.orEmpty()
            )
        }
    )

    fun OrderDetailsDto.toDomain(): Order = Order(
        id = orderId,
        orderNumber = orderNumber,
        status = orderStatus,
        paymentMethod = paymentMethod,
        paymentStatus = paymentStatus,
        subtotal = subtotal,
        shippingFee = shippingCharge,
        taxAmount = taxAmount,
        codFee = codFee,
        discount = discountAmount,
        total = totalAmount,
        couponCode = couponCode?.takeIf { it.isNotBlank() },
        shippingAddress = shippingAddress?.toDomain(),
        trackingCourier = courierName,
        trackingNumber = trackingNumber,
        trackingUrl = trackingUrl,
        estimatedDelivery = estimatedDeliveryDate,
        cancellationReason = cancellationReason,
        createdAt = createdAt,
        items = items.map {
            OrderItem(
                orderItemId = it.orderItemId,
                productId = it.productId,
                name = it.productName,
                sku = it.sku,
                imageUrl = resolveMediaUrl(it.imageUrl),
                price = it.unitPrice,
                totalPrice = it.totalPrice,
                quantity = it.quantity,
                color = it.attributes?.color.orEmpty(),
                size = it.attributes?.size.orEmpty()
            )
        }
    )

    fun OrderTrackingDto.toDomain(existingDetails: Order? = null): Order = Order(
        id = orderId,
        orderNumber = orderNumber,
        status = orderStatus,
        paymentMethod = paymentMethod,
        paymentStatus = paymentStatus,
        subtotal = existingDetails?.subtotal ?: totalAmount,
        shippingFee = existingDetails?.shippingFee ?: 0.0,
        taxAmount = existingDetails?.taxAmount ?: 0.0,
        codFee = existingDetails?.codFee ?: 0.0,
        discount = existingDetails?.discount ?: 0.0,
        total = totalAmount,
        couponCode = existingDetails?.couponCode,
        shippingAddress = existingDetails?.shippingAddress,
        items = existingDetails?.items.orEmpty(),
        trackingCourier = courierName,
        trackingNumber = trackingNumber,
        trackingUrl = trackingUrl,
        estimatedDelivery = estimatedDeliveryDate,
        createdAt = createdAt,
        timeline = timeline.map {
            OrderTimelineStep(
                key = it.key,
                title = it.title,
                description = it.description.orEmpty(),
                timestamp = it.timestamp.orEmpty(),
                completed = it.completed,
                current = it.current
            )
        }
    )

    fun PaymentInitiateResponse.toPayload(): RazorpayCheckoutPayload = RazorpayCheckoutPayload(
        orderId = orderId,
        orderNumber = orderNumber,
        keyId = keyId,
        razorpayOrderId = razorpayOrderId,
        amountPaise = amountPaise,
        currency = currency.ifBlank { "INR" },
        environment = environment,
        prefillName = prefill.name,
        prefillEmail = prefill.email,
        prefillContact = prefill.contact
    )

    fun SupportTicketDto.toDomain(): SupportTicket {
        val initialMsg = if (message.isNotBlank()) {
            listOf(SupportMessage(id = 0, sender = "user", text = message, createdAt = createdAt))
        } else emptyList()
        val replyMsgs = replies.map {
            SupportMessage(
                id = it.replyId,
                sender = if (it.senderType == "user" || it.senderType == "customer") "user" else "support",
                text = it.message,
                createdAt = it.createdAt
            )
        }
        return SupportTicket(
            id = ticketId,
            ticketNumber = ticketNumber,
            orderId = orderId,
            subject = subject,
            category = category,
            priority = priority,
            status = status,
            message = message,
            createdAt = createdAt,
            replies = initialMsg + replyMsgs
        )
    }

    fun BlogPostDto.toDomain(): BlogPost = BlogPost(
        id = id,
        slug = slug,
        title = title,
        category = categoryName,
        publishedAt = createdAt,
        excerpt = excerpt,
        contentHtml = content,
        imageUrl = resolveMediaUrl(featuredImage),
        viewCount = viewCount
    )

    fun FaqDto.toDomain(): FaqItem = FaqItem(
        id = faqId,
        category = category,
        question = question,
        answer = answer,
        displayOrder = displayOrder
    )

    fun AboutContentDto.toDomain(): AboutSection = AboutSection(
        id = id,
        sectionKey = sectionKey,
        title = title,
        content = content
    )

    fun CouponInfoDto.toDomain(): CouponInfo = CouponInfo(
        couponId = couponId,
        code = code,
        description = description,
        discountType = discountType,
        discountValue = discountValue,
        minOrderAmount = minOrderAmount,
        maxDiscountAmount = maxDiscountAmount
    )
}
