package com.cyberhelps.trinetra

import android.Manifest

enum class RiskLevel(val label: String) {
    SAFE("SAFE"), SUSPICIOUS("SUSPICIOUS"), HIGH_RISK("HIGH RISK"),
    // Kept as the fourth internal tier for compatibility; the user-facing label is precise.
    MALICIOUS("KNOWN IOC MATCH")
}

enum class SignalAvailability { AVAILABLE, UNAVAILABLE }

data class ObservedSignal<T>(
    val availability: SignalAvailability,
    val value: T? = null,
    val explanation: String? = null
) {
    companion object {
        fun <T> available(value: T): ObservedSignal<T> = ObservedSignal(SignalAvailability.AVAILABLE, value)
        fun <T> unavailable(explanation: String = RiskEngine.UNAVAILABLE_EXPLANATION): ObservedSignal<T> =
            ObservedSignal(SignalAvailability.UNAVAILABLE, null, explanation)
    }
}

data class AppRisk(
    val label: String,
    val packageName: String,
    val score: Int,
    val level: RiskLevel,
    val reasons: List<String>,
    val sensitivePermissions: List<String>,
    val installer: String,
    val unavailableSignals: List<String> = emptyList(),
    val limitations: List<String> = emptyList()
)

object RiskEngine {
    const val UNAVAILABLE_EXPLANATION = "Unavailable on this device/API"
    const val LEGITIMATE_USE_CAVEAT = "Permission patterns can also belong to legitimate parental-control, accessibility, antivirus, device-management, or OEM apps. Review context before acting."
    const val PACKAGE_VISIBILITY_LIMITATION = "Android 11+ can limit package inventory unless QUERY_ALL_PACKAGES is granted for this core security function; scan coverage may be incomplete."

    val dangerous = setOf(
        Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG,
        Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE, Manifest.permission.READ_EXTERNAL_STORAGE,
        "android.permission.MANAGE_EXTERNAL_STORAGE"
    )
    private val packageInstallerSources = setOf(
        "com.android.packageinstaller", "com.google.android.packageinstaller", "com.samsung.android.packageinstaller"
    )
    private val systemLike = Regex("(?i)(system|update|sync|service|security|device|parental|monitor)")

    fun classify(score: Int) = when {
        score >= 100 -> RiskLevel.MALICIOUS
        score >= 50 -> RiskLevel.HIGH_RISK
        score >= 20 -> RiskLevel.SUSPICIOUS
        else -> RiskLevel.SAFE
    }

    fun visibilityLimitations(androidSdk: Int): List<String> =
        if (androidSdk >= 30) listOf(PACKAGE_VISIBILITY_LIMITATION) else emptyList()

    fun assess(
        label: String,
        packageName: String,
        installerSource: ObservedSignal<String?>,
        requestedPermissions: ObservedSignal<Set<String>>,
        knownIoc: Boolean,
        limitations: List<String> = emptyList()
    ): AppRisk {
        var score = 0
        val reasons = mutableListOf<String>()
        val unavailable = mutableListOf<String>()
        var permissionHeuristicUsed = false

        if (knownIoc) {
            score += 100
            reasons += "KNOWN IOC MATCH: exact package-name match in the bundled public stalkerware indicator snapshot. This is a package-name signal, not forensic proof."
        }

        val permissions = if (requestedPermissions.availability == SignalAvailability.AVAILABLE) {
            requestedPermissions.value.orEmpty()
        } else {
            unavailable += "Requested permissions: ${requestedPermissions.explanation ?: UNAVAILABLE_EXPLANATION}"
            emptySet()
        }
        val sensitive = permissions.intersect(dangerous)
        if (requestedPermissions.availability == SignalAvailability.AVAILABLE) {
            if (sensitive.size >= 6) {
                score += 25; permissionHeuristicUsed = true
                reasons += "Requests ${sensitive.size} surveillance-relevant permissions"
            } else if (sensitive.size >= 3) {
                score += 12; permissionHeuristicUsed = true
                reasons += "Requests ${sensitive.size} surveillance-relevant permissions"
            }
            if (permissions.contains(Manifest.permission.RECORD_AUDIO) &&
                permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION) &&
                (permissions.contains(Manifest.permission.READ_SMS) || permissions.contains(Manifest.permission.READ_CALL_LOG))) {
                score += 25; permissionHeuristicUsed = true
                reasons += "High-risk microphone + location + communications permission combination"
            }
        }

        val installerDisplay = when (installerSource.availability) {
            SignalAvailability.UNAVAILABLE -> {
                unavailable += "Installer source: ${installerSource.explanation ?: UNAVAILABLE_EXPLANATION}"
                UNAVAILABLE_EXPLANATION
            }
            SignalAvailability.AVAILABLE -> {
                val source = installerSource.value
                if (source in packageInstallerSources) {
                    score += 10
                    reasons += "Package-installer source (possible sideload)"
                }
                source ?: "Not reported by Android"
            }
        }

        if (!knownIoc && systemLike.containsMatchIn(packageName)) {
            score += 8
            reasons += "Generic system-like package name (weak heuristic)"
        }
        if (permissionHeuristicUsed) reasons += LEGITIMATE_USE_CAVEAT

        return AppRisk(
            label = label,
            packageName = packageName,
            score = score,
            level = classify(score),
            reasons = reasons,
            sensitivePermissions = sensitive.sorted(),
            installer = installerDisplay,
            unavailableSignals = unavailable,
            limitations = limitations
        )
    }
}
