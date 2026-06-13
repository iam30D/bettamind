package org.bettamind.shared.security

interface SecurityGateway {
    suspend fun isSecureStorageAvailable(): Boolean
}
