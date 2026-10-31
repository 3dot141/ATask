package com.yes365.atask.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.ConstructedBy
import androidx.room.RoomDatabaseConstructor
import com.yes365.atask.data.local.dao.AttachmentDao
import com.yes365.atask.data.local.dao.CollaborationDao
import com.yes365.atask.data.local.dao.LabelDao
import com.yes365.atask.data.local.dao.LocalDataDao
import com.yes365.atask.data.local.dao.PendingActionDao
import com.yes365.atask.data.local.dao.ProjectDao
import com.yes365.atask.data.local.dao.RoutineArchiveDao
import com.yes365.atask.data.local.dao.TaskDao
import com.yes365.atask.data.local.entity.AttachmentEntity
import com.yes365.atask.data.local.entity.LabelEntity
import com.yes365.atask.data.local.entity.PendingActionEntity
import com.yes365.atask.data.local.entity.ProjectMemberEntity
import com.yes365.atask.data.local.entity.ProjectEntity
import com.yes365.atask.data.local.entity.RoutineOccurrenceArchiveEntity
import com.yes365.atask.data.local.entity.TaskCommentEntity
import com.yes365.atask.data.local.entity.TaskEntity
import com.yes365.atask.data.local.entity.TaskSignalEntity

@Database(
    entities = [
        TaskEntity::class,
        ProjectEntity::class,
        LabelEntity::class,
        PendingActionEntity::class,
        AttachmentEntity::class,
        RoutineOccurrenceArchiveEntity::class,
        ProjectMemberEntity::class,
        TaskSignalEntity::class,
        TaskCommentEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@ConstructedBy(VikunjaDatabaseConstructor::class)
abstract class VikunjaDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao
    abstract fun labelDao(): LabelDao
    abstract fun pendingActionDao(): PendingActionDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun routineArchiveDao(): RoutineArchiveDao
    abstract fun localDataDao(): LocalDataDao
    abstract fun collaborationDao(): CollaborationDao
}

// Room's KSP processor generates the actual for each target. The override is declared here so the
// common metadata compilation (which has no generated actual) sees a complete object.
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object VikunjaDatabaseConstructor : RoomDatabaseConstructor<VikunjaDatabase> {
    override fun initialize(): VikunjaDatabase
}
