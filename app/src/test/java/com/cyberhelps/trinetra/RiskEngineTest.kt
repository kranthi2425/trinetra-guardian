package com.cyberhelps.trinetra

import android.Manifest
import org.junit.Assert.*
import org.junit.Test

class RiskEngineTest {
    private val store = ObservedSignal.available<String?>("com.android.vending")
    private val noPermissions = ObservedSignal.available(emptySet<String>())

    @Test fun unavailableInstallerGetsNoSideloadPoints() {
        val result = RiskEngine.assess("Notes", "org.example.notes", ObservedSignal.unavailable(), noPermissions, false)
        assertEquals(0, result.score)
        assertEquals(RiskEngine.UNAVAILABLE_EXPLANATION, result.installer)
        assertTrue(result.unavailableSignals.any { it.startsWith("Installer source:") })
    }

    @Test fun unavailablePermissionsStayUnknownAndDoNotScore() {
        val result = RiskEngine.assess("Notes", "org.example.notes", store, ObservedSignal.unavailable(), false)
        assertEquals(0, result.score)
        assertTrue(result.sensitivePermissions.isEmpty())
        assertTrue(result.unavailableSignals.any { it.startsWith("Requested permissions:") })
    }

    @Test fun knownIocUsesPreciseUserFacingLabelAndCaveat() {
        val result = RiskEngine.assess("Example", "com.spy.example", store, noPermissions, true)
        assertEquals(RiskLevel.MALICIOUS, result.level)
        assertEquals("KNOWN IOC MATCH", result.level.label)
        assertTrue(result.reasons.any { it.contains("not forensic proof") })
    }

    @Test fun benignPlayInstalledAppIsSafe() {
        val result = RiskEngine.assess("Notes", "org.example.notes", store, noPermissions, false)
        assertEquals(RiskLevel.SAFE, result.level)
        assertEquals(0, result.score)
    }

    @Test fun legitimateParentalControlPatternGetsContextCaveat() {
        val permissions = ObservedSignal.available(setOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CAMERA
        ))
        val result = RiskEngine.assess("Family Safety", "com.vendor.family", store, permissions, false)
        assertEquals(RiskLevel.SAFE, result.level)
        assertTrue(result.reasons.contains(RiskEngine.LEGITIMATE_USE_CAVEAT))
    }

    @Test fun surveillanceClusterIsHighRiskButNotProof() {
        val permissions = ObservedSignal.available(setOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS
        ))
        val result = RiskEngine.assess("Helper", "org.example.helper", ObservedSignal.available<String?>("com.android.packageinstaller"), permissions, false)
        assertEquals(RiskLevel.HIGH_RISK, result.level)
        assertTrue(result.reasons.contains(RiskEngine.LEGITIMATE_USE_CAVEAT))
    }

    @Test fun android11AndLaterExposeVisibilityLimitation() {
        assertTrue(RiskEngine.visibilityLimitations(30).contains(RiskEngine.PACKAGE_VISIBILITY_LIMITATION))
        assertTrue(RiskEngine.visibilityLimitations(35).contains(RiskEngine.PACKAGE_VISIBILITY_LIMITATION))
        assertTrue(RiskEngine.visibilityLimitations(29).isEmpty())
    }
}
