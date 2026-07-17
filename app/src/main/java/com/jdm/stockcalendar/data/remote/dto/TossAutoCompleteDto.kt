package com.jdm.stockcalendar.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TossAutoCompleteRequestDto(
    val query: String,
    val sections: List<TossSectionRequestDto> = listOf(TossSectionRequestDto(type = "PRODUCT")),
)

@Serializable
data class TossSectionRequestDto(val type: String)

@Serializable
data class TossAutoCompleteResponseDto(val result: List<TossResultSectionDto>? = null)

@Serializable
data class TossResultSectionDto(val type: String? = null, val data: TossSectionDataDto? = null)

@Serializable
data class TossSectionDataDto(val items: List<TossProductItemDto>? = null)

@Serializable
data class TossProductItemDto(val productCode: String, val symbol: String? = null)
