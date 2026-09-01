package com.felipelopes.cryptrade.config

import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Configuration

/**
 * Caffeine: cache manager e spec (maximumSize/expireAfterWrite) vem de
 * spring.cache.caffeine.spec no application.yml. Aqui so liga o @Cacheable.
 */
@Configuration
@EnableCaching
class CacheConfig
