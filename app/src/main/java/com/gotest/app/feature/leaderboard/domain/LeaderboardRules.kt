package com.gotest.app.feature.leaderboard.domain

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/** Web rules of the Leaderboard pages that the screens need without Compose. */
object LeaderboardRules {
    private const val ACTIVE = "active"
    private const val INACTIVE = "inactive"
    private const val SECONDS_PER_HOUR = 3600
    private const val SECONDS_PER_MINUTE = 60
    private val updatedFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH.mm", Locale.forLanguageTag("id-ID"))

    /**
     * The subject choices of the Global filter (web `GlobalBoard` with `lib/taxonomy.ts`): every subject
     * without a category; with one, the subjects of its active branches in group then branch order,
     * named "{group} — {subject}" when the branch has a group.
     */
    fun subjectChoices(
        category: Long?,
        subjects: List<NamedOption>,
        hierarchy: TaxonomyHierarchy,
    ): List<NamedOption> {
        if (category == null) return subjects
        val groups = hierarchy.groups.associateBy(TaxonomyGroup::id)
        return hierarchy.branches
            .filter { branch -> branch.categoryId == category && branch.isOpen(groups) }
            .sortedWith(compareBy({ groups[it.groupId]?.orderNumber ?: 0 }, { it.orderNumber }))
            .mapNotNull { branch ->
                val subject = subjects.firstOrNull { it.id == branch.subjectId } ?: return@mapNotNull null
                val group = groups[branch.groupId]
                subject.copy(name = if (group != null) "${group.name} — ${subject.name}" else subject.name)
            }
    }

    private fun TaxonomyBranch.isOpen(groups: Map<Long, TaxonomyGroup>): Boolean =
        status == ACTIVE &&
            masterStatus != INACTIVE &&
            (categoryStatus == null || categoryStatus == ACTIVE) &&
            (groupId == null || groups[groupId]?.status == ACTIVE)

    /** Web: a chosen subject that the new category does not have goes back to "all". */
    fun keepSubject(
        subject: Long?,
        choices: List<NamedOption>,
    ): Long? = subject?.takeIf { id -> choices.any { it.id == id } }

    /** Web `formatClock`: "01:35:12". */
    fun clock(seconds: Int?): String {
        val value = (seconds ?: 0).coerceAtLeast(0)
        return "%02d:%02d:%02d".format(
            Locale.ROOT,
            value / SECONDS_PER_HOUR,
            value % SECONDS_PER_HOUR / SECONDS_PER_MINUTE,
            value % SECONDS_PER_MINUTE,
        )
    }

    fun instant(value: String?): Instant? =
        value?.let {
            try {
                OffsetDateTime.parse(it).toInstant()
            } catch (_: DateTimeParseException) {
                null
            }
        }

    /** "Terakhir diperbarui 18 Nov 2026, 14.05", in the phone's time zone like the web. */
    fun updatedLabel(
        value: String?,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String? = instant(value)?.let { updatedFormat.format(it.atZone(zone)) }
}
