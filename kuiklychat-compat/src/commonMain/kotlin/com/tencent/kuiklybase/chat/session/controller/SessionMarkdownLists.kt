package com.tencent.kuiklybase.chat.session.controller

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.components.MarkdownComponents
import com.tencent.kuiklybase.components.markdownComponents
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.config.FontStyle
import com.tencent.kuiklybase.config.FontWeight
import com.tencent.kuiklybase.config.MarkdownColors
import com.tencent.kuiklybase.config.MarkdownConfig
import com.tencent.kuiklybase.config.MarkdownDimens
import com.tencent.kuiklybase.config.MarkdownPadding
import com.tencent.kuiklybase.config.MarkdownTypography
import com.tencent.kuiklybase.config.TextStyleConfig
import com.tencent.kuiklybase.elements.listDepth
import com.tencent.kuiklybase.elements.markdownCheckBox
import com.tencent.kuiklybase.model.ReferenceLinkHandler
import com.tencent.kuiklybase.render.renderMarkdownElement
import com.tencent.kuiklybase.utils.getUnescapedTextInNode
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMTokenTypes

private const val LIST_DEPTH_KEY = "markdown_list_depth"

/**
 * Chat rows live in a List. The library list container uses flex(1), which
 * stretches the last message to the leftover viewport and leaves an empty band
 * above the composer. These renderers keep the row height to the text.
 */
internal fun sessionMarkdownConfig(theme: ChatTheme, outgoing: Boolean = false): MarkdownConfig {
    val textColor = if (outgoing) theme.outgoingTextColor else theme.incomingTextColor
    val mutedColor = if (outgoing) 0xCCFFFFFF else theme.systemTextColor
    val body = TextStyleConfig(fontSize = 14f, color = textColor, lineHeight = 20f)
    val code = TextStyleConfig(fontSize = 13f, color = textColor, fontFamily = "monospace", lineHeight = 18f)
    fun heading(size: Float, lineHeight: Float, weight: FontWeight) = TextStyleConfig(
        fontSize = size,
        fontWeight = weight,
        color = textColor,
        lineHeight = lineHeight,
    )
    return MarkdownConfig(
        colors = MarkdownColors(
            text = textColor,
            codeText = textColor,
            linkColor = if (outgoing) 0xFFE0E7FF else theme.accentColor,
            dividerColor = theme.borderColor,
            codeBackground = if (outgoing) 0x33FFFFFF else 0xFFF6F8FA,
            inlineCodeBackground = if (outgoing) 0x44FFFFFF else 0xFFF8FAFC,
            tableBackground = if (outgoing) 0x33FFFFFF else 0xFFFAFAFA,
            blockQuoteBar = if (outgoing) 0xCCFFFFFF else theme.accentColor,
            blockQuoteBackground = if (outgoing) 0x22FFFFFF else 0xFFF5F6F7,
        ),
        typography = MarkdownTypography(
            text = body,
            paragraph = body,
            ordered = body,
            bullet = body,
            list = body,
            quote = body.copy(color = mutedColor, fontStyle = FontStyle.Italic),
            textLink = body,
            h1 = heading(17f, 24f, FontWeight.Bold),
            h2 = heading(17f, 24f, FontWeight.Bold),
            h3 = heading(15f, 22f, FontWeight.Bold),
            h4 = heading(14f, 20f, FontWeight.SemiBold),
            h5 = heading(14f, 20f, FontWeight.SemiBold),
            h6 = heading(14f, 20f, FontWeight.SemiBold),
            code = code,
            inlineCode = code,
            table = code,
        ),
        dimens = MarkdownDimens(
            dividerThickness = 1f,
            codeBackgroundCornerSize = 6f,
            blockQuoteThickness = 3f,
            blockQuoteCornerSize = 6f,
            tableCellWidth = 136f,
            tableCellPadding = 10f,
            tableCornerSize = 6f,
        ),
        codeHighlightDarkTheme = false,
        codeHighlightEnabled = true,
        padding = MarkdownPadding(
            block = 4f,
            list = 4f,
            listItemTop = 2f,
            listItemBottom = 2f,
            listIndent = 16f,
            codeBlock = 12f,
            blockQuotePaddingLeft = 12f,
            blockQuoteBarPaddingLeft = 4f,
            blockQuoteTextVertical = 8f,
        ),
    )
}

internal fun sessionMarkdownComponents(): MarkdownComponents = SessionMarkdownComponents.components

