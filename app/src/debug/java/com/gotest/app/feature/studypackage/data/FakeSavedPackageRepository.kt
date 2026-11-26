package com.gotest.app.feature.studypackage.data

import com.gotest.app.core.data.fake.DummyCatalog
import com.gotest.app.core.data.fake.PackageIds
import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.data.fake.withFakeLatency
import com.gotest.app.feature.studypackage.domain.SavedPackage
import com.gotest.app.feature.studypackage.domain.SavedPackageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private data class SavedEntry(
    val packageId: String,
    val savedOn: LocalDate,
)

// The three cards of the "Paket / Tersimpan" frame, newest first.
private val seed =
    listOf(
        SavedEntry(PackageIds.TRYOUT_UTBK_1, LocalDate.of(2026, 10, 28)),
        SavedEntry(PackageIds.LATIHAN_SEPT_1, LocalDate.of(2026, 10, 26)),
        SavedEntry(PackageIds.TRYOUT_CPNS_3, LocalDate.of(2026, 10, 20)),
    )

/** In-memory bookmarks shared by the detail screens and Paket Tersimpan, reset on app start. */
@Singleton
class FakeSavedPackageRepository
    @Inject
    constructor() : SavedPackageRepository {
        private val entries = MutableStateFlow(seed)

        override fun observeSavedPackages(): Flow<List<SavedPackage>> =
            entries
                .map { list -> list.map { SavedPackage(DummyCatalog.packageById(it.packageId), it.savedOn) } }
                .withFakeLatency()

        override fun observeIsSaved(packageId: String): Flow<Boolean> =
            entries
                .map { list -> list.any { it.packageId == packageId } }
                .distinctUntilChanged()
                .withFakeLatency()

        override suspend fun toggle(packageId: String): Boolean {
            // Optimistic: the icon flips at once, then the "server" catches up.
            val updated =
                entries.updateAndGet { list ->
                    if (list.any { it.packageId == packageId }) {
                        list.filterNot { it.packageId == packageId }
                    } else {
                        listOf(SavedEntry(packageId, LocalDate.now())) + list
                    }
                }
            simulateLatency()
            return updated.any { it.packageId == packageId }
        }

        override suspend fun remove(packageId: String) {
            entries.update { list -> list.filterNot { it.packageId == packageId } }
            simulateLatency()
        }
    }
