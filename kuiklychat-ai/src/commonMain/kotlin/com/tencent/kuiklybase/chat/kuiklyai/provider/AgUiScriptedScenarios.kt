package com.tencent.kuiklybase.chat.kuiklyai.provider

/** Prebuilt AG-UI SSE payload sequences for scripted transport demos and tests. */
public object AgUiScriptedScenarios {
    private fun sse(json: String): String = "data: $json\n\n"

    public val textOnly: List<String> = listOf(
        sse("""{"type":"RUN_STARTED","threadId":"scripted","runId":"run-text"}"""),
        sse("""{"type":"TEXT_MESSAGE_CONTENT","messageId":"msg-1","delta":"Hello"}"""),
        sse("""{"type":"TEXT_MESSAGE_CONTENT","messageId":"msg-1","delta":" Compose"}"""),
        sse("""{"type":"RUN_FINISHED","threadId":"scripted","runId":"run-text"}""")
    )

    public val textAndToolCall: List<String> = listOf(
        sse("""{"type":"RUN_STARTED","threadId":"scripted","runId":"run-tool"}"""),
        sse("""{"type":"TEXT_MESSAGE_CONTENT","messageId":"msg-1","delta":"Searching"}"""),
        sse("""{"type":"TOOL_CALL_START","toolCallId":"tool-1","toolCallName":"search"}"""),
        sse("""{"type":"TOOL_CALL_ARGS","toolCallId":"tool-1","delta":"{\"query\":\"Compose DSL\"}"}"""),
        sse("""{"type":"TOOL_CALL_END","toolCallId":"tool-1"}"""),
        sse(
            """{"type":"TOOL_CALL_RESULT","messageId":"tool-result-1","toolCallId":"tool-1","content":"找到 Compose Chat UI 设计资料"}"""
        ),
        sse("""{"type":"TEXT_MESSAGE_CONTENT","messageId":"msg-1","delta":" done"}"""),
        sse("""{"type":"RUN_FINISHED","threadId":"scripted","runId":"run-tool"}""")
    )

    public val runError: List<String> = listOf(
        sse("""{"type":"RUN_STARTED","threadId":"scripted","runId":"run-error"}"""),
        sse("""{"type":"TEXT_MESSAGE_CONTENT","messageId":"msg-1","delta":"partial"}"""),
        sse("""{"type":"RUN_ERROR","message":"Agent 运行失败","code":"SERVER"}""")
    )

    /** Long pause before the next chunk so callers can cancel mid-stream. */
    public val disconnect: List<String> = listOf(
        sse("""{"type":"RUN_STARTED","threadId":"scripted","runId":"run-disconnect"}"""),
        sse("""{"type":"TEXT_MESSAGE_CONTENT","messageId":"msg-1","delta":"wait"}""")
    )
}
