package com.yes365.atask.data.mapper

import com.yes365.atask.data.local.entity.AttachmentEntity
import com.yes365.atask.data.remote.api.AttachmentDto
import com.yes365.atask.domain.model.Attachment

class AttachmentMapper {

    fun AttachmentDto.toEntity(taskId: Long): AttachmentEntity = AttachmentEntity(
        id = id,
        taskId = taskId,
        fileName = file?.name ?: "",
        mimeType = file?.mime ?: "",
        fileSize = file?.size ?: 0,
        createdAt = created,
    )

    fun AttachmentEntity.toDomain(): Attachment = Attachment(
        id = id,
        taskId = taskId,
        fileName = fileName,
        mimeType = mimeType,
        fileSize = fileSize,
        createdAt = createdAt,
    )
}
