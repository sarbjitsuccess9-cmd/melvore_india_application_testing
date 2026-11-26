package com.example.network

import com.example.model.BlogPost
import com.example.model.BootstrapConfigBundle
import com.example.model.BrandDetailBundle
import com.example.model.BrandProfileItem
import com.example.model.CampaignAsset
import com.example.model.CampaignItem
import com.example.model.Category
import com.example.model.DeliveryServiceability
import com.example.model.EliteHomeBundle
import com.example.model.ElitePack
import com.example.model.ElitePackDetailBundle
import com.example.model.ElitePackItem
import com.example.model.FaqItem
import com.example.model.FashionWorldTheme
import com.example.model.FeedDiagnostics
import com.example.model.ForYouBundle
import com.example.model.HelpFlow
import com.example.model.HelpFlowDetail
import com.example.model.HelpFlowNode
import com.example.model.HelpFlowOption
import com.example.model.HelpOrderItemSnapshot
import com.example.model.HelpOrderReport
import com.example.model.HelpSessionResult
import com.example.model.HelpTicketCreated
import com.example.model.HelpTopic
import com.example.model.HeroSlide
import com.example.model.HomeFeedBundle
import com.example.model.HomeSection
import com.example.model.HomeSectionItem
import com.example.model.OccasionTheme
import com.example.model.PreferredCategoryScore
import com.example.model.ProductReview
import com.example.model.PromoBanner
import com.example.model.ReviewEligibilityItem
import com.example.model.ReviewListBundle
import com.example.model.RunwayConfig
import com.example.model.RunwayModelSpec
import com.example.model.SearchResultBundle
import com.example.model.SearchSuggestionsBundle
import com.example.model.SmartOffer
import com.example.model.StyleWorld
import com.example.model.StyleWorldDetail
import com.example.model.StyleWorldFeedback
import com.example.model.StyleWorldFeedbackResult
import com.example.model.StyleWorldSatisfactionStatus
import com.example.model.ThemeTokenSpec
import com.example.model.UnlockedCoupon
import com.example.model.UserDynamicPreferences
import com.example.model.VideoReel
import com.example.model.VideosFeedPage
import com.example.model.WorldMapOriginPin
import com.example.model.WorldVectorAsset
import com.example.network.DtoMappers.resolveMediaUrl
import com.example.network.DtoMappers.toDomain

object DiscoveryMappers {

    fun CampaignAssetDto.toDomain(): CampaignAsset = CampaignAsset(
        assetId = assetId,
        assetType = assetType,
        mediaUrl = resolveMediaUrl(mediaUrl),
        mobileMediaUrl = resolveMediaUrl(mobileMediaUrl),
        animationKey = animationKey,
        title = title,
        subtitle = subtitle
    )

    fun CampaignDto.toDomain(): CampaignItem = CampaignItem(
        campaignId = campaignId,
        slug = slug,
        title = title,
        subtitle = subtitle,
        festivalKey = festivalKey,
        animationKey = animationKey,
        animationDurationMs = animationDurationMs,
        primaryColor = primaryColor,
        accentColor = accentColor,
        backgroundColor = backgroundColor,
        bannerImageUrl = resolveMediaUrl(bannerImageUrl),
        mobileBannerUrl = resolveMediaUrl(mobileBannerUrl),
        videoUrl = resolveMediaUrl(videoUrl),
        ctaText = ctaText,
        ctaLink = ctaLink,
        assets = assets.map { it.toDomain() }
    )

    fun HomeSectionItemDto.toDomain(): HomeSectionItem = HomeSectionItem(
        itemId = itemId,
        title = title,
        subtitle = subtitle,
        badgeText = badgeText,
        imageUrl = resolveMediaUrl(imageUrl),
        mobileImageUrl = resolveMediaUrl(mobileImageUrl),
        themeKey = themeKey,
        animationKey = animationKey,
        targetType = targetType,
        targetValue = targetValue,
        targetId = targetId
    )

