package com.ailegorreta.configservice

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import java.time.LocalTime

@Component("lucky")
class LuckyHealthIndicator : HealthIndicator {
    override fun health(): Health {
        val currentTime = LocalTime.now()
        val healthBuilder = if (currentTime.minute % 2 == 0) Health.up() else Health.down().withDetail("time", currentTime)
        return healthBuilder.build()
    }
}