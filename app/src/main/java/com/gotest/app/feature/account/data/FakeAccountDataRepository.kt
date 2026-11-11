package com.gotest.app.feature.account.data

import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.data.fake.withFakeLatency
import com.gotest.app.feature.account.domain.AccountDataCategory
import com.gotest.app.feature.account.domain.AccountDataRepository
import com.gotest.app.feature.account.domain.PersonalDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val BIRTH_DATE = LocalDate.of(2008, 5, 14)

/** Personal details from the "Akun / Pengaturan – Data Pribadi" frame; copy requests always succeed. */
@Singleton
class FakeAccountDataRepository
    @Inject
    constructor() : AccountDataRepository {
        private val details =
            PersonalDetails(
                phoneNumber = "0812-3456-7890",
                birthDate = BIRTH_DATE,
                school = "SMA Negeri 3 Bandung",
                city = "Bandung, Jawa Barat",
            )

        override fun observePersonalDetails(): Flow<PersonalDetails> = flowOf(details).withFakeLatency()

        override suspend fun requestDataCopy(categories: Set<AccountDataCategory>): Result<Unit> {
            simulateLatency()
            return if (categories.isEmpty()) {
                Result.failure(IllegalArgumentException("nothing selected"))
            } else {
                Result.success(Unit)
            }
        }
    }
