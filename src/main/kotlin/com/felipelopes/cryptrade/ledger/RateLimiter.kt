package com.felipelopes.cryptrade.ledger

import org.springframework.stereotype.Component
import java.time.Duration
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

// ponytail: janela deslizante em memoria, por instancia - single node, sem Redis.
// Se escalar horizontalmente, precisa de um limiter compartilhado.
@Component
class RateLimiter {
    private val hits = ConcurrentHashMap<String, ArrayDeque<Long>>()

    fun allow(key: String, maxRequests: Int, window: Duration): Boolean {
        val now = System.currentTimeMillis()
        val windowStart = now - window.toMillis()
        val deque = hits.computeIfAbsent(key) { ArrayDeque() }
        synchronized(deque) {
            while (deque.isNotEmpty() && deque.peekFirst() < windowStart) deque.pollFirst()
            if (deque.size >= maxRequests) return false
            deque.addLast(now)
            return true
        }
    }
}
