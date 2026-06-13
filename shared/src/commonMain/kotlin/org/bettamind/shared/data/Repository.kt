package org.bettamind.shared.data

interface Repository<T> {
    suspend fun get(): T?
    suspend fun save(value: T)
}
