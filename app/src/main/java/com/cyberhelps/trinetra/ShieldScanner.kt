package com.cyberhelps.trinetra

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONObject

class ShieldScanner(private val context: Context) {
    private val pm = context.packageManager

    private fun loadIocs(): Set<String> {
        val root = JSONObject(context.assets.open("stalkerware_packages.json").bufferedReader().use { it.readText() })
        val packages = root.getJSONObject("packages")
        return packages.keys().asSequence().toSet()
    }

    @Suppress("DEPRECATION")
    fun scan(onProgress: (Int, Int) -> Unit): List<AppRisk> {
        val installed = if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }
        val iocs = loadIocs()
        val visibilityLimitations = RiskEngine.visibilityLimitations(Build.VERSION.SDK_INT)
        return installed.mapIndexed { index, info ->
            onProgress(index + 1, installed.size)
            val app = info.applicationInfo
            val label = if (app != null) runCatching { pm.getApplicationLabel(app).toString() }.getOrDefault(info.packageName) else info.packageName

            val installer = runCatching {
                val value = if (Build.VERSION.SDK_INT >= 30) {
                    pm.getInstallSourceInfo(info.packageName).installingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstallerPackageName(info.packageName)
                }
                ObservedSignal.available(value)
            }.getOrElse { ObservedSignal.unavailable() }

            // GET_PERMISSIONS was requested above. A null array is kept unknown rather than silently treated as false.
            val permissions = info.requestedPermissions
                ?.let { ObservedSignal.available(it.toSet()) }
                ?: ObservedSignal.unavailable()

            RiskEngine.assess(
                label = label,
                packageName = info.packageName,
                installerSource = installer,
                requestedPermissions = permissions,
                knownIoc = info.packageName in iocs,
                limitations = visibilityLimitations
            )
        }.sortedWith(compareByDescending<AppRisk> { it.score }.thenBy { it.label.lowercase() })
    }
}
