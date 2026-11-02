package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ABSEN KIKAV 2", appName)
  }

  @Test
  fun `verifyAndRepairAllData executes successfully`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.repository.AttendanceRepository(context)
    val report = repo.verifyAndRepairAllData("2026-11-02")
    org.junit.Assert.assertTrue(report.success)
  }

  @Test
  fun `export and import config json string succeeds`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.repository.AttendanceRepository(context)
    val initialData = repo.getInitialAttendance()
    val json = repo.exportAppConfigJsonString("2026-11-02", com.example.data.model.WaktuApel.PAGI, initialData)
    org.junit.Assert.assertTrue(json.contains("customStatuses") || json.contains("platoonRoster"))

    val importResult = repo.importAppConfigFromJson(json)
    org.junit.Assert.assertTrue(importResult.success)
  }

  @Test
  fun `malam siaga report is combined into 1 report with Remaja grouped separately`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.repository.AttendanceRepository(context)
    val date = "2026-11-02"
    val malamData = repo.loadAttendance(date, com.example.data.model.WaktuApel.MALAM_SIAGA)
    val statuses = repo.loadCustomStatuses()
    val config = repo.loadReportConfig()

    // 1. Report contains both KELOMPOK REMAJA and PELETON SIAGA in 1 report
    val report = repo.generateFormattedReport(
      date = date,
      waktuApel = com.example.data.model.WaktuApel.MALAM_SIAGA,
      data = malamData,
      customStatuses = statuses,
      config = config
    )
    org.junit.Assert.assertTrue(report.contains("KELOMPOK REMAJA"))
    org.junit.Assert.assertTrue(report.contains("PELETON SIAGA"))
    org.junit.Assert.assertTrue(report.contains("JUMLAH KESELURUHAN"))

    // Kuat Nyata combines both (43 Remaja + companion unit)
    val totalCount = malamData.values.sumOf { it.size }
    org.junit.Assert.assertTrue(report.contains("Kuat Nyata : $totalCount Orang"))

    // 2. Word document export produces unified document
    val wordFile = repo.exportToWordFile(
      date = date,
      waktuApel = com.example.data.model.WaktuApel.MALAM_SIAGA,
      data = malamData,
      customStatuses = statuses,
      config = config
    )
    org.junit.Assert.assertTrue(wordFile.exists())
    val htmlContent = wordFile.readText(Charsets.UTF_8)
    org.junit.Assert.assertTrue(htmlContent.contains("KELOMPOK REMAJA"))
    org.junit.Assert.assertTrue(htmlContent.contains("PELETON SIAGA"))
    org.junit.Assert.assertTrue(htmlContent.contains("JUMLAH KESELURUHAN"))
  }
}
