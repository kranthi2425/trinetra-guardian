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
        val flags = PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
        val installed = if (Build.VERSION.SDK_INT >= 33) pm.getInstalledPackages(flags)
            else pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val iocs = loadIocs()
        return installed.mapIndexed { index, info ->
            onProgress(index + 1, installed.size)
            val app = info.applicationInfo
            val label = if (app != null) runCatching { pm.getApplicationLabel(app).toString() }.getOrDefault(info.packageName) else info.packageName
            val installer = if (Build.VERSION.SDK_INT >= 30) runCatching { pm.getInstallSourceInfo(info.packageName).installingPackageName }.getOrNull()
                else @Suppress("DEPRECATION") pm.getInstallerPackageName(info.packageName)
            RiskEngine.assess(label, info.packageName, installer, info.requestedPermissions?.toSet().orEmpty(), info.packageName in iocs)
        }.sortedWith(compareByDescending<AppRisk> { it.score }.thenBy { it.label.lowercase() })
    }
}
