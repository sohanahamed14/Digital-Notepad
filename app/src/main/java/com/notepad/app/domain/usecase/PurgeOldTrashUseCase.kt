package com.notepad.app.domain.usecase

import com.notepad.app.domain.repository.NoteRepository
import javax.inject.Inject

class PurgeOldTrashUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(daysThreshold: Int = 30) {
        repository.purgeOldTrash(daysThreshold)
    }
}
