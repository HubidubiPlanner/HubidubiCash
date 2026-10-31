package com.hubidubicash.desktop

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformTest {
    @Test
    fun `the data folder is where each system keeps an app's data`() {
        assertEquals(File("/Users/a/Library/Application Support/HubidubiCash"), Platform.dataDir("/Users/a", null, mac = true))
        assertEquals(File("/home/a/.local/share/hubidubicash"), Platform.dataDir("/home/a", null, mac = false))
        assertEquals(File("/data/hubidubicash"), Platform.dataDir("/home/a", "/data", mac = false))
        assertEquals(File("/home/a/.local/share/hubidubicash"), Platform.dataDir("/home/a", " ", mac = false))
    }
}