private object SessionMarkdownComponents {
    val components: MarkdownComponents = markdownComponents(
        orderedList = { model, container ->
            container.sessionMarkdownList(
                content = model.content,
                node = model.node,
                style = model.typography.ordered,
                config = model.config,
                referenceLinkHandler = model.referenceLinkHandler,
                depth = model.listDepth,
                isOrdered = true,
            )
        },
        unorderedList = { model, container ->
            container.sessionMarkdownList(
                content = model.content,
                node = model.node,
                style = model.typography.bullet,
                config = model.config,
                referenceLinkHandler = model.referenceLinkHandler,
                depth = model.listDepth,
                isOrdered = false,
            )
        },
    )
}

private fun ViewContainer<*, *>.sessionMarkdownList(
    content: String,
    node: ASTNode,
    style: TextStyleConfig,
    config: MarkdownConfig,
    referenceLinkHandler: ReferenceLinkHandler?,
    depth: Int,
    isOrdered: Boolean,
) {
    View {
        attr {
            alignSelfStretch()
            marginLeft(config.padding.listIndent * depth)
            marginTop(config.padding.list)
            marginBottom(config.padding.list)
        }
        val initialListNumber = node.findChildOfType(MarkdownElementTypes.LIST_ITEM)
            ?.getUnescapedTextInNode(content)
            ?.takeWhile(Char::isDigit)
            ?.toIntOrNull()
            ?: 1
        var index = 0
        node.children.forEach { child ->
            if (child.type == MarkdownElementTypes.LIST_ITEM) {
                sessionMarkdownListItem(
                    content = content,
                    child = child,
                    index = index,
                    listNumber = initialListNumber,
                    depth = depth,
                    style = style,
                    config = config,
                    referenceLinkHandler = referenceLinkHandler,
                    isOrdered = isOrdered,
                )
                index++
            }
        }
    }
}

private fun ViewContainer<*, *>.sessionMarkdownListItem(
    content: String,
    child: ASTNode,
    index: Int,
    listNumber: Int,
    depth: Int,
    style: TextStyleConfig,
    config: MarkdownConfig,
    referenceLinkHandler: ReferenceLinkHandler?,
    isOrdered: Boolean,
) {
    val checkboxNode = child.children.getOrNull(1)?.takeIf { it.type == GFMTokenTypes.CHECK_BOX }
    View {
        attr {
            flexDirectionRow()
            alignSelfStretch()
            marginTop(config.padding.listItemTop)
            marginBottom(config.padding.listItemBottom)
        }
        if (checkboxNode != null) {
            val checked = checkboxNode.getTextInNode(content).contains("[x]")
            markdownCheckBox(checked, style, config)
        } else {
            val bulletText = if (isOrdered) {
                config.orderedListBullet(index, listNumber, depth)
            } else {
                config.unorderedListBullet(index, depth)
            }
            Text {
                attr {
                    text(bulletText)
                    fontSize(style.fontSize)
                    if (style.fontWeight == FontWeight.Bold) fontWeightBold()
                    if (style.fontStyle == FontStyle.Italic) fontStyleItalic()
                    color(style.color ?: config.colors.text)
                    style.lineHeight?.let { lineHeight(it) }
                }
            }
        }
        View {
            attr { alignSelfStretch() }
            child.children.forEach { nestedChild ->
                when (nestedChild.type) {
                    MarkdownElementTypes.ORDERED_LIST,
                    MarkdownElementTypes.UNORDERED_LIST,
                    -> sessionMarkdownList(
                        content = content,
                        node = nestedChild,
                        style = if (nestedChild.type == MarkdownElementTypes.ORDERED_LIST) {
                            config.typography.ordered
                        } else {
                            config.typography.bullet
                        },
                        config = config,
                        referenceLinkHandler = referenceLinkHandler,
                        depth = depth + 1,
                        isOrdered = nestedChild.type == MarkdownElementTypes.ORDERED_LIST,
                    )
                    MarkdownElementTypes.LIST_ITEM,
                    MarkdownTokenTypes.LIST_BULLET,
                    MarkdownTokenTypes.LIST_NUMBER,
                    GFMTokenTypes.CHECK_BOX,
                    -> Unit
                    else -> renderMarkdownElement(
                        node = nestedChild,
                        components = SessionMarkdownComponents.components,
                        content = content,
                        config = config,
                        referenceLinkHandler = referenceLinkHandler,
                        includeSpacer = false,
                        extra = mapOf(LIST_DEPTH_KEY to depth + 1),
                    )
                }
            }
        }
    }
}
