package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.parser.ConfigParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("پروکسی هاب", appName)
    }

    @Test
    fun testConfigAndProxyParser() {
        val sampleText = """
            🔥 تست کانفیگ VLESS:
            vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@1.1.1.1:443?security=tls&type=ws&sni=example.com#Germany_Fast

            🚀 پروکسی MTProto تلگرام:
            tg://proxy?server=proxy.digitalresistance.dog&port=443&secret=ee112233445566778899aabbccddeeff11
        """.trimIndent()

        val result = ConfigParser.extractAll(sampleText, "TestChannel")

        assertEquals(1, result.configs.size)
        assertEquals("1.1.1.1", result.configs[0].server)
        assertEquals(443, result.configs[0].port)
        assertEquals("Germany_Fast", result.configs[0].remark)

        assertEquals(1, result.proxies.size)
        assertEquals("proxy.digitalresistance.dog", result.proxies[0].server)
        assertEquals(443, result.proxies[0].port)
        assertEquals("ee112233445566778899aabbccddeeff11", result.proxies[0].secret)
    }
}