    fun HomeSectionDto.toDomain(): HomeSection = HomeSection(
        sectionId = sectionId,
        sectionKey = sectionKey,
        sectionType = sectionType,
        eyebrow = eyebrow,
        title = title,
        subtitle = subtitle,
        animationKey = animationKey,
        themeKey = themeKey,
        ctaText = ctaText,
        ctaLink = ctaLink,
        displayOrder = displayOrder,
        items = items.map { it.toDomain() },
        products = products.map { it.toDomain() }
    )

    fun HomeFeedDataDto.toDomain(): HomeFeedBundle = HomeFeedBundle(
        heroSlides = heroSlides.filter { it.isActiveBool() }.map {
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
        activeCampaign = activeCampaign?.toDomain(),
        campaigns = campaigns.map { it.toDomain() },
        promoBanners = promoBanners.map {
            PromoBanner(
                id = it.id,
                title = it.title,
                subtitle = it.subtitle,
                tag = it.badgeText,
                imageUrl = resolveMediaUrl(it.imageUrl),
                ctaText = it.ctaText.ifBlank { "Shop Edit" },
                ctaLink = it.ctaLink,
                filterTag = ""
            )
        },
        freeShippingMinimum = siteSettings?.freeShippingMinimum ?: 0.0,
        categories = categories.map { it.toDomain() },
        brands = brands.map { it.toDomain() },
        styleWorlds = styleWorlds.map { it.toDomain() },
        sections = sections.map { it.toDomain() },
        newArrivals = newArrivals.map { it.toDomain() },
        bestsellers = bestsellers.map { it.toDomain() },
        trendingProducts = trendingProducts.map { it.toDomain() },
        mostWishlisted = mostWishlisted.map { it.toDomain() },
        budget999 = budget999.map { it.toDomain() },
        budget1999 = budget1999.map { it.toDomain() },
        sponsoredProducts = sponsoredProducts.map { it.toDomain() },
        recentlyViewed = recentlyViewed.map { it.toDomain() },
        editorialStories = editorialStories.map {
            BlogPost(
                id = it.id,
                slug = it.slug,
                title = it.title,
                category = it.readTime.ifBlank { "Journal" },
                publishedAt = it.publishedAt,
                excerpt = it.excerpt,
                imageUrl = resolveMediaUrl(it.featuredImage)
            )
        },
        previewVideos = previewVideos.map { it.toDomain() },
        discoveryProducts = discoveryProducts.map { it.toDomain() },
        diagnostics = FeedDiagnostics(
            missingTables = diagnostics?.missingTables.orEmpty(),
            emptyModules = diagnostics?.emptyModules.orEmpty()
        )
    )

    fun BrandProfileDto.toDomain(): BrandProfileItem = BrandProfileItem(
        brandId = brandId,
        name = name,
        slug = slug,
        description = description,
        tagline = tagline,
        story = story,
        logoUrl = resolveMediaUrl(logoUrl),
        coverImageUrl = resolveMediaUrl(coverImageUrl),
        mobileCoverUrl = resolveMediaUrl(mobileCoverUrl),
        brandVideoUrl = resolveMediaUrl(brandVideoUrl),
        originCountry = originCountry,
        isFeatured = isFeatured,
        isSponsored = isSponsored,
        isNew = isNew,
        productCount = productCount
    )

    fun BrandDetailsDataDto.toDomain(): BrandDetailBundle? {
        val b = brand?.toDomain() ?: return null
        return BrandDetailBundle(
            brand = b,
            featuredProducts = featuredProducts.map { it.toDomain() },
            newArrivals = newArrivals.map { it.toDomain() },
            bestsellers = bestsellers.map { it.toDomain() },
            sponsoredProducts = sponsoredProducts.map { it.toDomain() }
        )
    }

    fun RunwayModelDto.toDomain(): RunwayModelSpec = RunwayModelSpec(
        name = name,
        role = role,
        palette = palette,
        styleNote = styleNote,
        silhouette = silhouette.ifBlank { "cargo_baby_tee" },
        spriteUrl = resolveMediaUrl(spriteUrl)
    )

    fun RunwayConfigDto.toDomain(): RunwayConfig = RunwayConfig(
        enabled = enabled,
        title = title.ifBlank { "PIXEL RUNWAY SHOWCASE" },
        subtitle = subtitle,
        animationPreset = animationPreset.ifBlank { "pixel_walk_loop" },
        speed = speed.coerceIn(0.4f, 2.5f),
        backgroundHex = backgroundHex.ifBlank { "#160D2D" },
        displayPosition = displayPosition.ifBlank { "after_promo" },
        collectionSlug = collectionSlug,
        artworkUrl = resolveMediaUrl(artworkUrl),
        models = models.map { it.toDomain() }
    )

    fun StyleWorldFeedbackDto.toDomain(): StyleWorldFeedback = StyleWorldFeedback(
        feedbackId = feedbackId,
        styleWorldId = styleWorldId,
        worldSlug = worldSlug,
        satisfactionScore = satisfactionScore,
        feedbackLabel = feedbackLabel,
        couponCode = couponCode,
        submittedAt = submittedAt
    )

    fun UnlockedCouponDto.toDomain(): UnlockedCoupon = UnlockedCoupon(
        couponId = couponId,
        code = code,
        discountType = discountType,
        discountValue = discountValue,
        discountSummary = discountSummary,
        minOrderAmount = minOrderAmount,
        maxDiscountAmount = maxDiscountAmount,
        validTo = validTo,
        conditionsText = conditionsText
    )

    fun StyleWorldSatisfactionStatusDto.toDomain(): StyleWorldSatisfactionStatus = StyleWorldSatisfactionStatus(
        hasSubmitted = hasSubmitted,
        feedback = feedback?.toDomain()
    )

    fun StyleWorldFeedbackResponseDataDto.toDomain(): StyleWorldFeedbackResult = StyleWorldFeedbackResult(
        hasSubmitted = hasSubmitted,
        alreadySubmitted = alreadySubmitted,
        submittedWorldIds = submittedWorldIds,
        submittedWorldSlugs = submittedWorldSlugs,
        feedback = feedback?.toDomain(),
        coupon = coupon?.toDomain()
    )

    fun StyleWorldDto.toDomain(): StyleWorld = StyleWorld(
        styleWorldId = styleWorldId,
        slug = slug,
        name = name,
        tagline = tagline,
        description = description,
        themeKey = themeKey,
        animationKey = animationKey,
        fontStyle = fontStyle,
        buttonStyle = buttonStyle,
        primaryColor = primaryColor,
        accentColor = accentColor,
        backgroundColor = backgroundColor,
        coverImageUrl = resolveMediaUrl(coverImageUrl),
        mobileImageUrl = resolveMediaUrl(mobileImageUrl),
        videoUrl = resolveMediaUrl(videoUrl),
        audioTrackKey = audioTrackKey,
        audioUrl = resolveMediaUrl(audioUrl),
        audioLoop = audioLoop,
        audioVolume = audioVolume.coerceIn(0.05f, 1.0f),
        promoVideoUrl = resolveMediaUrl(promoVideoUrl) ?: resolveMediaUrl(videoUrl),
        promoVideoPosterUrl = resolveMediaUrl(promoVideoPosterUrl)
            ?: resolveMediaUrl(mobileImageUrl)
            ?: resolveMediaUrl(coverImageUrl),
        promoVideoIsSponsored = promoVideoIsSponsored,
        runwayConfig = runwayConfig?.toDomain(),
        hasSubmittedFeedback = hasSubmittedFeedback,
        productCount = productCount
    )

    fun StyleWorldDetailsDataDto.toDomain(): StyleWorldDetail? {
        val sw = styleWorld?.toDomain() ?: return null
        return StyleWorldDetail(
            styleWorld = sw,
            categories = categories.map { it.toDomain() },
            products = products.map { it.toDomain() },
            satisfactionStatus = satisfactionStatus?.toDomain()
        )
    }

    fun VideoReelDto.toDomain(): VideoReel = VideoReel(
        videoId = videoId,
        title = title,
        caption = caption,
        creatorName = creatorName,
        creatorHandle = creatorHandle,
        videoUrl = resolveMediaUrl(videoUrl),
        posterUrl = resolveMediaUrl(posterUrl),
        durationSeconds = durationSeconds,
        aspectRatio = aspectRatio,
        isSponsored = isSponsored,
        isFeatured = isFeatured,
        brandId = brandId,
        brandName = brandName,
        brandSlug = brandSlug,
        brandLogo = resolveMediaUrl(brandLogo),
        productId = productId,
        product = product?.toDomain(),
        viewsCount = viewsCount,
        likesCount = likesCount,
        savesCount = savesCount,
        sharesCount = sharesCount,
        isLiked = isLiked,
        isSaved = isSaved
    )

    fun VideosFeedDataDto.toDomain(): VideosFeedPage = VideosFeedPage(
        videos = videos.map { it.toDomain() },
        page = pagination.page.coerceAtLeast(1),
        totalPages = pagination.totalPages.coerceAtLeast(1),
        total = pagination.total.coerceAtLeast(0),
        tableExists = diagnostics?.tableExists ?: true,
        invalidUrlCount = diagnostics?.invalidUrlCount ?: 0,
        diagnosticsReason = diagnostics?.reason.orEmpty()
    )

    fun ElitePackItemDto.toDomain(): ElitePackItem {
        val mappedProduct = product?.toDomain()
        val isProductItem = itemType.equals("product", ignoreCase = true) || mappedProduct != null
        val safeImageUrl = if (isProductItem && mappedProduct != null) {
            mappedProduct.imageUrl
        } else {
            resolveMediaUrl(imageUrl) ?: mappedProduct?.imageUrl
        }
        return ElitePackItem(
            itemId = itemId,
            itemType = itemType,
            title = title,
            subtitle = subtitle,
            badgeText = badgeText,
            imageUrl = safeImageUrl,
            videoUrl = resolveMediaUrl(videoUrl),
            isSponsored = isSponsored,
            product = mappedProduct,
            brand = brand?.toDomain()
        )
    }

    fun ElitePackDto.toDomain(): ElitePack = ElitePack(
        packId = packId,
        slug = slug,
        sectionKey = sectionKey,
        brandName = brandName,
        eyebrow = eyebrow,
        title = title,
        subtitle = subtitle,
        story = story,
        ctaText = ctaText,
        ctaLink = ctaLink,
        heroImageUrl = resolveMediaUrl(heroImageUrl),
        mobileImageUrl = resolveMediaUrl(mobileImageUrl),
        videoUrl = resolveMediaUrl(videoUrl),
        animationKey = animationKey,
        accentColor = accentColor,
        backgroundColor = backgroundColor,
        layoutType = layoutType.ifBlank { "editorial_grid" },
        featuredProductId = featuredProductId,
        isHero = isHero,
        isSponsored = isSponsored,
        items = items.map { it.toDomain() }
    )

    fun EliteHomeDataDto.toDomain(): EliteHomeBundle = EliteHomeBundle(
        heroPacks = heroPacks.map { it.toDomain() },
        packs = packs.map { it.toDomain() },
        eliteBrands = eliteBrands.map { it.toDomain() },
        sponsoredProducts = sponsoredProducts.map { it.toDomain() },
        eliteVideos = eliteVideos.map { it.toDomain() }
    )

    fun ElitePackDetailDataDto.toDomain(): ElitePackDetailBundle? {
        val p = pack?.toDomain() ?: return null
        return ElitePackDetailBundle(
            pack = p,
            products = products.map { it.toDomain() },
            brands = brands.map { it.toDomain() },
            relatedPacks = relatedPacks.map { it.toDomain() }
        )
    }

    fun PreferredCategoryScoreDto.toDomain(): PreferredCategoryScore = PreferredCategoryScore(
        categoryId = categoryId,
        name = name,
        slug = slug,
        imageUrl = resolveMediaUrl(imageUrl),
        affinityScore = affinityScore
    )

    fun ForYouDataDto.toDomain(): ForYouBundle = ForYouBundle(
        isPersonalized = isPersonalized,
        title = title,
        subtitle = subtitle,
        products = products.map { it.toDomain() },
        recentBrands = recentBrands.map { it.toDomain() },
        favouriteStyles = favouriteStyles.map { it.toDomain() },
        preferredWorlds = preferredWorlds.map { it.toDomain() },
        avoidedWorlds = avoidedWorlds.map { it.toDomain() },
        preferredCategories = preferredCategories.map { it.toDomain() },
        preferredStyles = preferredStyles,
        recentlyExploredWorlds = recentlyExploredWorlds.map { it.toDomain() }
    )

    fun SearchQueryDataDto.toDomain(): SearchResultBundle = SearchResultBundle(
        query = query,
        products = products.map { it.toDomain() },
        page = pagination.page,
        total = pagination.total,
        totalPages = pagination.totalPages
    )

    fun SearchSuggestionsDataDto.toDomain(): SearchSuggestionsBundle = SearchSuggestionsBundle(
        suggestions = suggestions,
        trending = trending,
        recent = recent
    )

    fun DeliveryCheckDataDto.toDomain(): DeliveryServiceability = DeliveryServiceability(
        pincode = pincode,
        city = city,
        state = state,
        isServiceable = isServiceable,
        expressAvailable = expressAvailable,
        codAvailable = codAvailable,
        estimatedDaysMin = estimatedDaysMin,
        estimatedDaysMax = estimatedDaysMax,
        deliveryMessage = deliveryMessage
    )

    fun ReviewListDataDto.toDomain(): ReviewListBundle = ReviewListBundle(
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
        eligibleOrderItems = eligibleOrderItems.map {
            ReviewEligibilityItem(
                orderItemId = it.orderItemId,
                orderId = it.orderId,
                orderNumber = it.orderNumber,
                productName = it.productName,
                orderedAt = it.orderedAt
            )
        },
        total = pagination.total
    )

    fun HelpTopicDto.toDomain(): HelpTopic = HelpTopic(
        topicId = topicId,
        slug = slug,
        title = title,
        subtitle = subtitle,
        iconKey = iconKey,
        category = category,
        requiresOrder = requiresOrder
    )

    fun HelpFlowDataDto.toDomain(): HelpFlowDetail? {
        val t = topic?.toDomain() ?: return null
        val f = flow?.let { fl ->
            HelpFlow(
                flowId = fl.flowId,
                title = fl.title,
                startNodeId = fl.startNodeId,
                nodes = fl.nodes.map { n ->
                    HelpFlowNode(
                        nodeId = n.nodeId,
                        nodeKey = n.nodeKey,
                        nodeType = n.nodeType,
                        title = n.title,
                        bodyText = n.bodyText,
                        policyText = n.policyText,
                        requiresOrderSelect = n.requiresOrderSelect,
                        requiresItemSelect = n.requiresItemSelect,
                        allowsPhotoUpload = n.allowsPhotoUpload,
                        allowsNoteInput = n.allowsNoteInput,
                        isTerminal = n.isTerminal,
                        canEscalateTicket = n.canEscalateTicket,
                        options = n.options.map { opt ->
                            HelpFlowOption(
                                optionId = opt.optionId,
                                label = opt.label,
                                nextNodeId = opt.nextNodeId,
                                actionKey = opt.actionKey,
                                escalateToTicket = opt.escalateToTicket
                            )
                        }
                    )
                }
            )
        }
        return HelpFlowDetail(topic = t, flow = f)
    }

    fun HelpSessionDataDto.toDomain(): HelpSessionResult = HelpSessionResult(
        sessionToken = sessionToken,
        orderReport = orderReport?.let { rep ->
            HelpOrderReport(
                orderId = rep.orderId,
                orderNumber = rep.orderNumber,
                orderStatus = rep.orderStatus,
                paymentStatus = rep.paymentStatus,
                paymentMethod = rep.paymentMethod,
                trackingNumber = rep.trackingNumber,
                courierName = rep.courierName,
                trackingUrl = rep.trackingUrl,
                estimatedDeliveryDate = rep.estimatedDeliveryDate,
                returnEligible = rep.returnEligible,
                cancelEligible = rep.cancelEligible,
                statusExplanation = rep.statusExplanation,
                policySummary = rep.policySummary,
                items = rep.items.map {
                    HelpOrderItemSnapshot(
                        orderItemId = it.orderItemId,
                        productId = it.productId,
                        productName = it.productName,
                        quantity = it.quantity,
                        unitPrice = it.unitPrice
                    )
                }
            )
        },
        ticket = ticket?.let {
            HelpTicketCreated(
                ticketId = it.ticketId,
                ticketNumber = it.ticketNumber,
                status = it.status,
                note = it.note
            )
        }
    )

    fun ThemeTokensDto.toDomain(): ThemeTokenSpec = ThemeTokenSpec(
        themeKey = themeKey,
        name = name,
        mode = mode,
        backgroundColor = backgroundColor,
        surfaceColor = surfaceColor,
        surfaceElevated = surfaceElevated,
        primaryColor = primaryColor,
        accentColor = accentColor,
        secondaryAccent = secondaryAccent,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        borderColor = borderColor,
        fontStyle = fontStyle,
        buttonStyle = buttonStyle,
        cardRadiusDp = cardRadiusDp,
        borderWidthDp = borderWidthDp,
        ambientPreset = ambientPreset,
        audioTrackKey = audioTrackKey,
        motionIntensity = motionIntensity
    )

    fun OccasionDto.toDomain(): OccasionTheme = OccasionTheme(
        occasionId = occasionId,
        slug = slug,
        name = name,
        eyebrow = eyebrow,
        headline = headline,
        subtitle = subtitle,
        occasionType = occasionType,
        motifKey = motifKey,
        buttonStyle = buttonStyle,
        primaryColor = primaryColor,
        accentColor = accentColor,
        backgroundColor = backgroundColor,
        fontStyle = fontStyle,
        audioTrackKey = audioTrackKey,
        audioUrl = resolveMediaUrl(audioUrl),
        audioLoop = audioLoop,
        ctaText = ctaText,
        ctaCollectionSlug = ctaCollectionSlug,
        bannerImageUrl = resolveMediaUrl(bannerImageUrl),
        priority = priority,
        eventDate = eventDate,
        activationDaysBefore = activationDaysBefore,
        activationStartAt = activationStartAt,
        activationEndAt = activationEndAt,
        timezone = timezone.ifBlank { "Asia/Kolkata" },
        regionCountry = regionCountry.ifBlank { "ALL" },
        regionState = regionState,
        regionCity = regionCity,
        startDate = startDate,
        endDate = endDate
    )

    fun FashionWorldThemeDto.toDomain(): FashionWorldTheme = FashionWorldTheme(
        styleWorldId = styleWorldId,
        slug = slug,
        name = name,
        tagline = tagline,
        description = description,
        themeKey = themeKey,
        mode = mode,
        animationKey = animationKey,
        fontStyle = fontStyle,
        buttonStyle = buttonStyle,
        cardRadiusDp = cardRadiusDp,
        borderWidthDp = borderWidthDp,
        primaryColor = primaryColor,
        accentColor = accentColor,
        secondaryAccent = secondaryAccent,
        backgroundColor = backgroundColor,
        surfaceColor = surfaceColor,
        surfaceElevated = surfaceElevated,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        borderColor = borderColor,
        ambientPreset = ambientPreset,
        audioTrackKey = audioTrackKey,
        audioUrl = resolveMediaUrl(audioUrl),
        audioLoop = audioLoop,
        audioVolume = audioVolume.coerceIn(0.05f, 1.0f),
        sfxProfile = sfxProfile,
        heroLayout = heroLayout,
        coverImageUrl = resolveMediaUrl(coverImageUrl),
        mobileImageUrl = resolveMediaUrl(mobileImageUrl),
        videoUrl = resolveMediaUrl(videoUrl),
        promoVideoUrl = resolveMediaUrl(promoVideoUrl) ?: resolveMediaUrl(videoUrl),
        promoVideoPosterUrl = resolveMediaUrl(promoVideoPosterUrl)
            ?: resolveMediaUrl(mobileImageUrl)
            ?: resolveMediaUrl(coverImageUrl),
        promoVideoIsSponsored = promoVideoIsSponsored,
        runwayConfig = runwayConfig?.toDomain(),
        hasSubmittedFeedback = hasSubmittedFeedback,
        displayOrder = displayOrder,
        versionTag = versionTag
    )

    fun WorldVectorAssetDto.toDomain(): WorldVectorAsset = WorldVectorAsset(
        assetId = assetId,
        assetKey = assetKey,
        worldSlug = worldSlug,
        occasionSlug = occasionSlug,
        vectorType = vectorType,
        layerRole = layerRole,
        primaryHex = primaryHex,
        secondaryHex = secondaryHex,
        opacity = opacity.coerceIn(0.05f, 0.95f),
        motionPattern = motionPattern,
        imageUrl = resolveMediaUrl(imageUrl)
    )

    fun WorldMapOriginDto.toDomain(): WorldMapOriginPin = WorldMapOriginPin(
        originId = originId,
        brandId = brandId,
        brandName = brandName,
        brandSlug = brandSlug,
        brandLogo = resolveMediaUrl(brandLogo),
        coverImageUrl = resolveMediaUrl(coverImageUrl),
        tagline = tagline,
        story = story,
        city = city,
        country = country,
        countryCode = countryCode,
        latitude = latitude,
        longitude = longitude,
        worldSlug = worldSlug,
        productCount = productCount,
        isVerified = isVerified
    )

    fun SmartOfferDto.toDomain(): SmartOffer = SmartOffer(
        offerId = offerId,
        offerKey = offerKey,
        title = title,
        subtitle = subtitle,
        badgeText = badgeText,
        couponCode = couponCode,
        worldSlug = worldSlug,
        occasionSlug = occasionSlug,
        minCartValue = minCartValue,
        maxImpressionsPerSession = maxImpressionsPerSession,
        cooldownHours = cooldownHours,
        priority = priority,
        accentHex = accentHex,
        ctaText = ctaText
    )

    fun UserDynamicPreferencesDto.toDomain(): UserDynamicPreferences = UserDynamicPreferences(
        ambientAudioEnabled = ambientAudioEnabled,
        uiSfxEnabled = uiSfxEnabled,
        reducedMotion = reducedMotion,
        videoAutoplayMuted = videoAutoplayMuted,
        dataSaverMode = dataSaverMode,
        audioConsentAnswered = audioConsentAnswered,
        preferredWorldSlug = preferredWorldSlug
    )

    fun UserDynamicPreferences.toDto(): UserDynamicPreferencesDto = UserDynamicPreferencesDto(
        ambientAudioEnabled = ambientAudioEnabled,
        uiSfxEnabled = uiSfxEnabled,
        reducedMotion = reducedMotion,
        videoAutoplayMuted = videoAutoplayMuted,
        dataSaverMode = dataSaverMode,
        audioConsentAnswered = audioConsentAnswered,
        preferredWorldSlug = preferredWorldSlug
    )

    fun BootstrapDataDto.toDomain(): BootstrapConfigBundle = BootstrapConfigBundle(
        configVersion = configVersion,
        serverTimeUtc = serverTimeUtc,
        activeDesign = activeDesign,
        designSlot = designSlot.ifBlank { if (activeDesign == 1) "live" else "default" },
        designVersion = designVersion.ifBlank { "1.0.0" },
        designChecksum = designChecksum,
        designManifest = designManifest,
        defaultTheme = defaultTheme.toDomain(),
        activeOccasion = activeOccasion?.toDomain(),
        occasions = occasions.map { it.toDomain() },
        fashionWorlds = fashionWorlds.map { it.toDomain() },
        submittedWorldIds = submittedWorldIds,
        submittedWorldSlugs = submittedWorldSlugs,
        worldAssets = worldAssets.map { it.toDomain() },
        worldMapOrigins = worldMapOrigins.map { it.toDomain() }.filter { it.isVerified },
        smartOffers = smartOffers.map { it.toDomain() },
        userPreferences = userPreferences?.toDomain()
    )
}

