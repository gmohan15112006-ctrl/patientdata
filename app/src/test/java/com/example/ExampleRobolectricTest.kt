package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.SecurityUtils
import com.example.excel.ExcelExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Hospital PDMS", appName)
  }

  @Test
  fun `verify password hashing and verification`() {
    val plain = "SecureDoctorPass123"
    val hash = SecurityUtils.hashPassword(plain)
    assertTrue(SecurityUtils.verifyPassword(plain, hash))
  }

  @Test
  fun `verify required excel columns count and order`() {
    val headers = ExcelExporter.REQUIRED_HEADERS
    assertEquals(15, headers.size)
    assertEquals("S NO", headers[0])
    assertEquals("IP NO", headers[1])
    assertEquals("NAME", headers[2])
    assertEquals("AGE", headers[3])
    assertEquals("SEX", headers[4])
    assertEquals("ADMISSION DATE & TIME", headers[5])
    assertEquals("PATIENT RECEIVED TIME", headers[6])
    assertEquals("BROAD SPECIALITY CATEGORY", headers[7])
    assertEquals("DIAGNOSIS", headers[8])
    assertEquals("AGE INTERVAL", headers[9])
    assertEquals("TAEI PILLAR/TAEI NON PILLAR", headers[10])
    assertEquals("MEDICOLEGAL CATEGORY", headers[11])
    assertEquals("TRANSFERRED OUT", headers[12])
    assertEquals("TRANSFERRED OUT TIME", headers[13])
    assertEquals("EMERGENCY RESPONSE TIME", headers[14])
  }
}
