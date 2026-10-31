package com.yes365.atask.data.mapper

import com.yes365.atask.data.local.entity.LabelEntity
import com.yes365.atask.data.remote.api.CreateLabelDto
import com.yes365.atask.data.remote.api.LabelDto
import com.yes365.atask.domain.model.Label

class LabelMapper {

    fun LabelDto.toEntity(): LabelEntity = LabelEntity(
        id = id,
        title = title,
        description = description,
        hexColor = hexColor,
        createdById = createdBy?.id ?: 0,
        created = created,
        updated = updated,
    )

    fun LabelEntity.toDomain(): Label = Label(
        id = id,
        title = title,
        description = description,
        hexColor = hexColor,
        createdById = createdById,
        created = created,
        updated = updated,
    )

    fun LabelDto.toDomain(): Label = Label(
        id = id,
        title = title,
        description = description,
        hexColor = hexColor,
        createdById = createdBy?.id ?: 0,
        created = created,
        updated = updated,
    )

    fun Label.toDto(): LabelDto = LabelDto(
        id = id,
        title = title,
        description = description,
        hexColor = hexColor.removePrefix("#"),
    )

    fun Label.toCreateDto(): CreateLabelDto = CreateLabelDto(
        title = title,
        description = description,
        hexColor = hexColor.removePrefix("#"),
    )

    fun Label.toEntity(): LabelEntity = LabelEntity(
        id = id,
        title = title,
        description = description,
        hexColor = hexColor,
        createdById = createdById,
        created = created,
        updated = updated,
    )
}
