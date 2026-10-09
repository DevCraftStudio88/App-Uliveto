package com.example

import com.example.data.*
import com.example.notifications.ReminderNotificationManager
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testPolygonAreaCalculation() {
        val p1 = GeoPoint(40.0, 16.0)
        val p2 = GeoPoint(40.0, 16.001)
        val p3 = GeoPoint(40.001, 16.001)
        val p4 = GeoPoint(40.001, 16.0)

        val (m2, ha) = FarmRepository.calculatePolygonArea(listOf(p1, p2, p3, p4))
        assertTrue(m2 > 0)
        assertTrue(ha > 0)
    }

    @Test
    fun testRecurrenceCalculation() {
        val nextDaily = ReminderNotificationManager.computeNextRecurrenceDate(
            currentDateStr = "10/10/2026",
            recurrence = RecurrenceType.OGNI_GIORNO,
            customInterval = 1,
            customUnit = "GIORNI",
            endDateStr = null
        )
        assertEquals("11/10/2026", nextDaily)

        val nextWeekly = ReminderNotificationManager.computeNextRecurrenceDate(
            currentDateStr = "10/10/2026",
            recurrence = RecurrenceType.OGNI_SETTIMANA,
            customInterval = 1,
            customUnit = "SETTIMANE",
            endDateStr = null
        )
        assertEquals("17/10/2026", nextWeekly)

        val nextCustomMonths = ReminderNotificationManager.computeNextRecurrenceDate(
            currentDateStr = "15/01/2026",
            recurrence = RecurrenceType.PERSONALIZZATA,
            customInterval = 2,
            customUnit = "MESI",
            endDateStr = null
        )
        assertEquals("15/03/2026", nextCustomMonths)
    }

    @Test
    fun testReminderTriggerTime() {
        val triggerMs = ReminderNotificationManager.calculateTriggerTimeMs(
            dateStr = "15/10/2026",
            timeStr = "10:30",
            offsetMinutes = 15
        )
        assertTrue(triggerMs > 0L)
    }

    @Test
    fun testDefaultSettings() {
        val settings = FarmRepository.settings.value
        assertEquals("Il mio assistente", settings.assistant.assistantName)
        assertEquals(TextSizeOption.NORMALE, settings.textSize)
        assertTrue(settings.remindersEnabled)
        assertTrue(settings.notificationSettings.soundEnabled)
        assertEquals("DETTAGLI", settings.notificationSettings.privacyMode)
    }

    @Test
    fun testCreateAndCompleteReminder() {
        val initialCount = FarmRepository.reminders.value.size
        val reminder = ReminderItem(
            title = "Irrigazione Ulivi Nord",
            description = "Aprire valvola settore 2",
            date = "15/10/2026",
            time = "08:00",
            priority = ReminderPriority.ALTA,
            category = "Irrigazione"
        )
        FarmRepository.addReminder(reminder, null)

        val afterAdd = FarmRepository.reminders.value
        assertEquals(initialCount + 1, afterAdd.size)
        val added = afterAdd.first { it.id == reminder.id }
        assertEquals(ReminderStatus.ATTIVO, added.status)
        assertEquals("Irrigazione Ulivi Nord", added.title)

        // Complete it
        FarmRepository.toggleReminderCompleted(reminder.id, null)
        val afterComplete = FarmRepository.reminders.value.first { it.id == reminder.id }
        assertEquals(ReminderStatus.COMPLETATO, afterComplete.status)
        assertNotNull(afterComplete.completedAt)

        // Reopen it
        FarmRepository.toggleReminderCompleted(reminder.id, null)
        val afterReopen = FarmRepository.reminders.value.first { it.id == reminder.id }
        assertEquals(ReminderStatus.ATTIVO, afterReopen.status)

        // Cleanup
        FarmRepository.deleteReminder(reminder.id, null)
        assertEquals(initialCount, FarmRepository.reminders.value.size)
    }

    @Test
    fun testSnoozeDoesNotCreateDuplicates() {
        val reminder = ReminderItem(
            title = "Trattamento Rameico",
            date = "20/10/2026",
            time = "09:00",
            priority = ReminderPriority.URGENTE
        )
        FarmRepository.addReminder(reminder, null)
        val countBefore = FarmRepository.reminders.value.size

        // Posticipa di 30 minuti
        FarmRepository.snoozeReminder(reminder.id, offsetMinutes = 30, context = null)
        val countAfter30 = FarmRepository.reminders.value.size
        assertEquals("Il posticipo non deve creare duplicati!", countBefore, countAfter30)

        val snoozed30 = FarmRepository.reminders.value.first { it.id == reminder.id }
        assertEquals("20/10/2026", snoozed30.date)
        assertEquals("09:30", snoozed30.time)

        // Posticipa a data e ora personalizzata
        FarmRepository.snoozeReminder(reminder.id, newDateStr = "25/10/2026", newTimeStr = "14:00", context = null)
        val countAfterCustom = FarmRepository.reminders.value.size
        assertEquals("Il posticipo a data custom non deve creare duplicati!", countBefore, countAfterCustom)

        val snoozedCustom = FarmRepository.reminders.value.first { it.id == reminder.id }
        assertEquals("25/10/2026", snoozedCustom.date)
        assertEquals("14:00", snoozedCustom.time)

        // Cleanup
        FarmRepository.deleteReminder(reminder.id, null)
    }

    @Test
    fun testRecurrenceEndDateRespected() {
        // Date after end date should return null
        val next = ReminderNotificationManager.computeNextRecurrenceDate(
            currentDateStr = "30/10/2026",
            recurrence = RecurrenceType.OGNI_GIORNO,
            customInterval = 1,
            customUnit = "GIORNI",
            endDateStr = "30/10/2026"
        )
        assertNull(next)
    }
}
