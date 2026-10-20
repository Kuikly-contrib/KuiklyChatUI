package com.tencent.kuiklybase.chat.android

import java.net.URLConnection
import java.net.URLEncoder
import org.json.JSONObject

/**
 * Stream body and query encoding copied from KuiklyUI `KRNetworkModule`.
 * JSON Content-Type sends JSON; otherwise the body is UTF-8 form encoding.
 */
internal fun prepareStreamBody(
    connection: URLConnection,
    method: String,
    header: JSONObject?,
    param: JSONObject?,
): ByteArray? {
    if (method != "POST" || param == null) return null
    val contentType = findHeaderIgnoreCase(header, "Content-Type")
    val body = if (contentType?.contains("json", ignoreCase = true) == true) {
        param.toString()
    } else {
        if (contentType == null) {
            connection.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded; charset=UTF-8",
            )
        }
        buildFormUrlEncodedBody(param)
    }
    connection.doOutput = true
    return body.toByteArray(Charsets.UTF_8)
}

internal fun addStreamHeaders(connection: URLConnection, header: JSONObject?, cookie: String?) {
    if (!cookie.isNullOrEmpty()) {
        connection.addRequestProperty("Cookie", cookie)
    }
    header ?: return
    val keys = header.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        connection.addRequestProperty(key, header.opt(key)?.toString() ?: "")
    }
}

internal fun createStreamRequestUrl(url: String, param: JSONObject?): String {
    if (param == null || param.length() == 0) return url
    val query = StringBuilder()
    val keys = param.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        if (query.isNotEmpty()) query.append('&')
        query.append(encodeQueryComponent(key))
            .append('=')
            .append(encodeQueryComponent(streamParamValue(param.opt(key))))
    }
    val fragmentIndex = url.indexOf('#')
    val base = if (fragmentIndex >= 0) url.substring(0, fragmentIndex) else url
    val fragment = if (fragmentIndex >= 0) url.substring(fragmentIndex) else ""
    val separator = when {
        !base.contains('?') -> "?"
        base.endsWith('?') || base.endsWith('&') -> ""
        else -> "&"
    }
    return base + separator + query + fragment
}

internal fun headerFieldsToJson(fields: Map<String, List<String>>?): String {
    if (fields.isNullOrEmpty()) return ""
    val json = JSONObject()
    for ((key, values) in fields) {
        json.put(key, if (values.size == 1) values[0] else values.joinToString(", "))
    }
    return json.toString()
}

internal fun parseJsonObject(raw: String?): JSONObject {
    if (raw.isNullOrBlank()) return JSONObject()
    return try {
        JSONObject(raw)
    } catch (_: Throwable) {
        JSONObject()
    }
}

private fun buildFormUrlEncodedBody(param: JSONObject): String {
    val body = StringBuilder()
    val keys = param.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        if (body.isNotEmpty()) body.append('&')
        body.append(URLEncoder.encode(key, "UTF-8"))
            .append('=')
            .append(URLEncoder.encode(streamParamValue(param.opt(key)), "UTF-8"))
    }
    return body.toString()
}

private fun streamParamValue(value: Any?): String {
    return if (value == null || value == JSONObject.NULL) "" else value.toString()
}

private fun findHeaderIgnoreCase(header: JSONObject?, name: String): String? {
    header ?: return null
    val keys = header.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        if (key.equals(name, ignoreCase = true)) {
            return header.opt(key)?.toString()
        }
    }
    return null
}

private fun encodeQueryComponent(value: String): String {
    return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
