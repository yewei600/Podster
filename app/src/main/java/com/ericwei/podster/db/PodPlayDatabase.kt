/*
 * Copyright (c) 2020 Eric Wei. All rights reserved.
 */

package com.ericwei.podster.db

import android.content.Context
import androidx.room.*
import com.ericwei.podster.model.Episode
import com.ericwei.podster.model.Podcast
import java.util.*

class Converters {
    //Room only knows how to deal with basic types and boxed types(a type that's nullable)
    //therefore need type converters
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return date?.time
    }
}

@Database(entities = [Podcast::class, Episode::class], version = 1)
@TypeConverters(Converters::class)
abstract class PodPlayDatabase : RoomDatabase() {

    abstract fun podcastDao(): PodcastDao

    companion object {
        private var instance: PodPlayDatabase? = null
        fun getInstance(context: Context): PodPlayDatabase {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    PodPlayDatabase::class.java, "PodPlayer"
                ).build()
            }
            return instance as PodPlayDatabase
        }
    }
}