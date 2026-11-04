package com.kuikly.kuiklychat.dsl.chat

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AgentCardDemoJsonRepairTest {
    @Test
    fun closesArrayLeftOpenBeforeNextKey() {
        // Captured from a real reply: dish3_steps never closed its `]`.
        val broken = """{"v":{"a":[{"n":"1"},{"n":"2"},"b":[{"n":"3"}]}}"""
        val repaired = repairJsonBrackets(broken)!!
        val value = JSONObject(repaired).optJSONObject("v")!!
        assertEquals(2, value.optJSONArray("a")!!.length())
        assertEquals(1, value.optJSONArray("b")!!.length())
    }

    @Test
    fun dropsStrayQuoteBeforeArrayClose() {
        // Captured from a real reply: every steps array ended with `},"],`.
        val broken = """{"v":{"a":[{"n":"1"},{"n":"2"},"],"b":[{"n":"3"},"],"c":"x"}}"""
        val value = JSONObject(repairJsonBrackets(broken)!!).optJSONObject("v")!!
        assertEquals(2, value.optJSONArray("a")!!.length())
        assertEquals(1, value.optJSONArray("b")!!.length())
        assertEquals("x", value.optString("c"))
    }

    @Test
    fun appendsMissingClosers() {
        val repaired = repairJsonBrackets("""{"a":{"b":[1,2""")!!
        assertEquals(2, JSONObject(repaired).optJSONObject("a")!!.optJSONArray("b")!!.length())
    }

    @Test
    fun leavesStringsAlone() {
        val line = """{"t":"a:[b],{c}"}"""
        assertEquals(line, repairJsonBrackets(line))
    }

    @Test
    fun rejectsUnrepairableInput() {
        assertNull(repairJsonBrackets("""{"a":1}}"""))
    }
}
