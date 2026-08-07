package com.tencent.kuiklybase.chat.compose.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextPartMarkdownTest {
    @Test
    fun plainAssistantTextDoesNotRequireBlockMarkdown() {
        assertFalse(hasBlockMarkdownSyntax("Searching done"))
        assertFalse(hasBlockMarkdownSyntax("hello"))
    }

    @Test
    fun blockMarkdownSyntaxIsDetected() {
        assertTrue(hasBlockMarkdownSyntax("# Title"))
        assertTrue(hasBlockMarkdownSyntax("```kotlin\ncode\n```"))
        assertTrue(hasBlockMarkdownSyntax("- item"))
        assertTrue(hasBlockMarkdownSyntax("1. item"))
    }
}
