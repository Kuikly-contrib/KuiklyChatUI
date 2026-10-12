package com.tencent.kuiklybase.chat.compose.ui

import com.tencent.kuiklybase.chat.compose.core.MessagePart
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatComposerAttachmentsTest {
    private val sample = MessagePart.Attachment(
        id = "a1",
        uri = "https://example.com/a.jpg",
        mimeType = "image/jpeg",
        name = "a.jpg",
    )

    @Test
    fun validateRejectsBlankIdOrUri() {
        assertFalse(
            validateComposerAttachments(
                listOf(sample.copy(id = "")),
            ),
        )
        assertFalse(
            validateComposerAttachments(
                listOf(sample.copy(uri = "")),
            ),
        )
    }

    @Test
    fun validateRejectsDuplicateIdsInBatch() {
        assertFalse(validateComposerAttachments(listOf(sample, sample)))
    }

    @Test
    fun mergeAppendsDistinctAttachments() {
        val other = sample.copy(id = "a2", name = "b.jpg")
        val merged = mergePickedAttachments(listOf(sample), listOf(other))
        assertEquals(listOf(sample, other), merged)
    }

    @Test
    fun mergeSkipsExistingIds() {
        val merged = mergePickedAttachments(listOf(sample), listOf(sample.copy(name = "dup.jpg")))
        assertEquals(listOf(sample), merged)
    }

    @Test
    fun mergeRejectsInvalidBatch() {
        assertFailsWith<IllegalArgumentException> {
            mergePickedAttachments(emptyList(), listOf(sample.copy(id = "")))
        }
    }
}
