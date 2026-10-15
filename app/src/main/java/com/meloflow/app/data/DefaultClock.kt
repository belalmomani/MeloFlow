package com.meloflow.app.data

import com.meloflow.app.domain.Clock
import javax.inject.Inject

class DefaultClock @Inject constructor() : Clock {
    override fun nowMs(): Long = System.currentTimeMillis()
}
