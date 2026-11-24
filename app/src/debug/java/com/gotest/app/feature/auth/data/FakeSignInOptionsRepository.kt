package com.gotest.app.feature.auth.data

import com.gotest.app.feature.auth.domain.SignInOptions
import com.gotest.app.feature.auth.domain.SignInOptionsRepository
import javax.inject.Inject

/** Debug without the API: every way of signing in is on, as the screens were built and checked. */
class FakeSignInOptionsRepository
    @Inject
    constructor() : SignInOptionsRepository {
        override suspend fun options(): SignInOptions = SignInOptions(phone = true, google = true, emailCode = true)
    }
