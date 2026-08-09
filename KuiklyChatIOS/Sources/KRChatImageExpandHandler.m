#import "KRChatImageExpandHandler.h"

#import <SDWebImage/SDImageCodersManager.h>
#import <SDWebImage/SDWebImageManager.h>
#import <SDWebImage/UIImageView+WebCache.h>
#import <SDWebImageSVGCoder/SDWebImageSVGCoder.h>

static NSString * const kAssetsScheme = @"assets://";
static NSString * const kFileScheme = @"file://";
static NSString * const kLogTag = @"[KuiklyChatImage]";

/// Strong reference so KuiklyRenderBridge never holds a deallocated handler.
static KRChatImageExpandHandler *sRegisteredHandler;

static void KRChatImageLog(NSString *format, ...) {
#if DEBUG
    va_list args;
    va_start(args, format);
    NSString *message = [[NSString alloc] initWithFormat:format arguments:args];
    va_end(args);
    NSLog(@"%@ %@", kLogTag, message);
#else
    (void)format;
#endif
}

static BOOL KRChatIsSvgPath(NSString *value) {
    if (value.length == 0) {
        return NO;
    }
    NSString *path = value;
    if ([value hasPrefix:kFileScheme]) {
        NSURL *fileURL = [NSURL URLWithString:value];
        if (fileURL.path.length > 0) {
            path = fileURL.path;
        } else {
            path = [value substringFromIndex:kFileScheme.length];
        }
    } else if ([value hasPrefix:kAssetsScheme]) {
        path = [value substringFromIndex:kAssetsScheme.length];
    }
    return [[path pathExtension] caseInsensitiveCompare:@"svg"] == NSOrderedSame;
}

static NSString *KRChatAbsolutePathForUrl(NSString *url) {
    if (url.length == 0) {
        return nil;
    }
    if ([url hasPrefix:kFileScheme]) {
        NSURL *fileURL = [NSURL URLWithString:url];
        if (fileURL.path.length > 0) {
            return fileURL.path;
        }
        return [url substringFromIndex:kFileScheme.length];
    }
    if ([url hasPrefix:kAssetsScheme]) {
        NSString *relativePath = [url substringFromIndex:kAssetsScheme.length];
        return [[[NSBundle mainBundle] bundlePath] stringByAppendingPathComponent:relativePath];
    }
    return nil;
}

/// Kuikly validates css_src against imageURL in p_srcMatch — keep the original request URL.
static NSURL *KRChatCallbackURL(NSString *requestUrl, NSString *path) {
    if (requestUrl.length > 0) {
        NSURL *requestNSURL = [NSURL URLWithString:requestUrl];
        if (requestNSURL != nil) {
            return requestNSURL;
        }
    }
    if (path.length > 0) {
        return [NSURL fileURLWithPath:path];
    }
    return nil;
}

/// Lucide SVG icons use stroke/fill=currentColor; SDWebImageSVGCoder renders that as transparent without a DOM context.
static NSData *KRChatNormalizeSvgData(NSData *data) {
    NSString *svg = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    if (svg.length == 0) {
        return data;
    }
    NSString *normalized = [svg stringByReplacingOccurrencesOfString:@"currentColor"
                                                            withString:@"#1E293B"
                                                               options:NSCaseInsensitiveSearch
                                                                 range:NSMakeRange(0, svg.length)];
    return [normalized dataUsingEncoding:NSUTF8StringEncoding] ?: data;
}

/// Flatten SVG-backed UIImage so Kuikly css_colorFilter / tint can process it reliably.
static UIImage *KRChatPrepareSvgImage(UIImage *image) {
    if (image == nil) {
        return nil;
    }
    CGSize size = image.size;
    if (size.width <= 0 || size.height <= 0) {
        return image;
    }
    CGFloat scale = [UIScreen mainScreen].scale;
    UIGraphicsImageRendererFormat *format = [UIGraphicsImageRendererFormat defaultFormat];
    format.opaque = NO;
    format.scale = scale > 0 ? scale : 1.0;
    UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithSize:size format:format];
    UIImage *flattened = [renderer imageWithActions:^(UIGraphicsImageRendererContext * _Nonnull context) {
        [image drawInRect:CGRectMake(0, 0, size.width, size.height)];
    }];
    // Template mode lets Kuikly css_tintColor / imageWithTintColor recolor the alpha mask on iOS.
    return [flattened imageWithRenderingMode:UIImageRenderingModeAlwaysTemplate];
}

