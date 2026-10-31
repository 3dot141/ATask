package com.yes365.atask.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.yes365.atask.util.IosPaths
import com.yes365.atask.util.ioDispatcher

actual class PlatformContext

actual fun getDatabaseBuilder(ctx: PlatformContext): RoomDatabase.Builder<VikunjaDatabase> {
    val dir = IosPaths.subdirectory(IosPaths.applicationSupport, "database")
    // The folder is excluded from backup (like Android's backup rules), so the journal files are too.
    IosPaths.excludeFromBackup(dir)
    return Room.databaseBuilder<VikunjaDatabase>(
        name = "$dir/vicu_database.db",
        factory = { VikunjaDatabaseConstructor.initialize() },
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(ioDispatcher)
}
