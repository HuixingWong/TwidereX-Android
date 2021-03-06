/*
 *  Twidere X
 *
 *  Copyright (C) 2020 Tlaster <tlaster@outlook.com>
 * 
 *  This file is part of Twidere X.
 * 
 *  Twidere X is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  Twidere X is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with Twidere X. If not, see <http://www.gnu.org/licenses/>.
 */
package com.twidere.twiderex.di

import android.accounts.AccountManager
import android.content.Context
import android.content.SharedPreferences
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build.VERSION.SDK_INT
import androidx.room.Room
import androidx.work.WorkManager
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.fetch.VideoFrameFileFetcher
import coil.fetch.VideoFrameUriFetcher
import com.twidere.twiderex.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AndroidModule {
    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    fun provideAccountManager(@ApplicationContext context: Context): AccountManager =
        AccountManager.get(context)

    @Provides
    fun provideSharedPreference(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("twiderex", Context.MODE_PRIVATE)

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "twiderex-db")
            .build()

    @Provides
    fun provideLocationManager(@ApplicationContext context: Context): LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Provides
    fun provideGeoCoder(@ApplicationContext context: Context): Geocoder = Geocoder(context)

    @Provides
    fun provideImageLoader(@ApplicationContext context: Context) : ImageLoader = ImageLoader.Builder(context)
        .componentRegistry {
            if (SDK_INT >= 28) {
                add(ImageDecoderDecoder())
            } else {
                add(GifDecoder())
            }
            add(VideoFrameFileFetcher(context))
            add(VideoFrameUriFetcher(context))
        }
        .build()
}
