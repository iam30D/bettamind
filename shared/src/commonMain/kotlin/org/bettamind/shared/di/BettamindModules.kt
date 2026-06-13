package org.bettamind.shared.di

import org.bettamind.shared.foundation.AppEnvironment
import org.bettamind.shared.foundation.getPlatform
import org.koin.dsl.module

val sharedFoundationModule = module {
    single { AppEnvironment(platform = getPlatform()) }
}
