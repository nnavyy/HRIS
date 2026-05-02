package com.ptniger.hris.utils

import com.ptniger.hris.data.model.AutomationRule
import com.ptniger.hris.data.repository.AuditLogRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Central Automation Engine that checks whether specific features/rules
 * are enabled or disabled by the SuperAdmin via the Automation Rules screen.
 *
 * When a rule is disabled here, the corresponding feature is blocked
 * across ALL roles (Employee, Manager, Finance, HR).
 *
 * Rule types:
 * - "attendance"    → Auto-late marking on check-in
 * - "leave"         → Auto quota check on leave submission
 * - "payroll"       → KPI bonus calculation in payroll generation
 * - "notification"  → Auto-send notifications on events
 * - "account"       → Auto-create ESS account on new employee
 * - "audit"         → Auto-record sensitive data changes
 */
object AutomationEngine {
    private var cachedRules: List<AutomationRule> = emptyList()
    private var lastFetched: Long = 0
    private val cacheDurationMs = 60_000L // 1 minute cache
    private val mutex = Mutex()

    /**
     * Refresh the rules cache from Firestore.
     * Called on app start and when SuperAdmin toggles a rule.
     */
    suspend fun refreshRules() {
        mutex.withLock {
            cachedRules = AuditLogRepository().getRules()
            lastFetched = System.currentTimeMillis()
        }
    }

    /**
     * Force-set rules (used after a toggle to avoid re-fetch delay).
     */
    fun setRules(rules: List<AutomationRule>) {
        cachedRules = rules
        lastFetched = System.currentTimeMillis()
    }

    /**
     * Check if a rule type is currently active.
     * If rules haven't been fetched yet, defaults to true (safe default).
     */
    suspend fun isRuleActive(ruleType: String): Boolean {
        if (cachedRules.isEmpty() || System.currentTimeMillis() - lastFetched > cacheDurationMs) {
            try { refreshRules() } catch (_: Exception) { /* use cached */ }
        }
        // If no rule found for this type, default to active
        val rule = cachedRules.find { it.type == ruleType }
        return rule?.isActive ?: true
    }

    /**
     * Quick sync check (no suspend) using cached data only.
     * Returns true if rule is active or not found.
     */
    fun isRuleActiveCached(ruleType: String): Boolean {
        val rule = cachedRules.find { it.type == ruleType }
        return rule?.isActive ?: true
    }

    // Convenience constants for rule types
    object RuleType {
        const val ATTENDANCE = "attendance"
        const val LEAVE = "leave"
        const val PAYROLL = "payroll"
        const val NOTIFICATION = "notification"
        const val ACCOUNT = "account"
        const val AUDIT = "audit"
    }
}