static UIImage *KRChatDecodeSvgAtPath(NSString *path) {
    if (path.length == 0) {
        KRChatImageLog(@"decodeSvg skipped: empty path");
        return nil;
    }
    BOOL exists = [[NSFileManager defaultManager] fileExistsAtPath:path];
    if (!exists) {
        KRChatImageLog(@"decodeSvg file missing: %@", path);
        return nil;
    }
    NSData *data = [NSData dataWithContentsOfFile:path];
    if (data.length == 0) {
        KRChatImageLog(@"decodeSvg empty data: %@", path);
        return nil;
    }
    data = KRChatNormalizeSvgData(data);
    UIImage *image = [[SDImageCodersManager sharedManager] decodedImageWithData:data options:nil];
    image = KRChatPrepareSvgImage(image);
    if (image == nil) {
        KRChatImageLog(@"decodeSvg coder returned nil (%lu bytes): %@", (unsigned long)data.length, path);
    } else {
        KRChatImageLog(@"decodeSvg ok size=%@ scale=%.1f path=%@",
                       NSStringFromCGSize(image.size),
                       image.scale,
                       path);
    }
    return image;
}

static void KRChatFinishLoad(NSString *phase,
                             UIImage * _Nullable image,
                             NSError * _Nullable error,
                             NSURL * _Nullable imageURL,
                             UIImageView * _Nullable imageView,
                             ImageCompletionBlock _Nullable completeBlock) {
    void (^finish)(void) = ^{
        if (imageView != nil && image != nil) {
            imageView.image = image;
        }
        if (completeBlock != nil) {
            completeBlock(image, error, imageURL);
        }
        if (error != nil) {
            KRChatImageLog(@"%@ failed url=%@ error=%@",
                           phase,
                           imageURL.absoluteString ?: @"(nil)",
                           error.localizedDescription);
        } else if (image == nil) {
            KRChatImageLog(@"%@ no image url=%@ imageView=%@",
                           phase,
                           imageURL.absoluteString ?: @"(nil)",
                           imageView);
        } else {
            KRChatImageLog(@"%@ success size=%@ url=%@ imageView=%@ complete=%@",
                           phase,
                           NSStringFromCGSize(image.size),
                           imageURL.absoluteString ?: @"(nil)",
                           imageView,
                           completeBlock != nil ? @"yes" : @"no");
        }
    };
    if ([NSThread isMainThread]) {
        finish();
    } else {
        dispatch_async(dispatch_get_main_queue(), finish);
    }
}

static BOOL KRChatLoadSvg(NSString *requestUrl,
                          NSString *path,
                          UIImageView * _Nullable imageView,
                          ImageCompletionBlock _Nullable completeBlock) {
    UIImage *image = KRChatDecodeSvgAtPath(path);
    NSURL *callbackURL = KRChatCallbackURL(requestUrl, path);
    if (image == nil) {
        if (completeBlock != nil) {
            NSError *error = [NSError errorWithDomain:@"KuiklyChatImage"
                                                 code:-1
                                             userInfo:@{NSLocalizedDescriptionKey: @"SVG decode failed"}];
            KRChatFinishLoad(@"svg", nil, error, callbackURL, imageView, completeBlock);
        }
        return completeBlock != nil;
    }
    KRChatFinishLoad(@"svg", image, nil, callbackURL, imageView, completeBlock);
    return YES;
}

static BOOL KRChatLoadHttp(NSString *requestUrl,
                           UIImageView * _Nullable imageView,
                           ImageCompletionBlock _Nullable completeBlock) {
    NSURL *remoteURL = [NSURL URLWithString:requestUrl];
    if (remoteURL == nil) {
        return NO;
    }
    NSURL *callbackURL = KRChatCallbackURL(requestUrl, nil);
    if (imageView != nil) {
        [imageView sd_setImageWithURL:remoteURL
                            completed:^(UIImage *image, NSError *error, SDImageCacheType cacheType, NSURL *imageURL) {
            KRChatFinishLoad(@"http", image, error, callbackURL ?: imageURL, imageView, completeBlock);
        }];
        return YES;
    }
    [[SDWebImageManager sharedManager] loadImageWithURL:remoteURL
                                                options:0
                                               progress:nil
                                              completed:^(UIImage *image,
                                                          NSData *data,
                                                          NSError *error,
                                                          SDImageCacheType cacheType,
                                                          BOOL finished,
                                                          NSURL *imageURL) {
        if (!finished) {
            return;
        }
        KRChatFinishLoad(@"http", image, error, callbackURL ?: imageURL, nil, completeBlock);
    }];
    return YES;
}

static BOOL KRChatLoadRasterFile(NSString *requestUrl,
                                 NSString *path,
                                 UIImageView * _Nullable imageView,
                                 ImageCompletionBlock _Nullable completeBlock) {
    NSURL *fileURL = [NSURL fileURLWithPath:path];
    NSURL *callbackURL = KRChatCallbackURL(requestUrl, path);
    if (imageView != nil) {
        [imageView sd_setImageWithURL:fileURL
                            completed:^(UIImage *image, NSError *error, SDImageCacheType cacheType, NSURL *imageURL) {
            KRChatFinishLoad(@"file", image, error, callbackURL ?: imageURL, imageView, completeBlock);
        }];
        return YES;
    }
    NSData *data = [NSData dataWithContentsOfURL:fileURL];
    UIImage *image = data.length > 0 ? [UIImage imageWithData:data scale:[UIScreen mainScreen].scale] : nil;
    NSError *error = nil;
    if (image == nil) {
        error = [NSError errorWithDomain:@"KuiklyChatImage"
                                    code:-2
                                userInfo:@{NSLocalizedDescriptionKey: @"Local image decode failed"}];
    }
    KRChatFinishLoad(@"file", image, error, callbackURL, nil, completeBlock);
    return YES;
}

