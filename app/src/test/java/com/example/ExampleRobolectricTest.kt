package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.analyzer.ImportMappingDatabase
import com.example.engine.analyzer.PythonProjectScanner
import com.example.engine.analyzer.PythonStdLib
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ATP Python", appName)
  }

  @Test
  fun `verify python stdlib detection`() {
    // os, sys, json, math, datetime, asyncio must be identified as standard library
    assertTrue(PythonStdLib.isStdLib("os"))
    assertTrue(PythonStdLib.isStdLib("sys"))
    assertTrue(PythonStdLib.isStdLib("json"))
    assertTrue(PythonStdLib.isStdLib("math"))
    assertTrue(PythonStdLib.isStdLib("datetime"))
    assertTrue(PythonStdLib.isStdLib("asyncio"))

    // Third-party packages must NOT be identified as standard library
    assertFalse(PythonStdLib.isStdLib("requests"))
    assertFalse(PythonStdLib.isStdLib("PIL"))
    assertFalse(PythonStdLib.isStdLib("Pillow"))
    assertFalse(PythonStdLib.isStdLib("telegram"))
    assertFalse(PythonStdLib.isStdLib("kivy"))
    assertFalse(PythonStdLib.isStdLib("kivymd"))
  }

  @Test
  fun `verify known import mappings`() {
    // PIL -> Pillow
    val pilMapping = ImportMappingDatabase.findMapping("PIL")
    assertNotNull(pilMapping)
    assertEquals("Pillow", pilMapping?.packageName)

    // telegram -> python-telegram-bot
    val tgMapping = ImportMappingDatabase.findMapping("telegram", "from telegram.ext import Application")
    assertNotNull(tgMapping)
    assertEquals("python-telegram-bot", tgMapping?.packageName)

    // yaml -> PyYAML
    val yamlMapping = ImportMappingDatabase.findMapping("yaml")
    assertNotNull(yamlMapping)
    assertEquals("PyYAML", yamlMapping?.packageName)

    // color -> ambiguous (multiple candidate packages)
    val colorMapping = ImportMappingDatabase.findMapping("color")
    assertNotNull(colorMapping)
    assertTrue(colorMapping!!.ambiguousCandidates.isNotEmpty())
    assertTrue(colorMapping.packageName.isEmpty())
  }
}
