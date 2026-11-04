package com.tencent.kuiklybase.chat.composer

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.CaptureRule
import com.tencent.kuikly.core.base.attr.CaptureRuleDirection
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

private const val THUMB_SIZE = 64f

/** Pending image strip for [ChatDockComposer]. Horizontal pans stay inside the strip. */
internal fun ViewContainer<*, *>.ChatAttachmentStrip(
    previewUris: () -> ObservableList<String>,
    borderColor: Long,
    recessedColor: Long,
    onRemove: (String) -> Unit,
    onPreview: (String) -> Unit,
) {
    Scroller {
        attr {
            height(THUMB_SIZE + 6f)
            flexDirectionRow()
            alignItemsCenter()
            showScrollerIndicator(false)
            bouncesEnable(true)
            scrollEnable(true)
            capture(CaptureRule.pan(CaptureRuleDirection.HORIZONTAL))
            padding(left = 4f, right = 4f)
        }
        vfor(previewUris) { imageUri ->
            View {
                attr {
                    size(THUMB_SIZE, THUMB_SIZE)
                    borderRadius(14f)
                    backgroundColor(Color(recessedColor))
                    border(Border(1f, BorderStyle.SOLID, Color(borderColor)))
                    marginRight(9f)
                    overflow(true)
                }
                Image {
                    attr {
                        absolutePositionAllZero()
                        resizeCover()
                        src(imageUri, false)
                    }
                    event { click { onPreview(imageUri) } }
                }
                View {
                    attr {
                        absolutePosition(top = 0f, right = 0f)
                        size(32f, 32f)
                        allCenter()
                        touchEnable(true)
                        zIndex(10)
                        capture(CaptureRule.click())
                    }
                    event { click { onRemove(imageUri) } }
                    View {
                        attr {
                            size(22f, 22f)
                            borderRadius(11f)
                            backgroundColor(Color(0xCC17201D))
                            allCenter()
                            touchEnable(false)
                        }
                        Text {
                            attr {
                                text("×")
                                fontSize(16f)
                                color(Color.WHITE)
                                touchEnable(false)
                            }
                        }
                    }
                }
            }
        }
    }
}
