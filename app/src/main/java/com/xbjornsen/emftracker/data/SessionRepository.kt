package com.xbjornsen.emftracker.data

import android.content.Context
import com.xbjornsen.emftracker.data.db.SessionDatabase
import com.xbjornsen.emftracker.data.db.SessionEntity
import com.xbjornsen.emftracker.data.models.Session

class SessionRepository(context: Context) {
    private val dao = SessionDatabase.getInstance(context).sessionDao()

    suspend fun getSessions(): List<Session> = dao.getAll().map { it.toModel() }

    suspend fun saveSession(session: Session) = dao.insert(session.toEntity())

    suspend fun deleteSession(id: String) = dao.delete(id)

    private fun SessionEntity.toModel() = Session(id, startTime, endTime, peak, average, readingCount)
    private fun Session.toEntity() = SessionEntity(id, startTime, endTime, peak, average, readingCount)
}
