package com.cyberhelps.trinetra

import android.Manifest
import org.junit.Assert.*
import org.junit.Test

class RiskEngineTest {
    @Test fun knownIocIsMalicious() { assertEquals(RiskLevel.MALICIOUS, RiskEngine.assess("x","com.spy.x","store", emptySet(),true).level) }
    @Test fun benignStoreAppIsSafe() { assertEquals(RiskLevel.SAFE, RiskEngine.assess("Notes","org.example.notes","com.android.vending", emptySet(),false).level) }
    @Test fun surveillanceClusterIsHighRisk() { val p=setOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.READ_SMS,Manifest.permission.READ_CALL_LOG,Manifest.permission.CAMERA,Manifest.permission.READ_CONTACTS); assertEquals(RiskLevel.HIGH_RISK,RiskEngine.assess("Helper","org.example.helper","",p,false).level) }
}
