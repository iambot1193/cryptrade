package com.felipelopes.cryptrade.repository

import com.felipelopes.cryptrade.domain.Order
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long>
