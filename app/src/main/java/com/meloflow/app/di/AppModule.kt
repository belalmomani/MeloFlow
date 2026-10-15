package com.meloflow.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.meloflow.app.data.DefaultClock
import com.meloflow.app.data.local.AppDatabase
import com.meloflow.app.data.local.InteractionDao
import com.meloflow.app.data.repository.InteractionRepositoryImpl
import com.meloflow.app.data.repository.MockMediaSourceRepository
import com.meloflow.app.data.repository.PreferencesRepositoryImpl
import com.meloflow.app.data.repository.RecommendationRepositoryImpl
import com.meloflow.app.domain.Clock
import com.meloflow.app.domain.recommendation.RecommendationEngine
import com.meloflow.app.domain.recommendation.RuleBasedRecommendationEngine
import com.meloflow.app.domain.repository.InteractionRepository
import com.meloflow.app.domain.repository.MediaSourceRepository
import com.meloflow.app.domain.repository.PreferencesRepository
import com.meloflow.app.domain.repository.RecommendationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    /** Swap this binding to a real provider to leave demo mode. */
    @Binds @Singleton
    abstract fun bindMediaSourceRepository(impl: MockMediaSourceRepository): MediaSourceRepository

    @Binds @Singleton
    abstract fun bindInteractionRepository(impl: InteractionRepositoryImpl): InteractionRepository

    @Binds @Singleton
    abstract fun bindRecommendationRepository(impl: RecommendationRepositoryImpl): RecommendationRepository

    /** Swap this binding to replace the rule-based engine (AI service, ML model, ...). */
    @Binds
    abstract fun bindRecommendationEngine(impl: RuleBasedRecommendationEngine): RecommendationEngine

    @Binds
    abstract fun bindClock(impl: DefaultClock): Clock

    companion object {
        @Provides @Singleton
        fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("user_preferences") }

        @Provides @Singleton
        fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "meloflow.db").build()

        @Provides
        fun provideInteractionDao(db: AppDatabase): InteractionDao = db.interactionDao()
    }
    // TODO(Phase 9): DownloadRepository bindings.
}
