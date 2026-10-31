package com.yes365.atask.util

expect class AtomicLong(initialValue: Long) {
    fun get(): Long
    fun set(newValue: Long)
    fun getAndIncrement(): Long
    fun decrementAndGet(): Long
}
