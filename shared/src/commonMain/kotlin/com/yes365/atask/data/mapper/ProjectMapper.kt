package com.yes365.atask.data.mapper

import com.yes365.atask.data.local.entity.ProjectEntity
import com.yes365.atask.data.remote.api.CreateProjectDto
import com.yes365.atask.data.remote.api.ProjectDto
import com.yes365.atask.data.remote.api.UpdateProjectDto
import com.yes365.atask.domain.model.Project

class ProjectMapper {

    fun ProjectDto.toEntity(): ProjectEntity = ProjectEntity(
        id = id,
        title = title,
        description = description,
        hexColor = hexColor,
        parentProjectId = parentProjectId,
        position = position,
        isArchived = isArchived,
        isFavorite = isFavorite,
        identifier = identifier,
        created = created,
        updated = updated,
        ownerId = owner?.id ?: 0,
        maxPermission = maxPermission ?: -1,
    )

    fun ProjectEntity.toDomain(): Project = Project(
        id = id,
        title = title,
        description = description,
        hexColor = hexColor,
        parentProjectId = parentProjectId,
        position = position,
        isArchived = isArchived,
        isFavorite = isFavorite,
        identifier = identifier,
        created = created,
        updated = updated,
        ownerId = ownerId,
        maxPermission = maxPermission,
    )

    fun Project.toEntity(): ProjectEntity = ProjectEntity(
        id = id,
        title = title,
        description = description,
        hexColor = hexColor,
        parentProjectId = parentProjectId,
        position = position,
        isArchived = isArchived,
        isFavorite = isFavorite,
        identifier = identifier,
        created = created,
        updated = updated,
        ownerId = ownerId,
        maxPermission = maxPermission,
    )

    fun Project.toCreateDto(): CreateProjectDto = CreateProjectDto(
        title = title,
        description = description,
        hexColor = hexColor.removePrefix("#"),
        parentProjectId = parentProjectId,
    )

    fun Project.toUpdateDto(): UpdateProjectDto = UpdateProjectDto(
        title = title,
        description = description,
        hexColor = hexColor.removePrefix("#"),
        isArchived = isArchived,
        position = position,
        parentProjectId = parentProjectId,
    )
}