@implementation KRChatImageExpandHandler

+ (void)load {
    KRChatImageLog(@"+load");
    [self registerDefaultHandler];
}

+ (void)registerDefaultHandler {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        [[SDImageCodersManager sharedManager] addCoder:SDImageSVGCoder.sharedCoder];
        sRegisteredHandler = [KRChatImageExpandHandler new];
        [KuiklyRenderBridge registerComponentExpandHandler:sRegisteredHandler];
        id currentHandler = [KuiklyRenderBridge componentExpandHandler];
        KRChatImageLog(@"registerDefaultHandler handler=%@ bridgeHandler=%@ bundle=%@",
                       sRegisteredHandler,
                       currentHandler,
                       [NSBundle mainBundle].bundlePath);
    });
}

- (NSURL *)hr_customBundleUrlForFileName:(NSString *)fileName extension:(NSString *)fileExtension {
    NSString *relativePath = fileExtension.length > 0
        ? [NSString stringWithFormat:@"%@.%@", fileName, fileExtension]
        : fileName;
    NSString *fullPath = [[[NSBundle mainBundle] bundlePath] stringByAppendingPathComponent:relativePath];
    BOOL exists = [[NSFileManager defaultManager] fileExistsAtPath:fullPath];
    KRChatImageLog(@"customBundle fileName=%@.%@ exists=%d path=%@",
                   fileName,
                   fileExtension,
                   exists,
                   fullPath);
    if (exists) {
        return [NSURL fileURLWithPath:fullPath];
    }
    return nil;
}

- (BOOL)hr_setImageWithUrl:(NSString *)url
                imageParams:(NSDictionary *)imageParams
                  complete:(ImageCompletionBlock)completeBlock {
    KRChatImageLog(@"load imageParams url=%@ params=%@", url, imageParams);
    return [self p_loadUrl:url forImageView:nil complete:completeBlock];
}

- (BOOL)hr_setImageWithUrl:(NSString *)url
              forImageView:(UIImageView *)imageView
                  complete:(ImageCompletionBlock)completeBlock {
    KRChatImageLog(@"load forImageView+complete url=%@", url);
    return [self p_loadUrl:url forImageView:imageView complete:completeBlock];
}

- (BOOL)hr_setImageWithUrl:(NSString *)url forImageView:(UIImageView *)imageView {
    KRChatImageLog(@"load forImageView url=%@", url);
    return [self p_loadUrl:url forImageView:imageView complete:nil];
}

- (BOOL)p_loadUrl:(NSString *)url
     forImageView:(UIImageView *)imageView
         complete:(ImageCompletionBlock)completeBlock {
    if (url.length == 0) {
        if (imageView != nil) {
            imageView.image = nil;
        }
        KRChatFinishLoad(@"clear", nil, nil, nil, imageView, completeBlock);
        return YES;
    }

    if ([url hasPrefix:@"http://"] || [url hasPrefix:@"https://"]) {
        return KRChatLoadHttp(url, imageView, completeBlock);
    }

    NSString *path = KRChatAbsolutePathForUrl(url);
    BOOL isSvg = KRChatIsSvgPath(url);
    BOOL fileExists = path.length > 0 && [[NSFileManager defaultManager] fileExistsAtPath:path];
    KRChatImageLog(@"resolve url=%@ path=%@ isSvg=%d exists=%d imageView=%@",
                   url,
                   path ?: @"(nil)",
                   isSvg,
                   fileExists,
                   imageView);

    if (path.length > 0 && isSvg) {
        BOOL handled = KRChatLoadSvg(url, path, imageView, completeBlock);
        if (!handled) {
            KRChatImageLog(@"svg load failed url=%@ path=%@", url, path);
        }
        return handled;
    }

    if (fileExists && path.length > 0) {
        return KRChatLoadRasterFile(url, path, imageView, completeBlock);
    }

    KRChatImageLog(@"unhandled url=%@ path=%@", url, path ?: @"(nil)");
    if (completeBlock != nil) {
        NSError *error = [NSError errorWithDomain:@"KuiklyChatImage"
                                             code:-3
                                         userInfo:@{NSLocalizedDescriptionKey: @"Image resource not found"}];
        KRChatFinishLoad(@"missing", nil, error, KRChatCallbackURL(url, path), imageView, completeBlock);
        return YES;
    }
    return NO;
}

- (UIColor *)hr_colorWithValue:(NSString *)value {
    return nil;
}

@end
