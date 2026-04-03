package com.waveform.data.remote.mapper

import com.waveform.data.remote.dto.AudioFileDto
import com.waveform.domain.model.AudioFileInfo

/**
 * Maps between the data-layer [AudioFileDto] and the domain-layer [AudioFileInfo].
 */
object AudioFileMapper {

    fun AudioFileDto.toDomain(): AudioFileInfo = AudioFileInfo(
        id = id,
        name = name,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        bucketId = bucketId,
        storagePath = storagePath,
        userId = userId,
        uploadedAt = uploadedAt,
    )

    fun List<AudioFileDto>.toDomain(): List<AudioFileInfo> = map { it.toDomain() }
}
