# Kuikly 预览 Mock 数据参考

本文件提供预览环境下常用的 Mock 数据定义，可在预览代码中直接引用。

## 1. Mock 图片 URL

```kotlin
object MockImages {
    // ===== 头像图片 =====
    val AVATAR_1 = "https://picsum.photos/id/64/200/200"
    val AVATAR_2 = "https://picsum.photos/id/65/200/200"
    val AVATAR_3 = "https://picsum.photos/id/91/200/200"
    val AVATAR_4 = "https://picsum.photos/id/177/200/200"
    val AVATAR_DEFAULT = "https://picsum.photos/id/237/200/200"

    // ===== 封面图片（宽图，适用于 Banner / 封面） =====
    val COVER_1 = "https://picsum.photos/id/10/800/400"
    val COVER_2 = "https://picsum.photos/id/29/800/400"

    // ===== 九宫格图片（方图，适用于图片网格） =====
    val GRID_1 = "https://picsum.photos/id/100/400/400"
    val GRID_2 = "https://picsum.photos/id/101/400/400"
    val GRID_3 = "https://picsum.photos/id/102/400/400"
    val GRID_4 = "https://picsum.photos/id/103/400/400"
    val GRID_5 = "https://picsum.photos/id/104/400/400"
    val GRID_6 = "https://picsum.photos/id/106/400/400"

    // ===== 缩略图 / 占位图 =====
    val THUMB_1 = "https://picsum.photos/id/110/150/150"
    val PLACEHOLDER = "https://picsum.photos/id/0/300/300"

    // ===== 背景图片 =====
    val BG_GRADIENT = "https://picsum.photos/id/1015/1080/1920"

    // 便捷获取一组图片（最多 6 张）
    fun gridImages(count: Int): List<String> {
        val all = listOf(GRID_1, GRID_2, GRID_3, GRID_4, GRID_5, GRID_6)
        return all.take(count.coerceIn(0, 6))
    }

    // 便捷获取头像列表
    fun avatars(count: Int): List<String> {
        val all = listOf(AVATAR_1, AVATAR_2, AVATAR_3, AVATAR_4, AVATAR_DEFAULT)
        return all.take(count.coerceIn(0, 5))
    }
}
```

## 2. Mock 视频 URL

```kotlin
object MockVideos {
    val VIDEO_MP4_SHORT = "https://www.w3schools.com/html/mov_bbb.mp4"
    val VIDEO_MP4_BUNNY = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    val SHORT_VIDEO_1 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
    val SHORT_VIDEO_2 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"

    // 视频封面图
    val VIDEO_POSTER_1 = "https://picsum.photos/id/180/640/360"
}
```

## 3. Mock 用户信息

```kotlin
object MockUsers {

    fun createUser(
        id: String = "10001",
        nick: String = "Demo User",
        headUrl: String = MockImages.AVATAR_DEFAULT,
        desc: String = "This is a demo user for preview",
        isMember: Int = 0,
        isVerify: Int = 0
    ): AppUserInfo {
        return AppUserInfo(
            id = id, nick = nick, headUrl = headUrl,
            desc = desc, isMember = isMember, isVerify = isVerify
        )
    }

    val USER_NORMAL = createUser(id = "10001", nick = "张三", headUrl = MockImages.AVATAR_1, desc = "普通用户")
    val USER_VIP = createUser(id = "10002", nick = "李四", headUrl = MockImages.AVATAR_2, desc = "VIP会员 | 科技数码博主", isMember = 1)
    val USER_VERIFIED = createUser(id = "10003", nick = "王五", headUrl = MockImages.AVATAR_3, desc = "认证用户 | 旅行摄影师", isVerify = 1)
    val USER_VIP_VERIFIED = createUser(id = "10004", nick = "赵六", headUrl = MockImages.AVATAR_4, desc = "VIP + 认证 | 美食探店达人", isMember = 1, isVerify = 1)
}
```

## 4. Mock Feed 数据

