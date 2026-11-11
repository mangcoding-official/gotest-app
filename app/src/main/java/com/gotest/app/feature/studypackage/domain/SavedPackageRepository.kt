package com.gotest.app.feature.studypackage.domain

import com.gotest.app.core.model.StudyPackage
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** A package on "Paket Tersimpan" and the day the student saved it. */
data class SavedPackage(
    val studyPackage: StudyPackage,
    val savedOn: LocalDate,
)

/** Packages the student bookmarked from Detail Paket / Detail Tryout. */
interface SavedPackageRepository {
    /** Most recently saved first. */
    fun observeSavedPackages(): Flow<List<SavedPackage>>

    /** Drives the bookmark icon on the detail screens. */
    fun observeIsSaved(packageId: String): Flow<Boolean>

    /** Saves [packageId], or removes it when it is already saved. Returns whether it is saved now. */
    suspend fun toggle(packageId: String): Boolean

    suspend fun remove(packageId: String)
}
