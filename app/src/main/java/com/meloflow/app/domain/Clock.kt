package com.meloflow.app.domain

/** Injectable time source so time-dependent logic (decay, timestamps) is testable. */
interface Clock {
    fun nowMs(): Long
}
