package com.gotest.app.feature.account.data

import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.data.fake.withFakeLatency
import com.gotest.app.feature.account.domain.AccountSettings
import com.gotest.app.feature.account.domain.SettingSwitch
import com.gotest.app.feature.account.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private const val REMINDER_HOUR = 19

/** In-memory settings matching the "Akun / Pengaturan" frame. */
@Singleton
class FakeSettingsRepository
    @Inject
    constructor() : SettingsRepository {
        private val settings =
            MutableStateFlow(
                AccountSettings(
                    switches =
                        mapOf(
                            SettingSwitch.ActivityResults to true,
                            SettingSwitch.RankingUpdates to true,
                            SettingSwitch.NewPackages to false,
                            SettingSwitch.SubscriptionStatus to true,
                            SettingSwitch.StudyReminder to true,
                            SettingSwitch.PromoEmails to false,
                        ),
                    reminderTime = LocalTime.of(REMINDER_HOUR, 0),
                    mainLevel = "UTBK-SNBT",
                ),
            )

        override fun observeSettings(): Flow<AccountSettings> = settings.withFakeLatency()

        override suspend fun setSwitch(
            switch: SettingSwitch,
            enabled: Boolean,
        ): Result<Unit> {
            // Optimistic: the switch flips at once, then the "server" catches up.
            settings.update { it.copy(switches = it.switches + (switch to enabled)) }
            simulateLatency()
            return Result.success(Unit)
        }
    }
