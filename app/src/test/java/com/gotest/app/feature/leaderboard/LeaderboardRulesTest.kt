package com.gotest.app.feature.leaderboard

import com.gotest.app.feature.leaderboard.domain.LeaderboardRules
import com.gotest.app.feature.leaderboard.domain.NamedOption
import com.gotest.app.feature.leaderboard.domain.TaxonomyBranch
import com.gotest.app.feature.leaderboard.domain.TaxonomyGroup
import com.gotest.app.feature.leaderboard.domain.TaxonomyHierarchy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class LeaderboardRulesTest {
    private val subjects =
        listOf(
            NamedOption(1, "Penalaran Umum"),
            NamedOption(2, "Matematika"),
            NamedOption(3, "Fisika"),
            NamedOption(4, "Kimia"),
        )

    private val hierarchy =
        TaxonomyHierarchy(
            groups =
                listOf(
                    TaxonomyGroup(id = 10, categoryId = 7, name = "TPS", status = "active", orderNumber = 1),
                    TaxonomyGroup(id = 11, categoryId = 7, name = "Lama", status = "inactive", orderNumber = 0),
                ),
            branches =
                listOf(
                    TaxonomyBranch(id = 1, categoryId = 7, subjectId = 2, status = "active", orderNumber = 2),
                    TaxonomyBranch(
                        id = 2,
                        categoryId = 7,
                        subjectId = 1,
                        groupId = 10,
                        status = "active",
                        orderNumber = 1,
                    ),
                    TaxonomyBranch(
                        id = 3,
                        categoryId = 7,
                        subjectId = 3,
                        groupId = 11,
                        status = "active",
                        orderNumber = 0,
                    ),
                    TaxonomyBranch(id = 4, categoryId = 7, subjectId = 4, status = "inactive", orderNumber = 0),
                    TaxonomyBranch(id = 5, categoryId = 8, subjectId = 3, status = "active", orderNumber = 0),
                    TaxonomyBranch(id = 6, categoryId = 7, subjectId = 3, status = "active", masterStatus = "inactive"),
                ),
        )

    @Test
    fun `without a category every subject is a choice`() {
        assertEquals(subjects, LeaderboardRules.subjectChoices(null, subjects, hierarchy))
    }

    @Test
    fun `a category offers its open branches in group then branch order, named with the group`() {
        val choices = LeaderboardRules.subjectChoices(7, subjects, hierarchy)

        // No group sorts as 0, before TPS (1); inactive branches, subjects and groups are left out.
        assertEquals(listOf("Matematika", "TPS — Penalaran Umum"), choices.map(NamedOption::name))
    }

    @Test
    fun `a subject the new category does not have goes back to all`() {
        val choices = LeaderboardRules.subjectChoices(8, subjects, hierarchy)

        assertEquals(3L, LeaderboardRules.keepSubject(3, choices))
        assertNull(LeaderboardRules.keepSubject(1, choices))
        assertNull(LeaderboardRules.keepSubject(null, choices))
    }

    @Test
    fun `the clock is always hours, minutes and seconds`() {
        assertEquals("01:35:12", LeaderboardRules.clock(5_712))
        assertEquals("00:00:00", LeaderboardRules.clock(null))
        assertEquals("00:00:00", LeaderboardRules.clock(-4))
    }

    @Test
    fun `the update time is shown in the phone's zone`() {
        assertEquals(
            "18 Nov 2026, 14.05",
            LeaderboardRules.updatedLabel("2026-11-18T07:05:00+00:00", ZoneId.of("Asia/Jakarta")),
        )
        assertNull(LeaderboardRules.updatedLabel(null))
    }
}
