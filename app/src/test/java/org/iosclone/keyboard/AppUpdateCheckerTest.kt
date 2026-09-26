package org.iosclone.keyboard

import org.iosclone.keyboard.settings.AppUpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun testVersionComparison() {
        assertTrue(AppUpdateChecker.compareVersions("1.2.5", "1.2.1") > 0)
        assertTrue(AppUpdateChecker.compareVersions("1.3.0", "1.2.5") > 0)
        assertTrue(AppUpdateChecker.compareVersions("2.0.0", "1.9.9") > 0)
        assertEquals(0, AppUpdateChecker.compareVersions("1.2.5", "1.2.5"))
        assertTrue(AppUpdateChecker.compareVersions("1.2.1", "1.2.5") < 0)
    }
}
