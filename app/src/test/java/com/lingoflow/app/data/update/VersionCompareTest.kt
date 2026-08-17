package com.lingoflow.app.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCompareTest {

    @Test
    fun `newer versions are detected`() {
        assertTrue(VersionCompare.isNewer("1.1.0", "1.0.0"))
        assertTrue(VersionCompare.isNewer("2.0", "1.9.9"))
        assertTrue(VersionCompare.isNewer("1.0.1", "1.0.0"))
    }

    @Test
    fun `same and older versions are not newer`() {
        assertFalse(VersionCompare.isNewer("1.0.0", "1.0.0"))
        assertFalse(VersionCompare.isNewer("1.0", "1.0.0"))
        assertFalse(VersionCompare.isNewer("0.9.9", "1.0.0"))
    }

    @Test
    fun `leading v and suffixes are tolerated`() {
        assertTrue(VersionCompare.isNewer("v1.0.1", "1.0.0"))
        assertFalse(VersionCompare.isNewer("v1.0.0", "1.0.0"))
    }
}
