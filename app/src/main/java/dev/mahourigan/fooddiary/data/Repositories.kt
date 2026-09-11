package dev.mahourigan.fooddiary.data

import android.content.Context
import java.io.File

/**
 * The single store.
 *
 * One instance, for the same reason the tasks app has one: once reminders can
 * write — closing a stale symptom episode straight from the notification shade —
 * a second repository over the same file would hold its own copy of everything
 * in memory and quietly overwrite whatever the app had just saved.
 */
object Repositories {

    @Volatile
    private var instance: LocalDiaryRepository? = null

    fun diary(context: Context): LocalDiaryRepository =
        instance ?: synchronized(this) {
            instance ?: LocalDiaryRepository(
                file = File(context.applicationContext.filesDir, "diary.json"),
            ).also { instance = it }
        }
}
