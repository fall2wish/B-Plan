package cn.edu.bnbu.bplan

import cn.edu.bnbu.bplan.data.model.PeriodSlot
import org.junit.Assert.assertEquals
import org.junit.Test

class PeriodSlotTest {

    private fun sortAndRenumberSlots(slots: List<PeriodSlot>): List<PeriodSlot> {
        val sorted = slots.sortedWith(
            compareBy<PeriodSlot> { it.startMinutes }
                .thenBy { it.endMinutes }
        )
        return sorted.mapIndexed { index, slot ->
            slot.copy(periodNumber = index + 1)
        }
    }

    @Test
    fun testRule2SortingAndRenumbering() {
        val slots = listOf(
            PeriodSlot(periodNumber = 99, label = "晚间大课", startTime = "18:00", endTime = "20:50"),
            PeriodSlot(periodNumber = 50, label = "TA 辅导课", startTime = "18:00", endTime = "19:00"),
            PeriodSlot(periodNumber = 10, label = "上午大课", startTime = "09:00", endTime = "11:50"),
            PeriodSlot(periodNumber = 20, label = "下午大课", startTime = "14:00", endTime = "16:50")
        )

        val renumbered = sortAndRenumberSlots(slots)

        assertEquals(4, renumbered.size)
        // 1st: 09:00 - 11:50
        assertEquals(1, renumbered[0].periodNumber)
        assertEquals("09:00", renumbered[0].startTime)
        assertEquals("11:50", renumbered[0].endTime)

        // 2nd: 14:00 - 16:50
        assertEquals(2, renumbered[1].periodNumber)
        assertEquals("14:00", renumbered[1].startTime)

        // 3rd: 18:00 - 19:00 (shorter duration sorted before 18:00 - 20:50)
        assertEquals(3, renumbered[2].periodNumber)
        assertEquals("18:00", renumbered[2].startTime)
        assertEquals("19:00", renumbered[2].endTime)
        assertEquals("TA 辅导课", renumbered[2].label)

        // 4th: 18:00 - 20:50
        assertEquals(4, renumbered[3].periodNumber)
        assertEquals("18:00", renumbered[3].startTime)
        assertEquals("20:50", renumbered[3].endTime)
        assertEquals("晚间大课", renumbered[3].label)
    }

    @Test
    fun testSlotDurationComputation() {
        val slot1 = PeriodSlot(periodNumber = 1, label = "TA", startTime = "18:00", endTime = "19:00")
        assertEquals(60, slot1.durationMinutes)

        val slot2 = PeriodSlot(periodNumber = 2, label = "Major", startTime = "09:00", endTime = "11:50")
        assertEquals(170, slot2.durationMinutes)
    }
}