```kotlin
object MockFeeds {

    fun createFeed(
        content: String = "这是一条测试动态内容，用于预览环境下展示 Feed 列表的效果。",
        userInfo: AppUserInfo = MockUsers.USER_NORMAL,
        picUrls: List<String> = emptyList(),
        videoUrl: String = "",
        containForward: Boolean = false,
        forwardContent: String? = null,
        forwardNick: String? = null,
        likeNum: Int = 42,
        commentNum: Int = 8,
        forwardNum: Int = 3
    ): AppFeedModel {
        return AppFeedModel(
            appId = "mockApp", categoryId = "preview", content = content,
            userInfo = userInfo, picUrl = picUrls,
            forwardContent = forwardContent, forwardNick = forwardNick,
            forwardUserId = null, forwardPicUrl = null, forwardAppId = null, forwardVideoUrl = null,
            containForward = containForward, videoUrl = videoUrl, tail = "Kuikly Preview",
            createTime = MockTimes.timeAt(0),
            likeStatus = 0, forwardNum = forwardNum, likeNum = likeNum, commentNum = commentNum
        )
    }

    // ===== 预置 Feed =====
    val FEED_TEXT_ONLY = createFeed(content = "今天天气真好，适合出去走走！🌞", userInfo = MockUsers.USER_NORMAL, likeNum = 128, commentNum = 32)
    val FEED_SINGLE_IMAGE = createFeed(content = "分享一张今天拍到的风景照 📸", userInfo = MockUsers.USER_VIP, picUrls = listOf(MockImages.GRID_1), likeNum = 256, commentNum = 45)
    val FEED_THREE_IMAGES = createFeed(content = "周末去了新开的网红餐厅 🍽️", userInfo = MockUsers.USER_VIP_VERIFIED, picUrls = MockImages.gridImages(3), likeNum = 512, commentNum = 89, forwardNum = 23)
    val FEED_SIX_IMAGES = createFeed(content = "假期旅行合集 ✈️", userInfo = MockUsers.USER_VERIFIED, picUrls = MockImages.gridImages(6), likeNum = 1024, commentNum = 156, forwardNum = 78)
    val FEED_VIDEO = createFeed(content = "拍了一段日落延时摄影 🌅", userInfo = MockUsers.USER_VIP, videoUrl = MockVideos.SHORT_VIDEO_1, likeNum = 2048, commentNum = 234)
    val FEED_FORWARD = createFeed(content = "这个太有意思了，转发一下！", userInfo = MockUsers.USER_NORMAL, containForward = true, forwardContent = "Kuikly 是一个优秀的跨平台 UI 框架 🚀", forwardNick = "Kuikly官方", likeNum = 64, commentNum = 12, forwardNum = 8)

    // 获取标准 Feed 列表
    fun feedList(count: Int = 6): List<AppFeedModel> {
        val allFeeds = listOf(FEED_TEXT_ONLY, FEED_SINGLE_IMAGE, FEED_THREE_IMAGES, FEED_NINE_IMAGES, FEED_VIDEO, FEED_FORWARD)
        return (0 until count).map { allFeeds[it % allFeeds.size] }
    }
}
```

## 5. Mock 时间/日期

```kotlin
object MockTimes {
    val JUST_NOW = "刚刚"
    val MINUTES_AGO_5 = "5分钟前"
    val MINUTES_AGO_30 = "30分钟前"
    val HOUR_AGO_1 = "1小时前"
    val HOURS_AGO_3 = "3小时前"
    val YESTERDAY = "昨天"
    val DAYS_AGO_3 = "3天前"
    val DATE_FULL = "2024-01-15"
    val DATE_CN = "1月15日"

    fun timeAt(index: Int): String {
        val all = listOf(JUST_NOW, MINUTES_AGO_5, MINUTES_AGO_30, HOUR_AGO_1, HOURS_AGO_3, YESTERDAY, DAYS_AGO_3, DATE_CN, DATE_FULL)
        return all[index % all.size]
    }
}
```

## 6. Mock 文本变体

```kotlin
object MockTexts {
    val EMPTY = ""
    val SHORT = "短文本"
    val NORMAL = "这是一段普通长度的文本内容，用于预览展示。"
    val LONG = "这是一段较长的文本内容，用于测试多行文本的显示效果。在实际业务场景中，用户可能会输入很长的文字描述，需要确保 UI 在长文本下仍然能正确展示，包括换行、省略号截断、滚动等行为。"
    val EMOJI_DENSE = "🎉🎊✨🌟💫🔥❤️👍🏻😄🥳"
    val MIXED_EMOJI = "今天心情很好 🌞 去了新餐厅 🍽️ 味道不错 👍"
    val WITH_NEWLINES = "第一行\n第二行\n第三行"

    fun textAt(index: Int): String {
        val all = listOf(SHORT, NORMAL, LONG, MIXED_EMOJI, NORMAL, SHORT)
        return all[index % all.size]
    }
}
```

## 7. Mock 数字/统计

```kotlin
object MockNumbers {
    val ZERO = 0
    val FEW = 3
    val NORMAL = 42
    val HUNDRED = 128
    val THOUSAND = 1024
    val LARGE = 9999
    val VERY_LARGE = 128000

    fun formatCount(count: Int): String = when {
        count < 1000 -> count.toString()
        count < 10000 -> String.format("%.1fk", count / 1000.0)
        count < 100000000 -> String.format("%.1fw", count / 10000.0)
        else -> "1亿+"
    }

    fun countAt(index: Int): Int {
        val all = listOf(3, 42, 128, 256, 512, 1024, 2048, 9999)
        return all[index % all.size]
    }

    fun formattedCountAt(index: Int): String = formatCount(countAt(index))
}
```

## 8. Mock 颜色常量

```kotlin
object MockColors {
    // 品牌色
    val PRIMARY = Color(0xFF6200EE)
    val SECONDARY = Color(0xFF03DAC6)
    // 背景色
    val BG_WHITE = Color(0xFFFFFFFF)
    val BG_LIGHT_GRAY = Color(0xFFF5F5F5)
    // 文字色
    val TEXT_PRIMARY = Color(0xFF111111)
    val TEXT_SECONDARY = Color(0xFF666666)
    // 状态色
    val SUCCESS = Color(0xFF4CAF50)
    val ERROR = Color(0xFFF44336)
}
```

## 9. 统一 Mock 数据入口

```kotlin
object MockDataProvider {
    val images = MockImages
    val videos = MockVideos
    val users = MockUsers
    val feeds = MockFeeds
    val colors = MockColors
}
```

> **Mock 数据使用原则**：在 Manager 层或 Module 层做源头拦截，详见 [global-dependency-injection.md](global-dependency-injection.md) 模式 3/4。