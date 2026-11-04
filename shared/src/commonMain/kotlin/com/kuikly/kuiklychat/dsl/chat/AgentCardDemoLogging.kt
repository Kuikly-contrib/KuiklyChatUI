package com.kuikly.kuiklychat.dsl.chat

/** Demo-only structured logging hook; platform implementations keep commonMain portable. */
internal expect fun logAgentCardDemo(event: String, payload: String)

