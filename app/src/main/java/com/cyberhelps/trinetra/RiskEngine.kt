package com.cyberhelps.trinetra

import android.Manifest

enum class RiskLevel(val label: String) { SAFE("SAFE"), SUSPICIOUS("SUSPICIOUS"), HIGH_RISK("HIGH RISK"), MALICIOUS("MALICIOUS") }

data class AppRisk(
    val label: String,
    val packageName: String,
    val score: Int,
    val level: RiskLevel,
    val reasons: List<String>,
    val sensitivePermissions: List<String>,
    val installer: String
)

object RiskEngine {
    val dangerous = setOf(
        Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG,
        Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE, Manifest.permission.READ_EXTERNAL_STORAGE,
        "android.permission.MANAGE_EXTERNAL_STORAGE"
    )
    private val sideloadInstallers = setOf("", "null", "com.android.packageinstaller", "com.google.android.packageinstaller", "com.samsung.android.packageinstaller")
    private val systemLike = Regex("(?i)(system|update|sync|service|security|device|parental|monitor)")

    fun classify(score: Int) = when {
        score >= 100 -> RiskLevel.MALICIOUS
        score >= 50 -> RiskLevel.HIGH_RISK
        score >= 20 -> RiskLevel.SUSPICIOUS
        else -> RiskLevel.SAFE
    }

    fun assess(label: String, packageName: String, installer: String?, requestedPermissions: Set<String>, knownIoc: Boolean): AppRisk {
        var score = 0
        val reasons = mutableListOf<String>()
        val sensitive = requestedPermissions.intersect(dangerous)
        if (knownIoc) { score += 100; reasons += "Exact package-name match in the public stalkerware IOC list" }
        if (sensitive.size >= 6) { score += 25; reasons += "Requests ${sensitive.size} surveillance-relevant permissions" }
        else if (sensitive.size >= 3) { score += 12; reasons += "Requests ${sensitive.size} surveillance-relevant permissions" }
        if (requestedPermissions.contains(Manifest.permission.RECORD_AUDIO) &&
            requestedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION) &&
            (requestedPermissions.contains(Manifest.permission.READ_SMS) || requestedPermissions.contains(Manifest.permission.READ_CALL_LOG))) {
            score += 25; reasons += "High-risk microphone + location + communications permission combination"
        }
        val source = installer.orEmpty()
        if (source in sideloadInstallers) { score += 10; reasons += "Unknown or package-installer source (possible sideload)" }
        if (!knownIoc && systemLike.containsMatchIn(packageName)) { score += 8; reasons += "Generic system-like package name (weak heuristic)" }
        return AppRisk(label, packageName, score, classify(score), reasons, sensitive.sorted(), source.ifBlank { "unknown" })
    }
}
