#import "ChatZoomImage.h"

#import <ImageIO/ImageIO.h>

static const CGFloat kMinScale = 1.0;
static const CGFloat kMaxScale = 4.0;
static const CGFloat kMaxBitmapSide = 2048.0;

@interface ChatZoomImage () <UIGestureRecognizerDelegate>
@property (nonatomic, strong, nullable) UIImage *previewImage;
@property (nonatomic, copy, nullable) KuiklyRenderCallback closeCallback;
@property (nonatomic, assign) CGFloat userScale;
@property (nonatomic, assign) CGFloat transX;
@property (nonatomic, assign) CGFloat transY;
@property (nonatomic, assign) CGFloat pinchBase;
@property (nonatomic, assign) CGFloat panBaseX;
@property (nonatomic, assign) CGFloat panBaseY;
@property (nonatomic, assign) NSInteger loadToken;
@property (nonatomic, strong) UIPinchGestureRecognizer *pinch;
@end

@implementation ChatZoomImage

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        self.backgroundColor = UIColor.clearColor;
        self.opaque = NO;
        self.clipsToBounds = YES;
        self.contentMode = UIViewContentModeRedraw;
        _userScale = kMinScale;
        _pinch = [[UIPinchGestureRecognizer alloc] initWithTarget:self action:@selector(onPinch:)];
        _pinch.delegate = self;
        [self addGestureRecognizer:_pinch];
        UIPanGestureRecognizer *pan = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(onPan:)];
        pan.delegate = self;
        pan.maximumNumberOfTouches = 1;
        [self addGestureRecognizer:pan];
        UITapGestureRecognizer *tap = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(onTap)];
        [self addGestureRecognizer:tap];
    }
    return self;
}

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
    if ([propKey isEqualToString:@"src"]) {
        [self setCss_src:propValue];
    } else if ([propKey isEqualToString:@"close"]) {
        [self setCss_close:propValue];
    }
}

- (void)setCss_src:(NSString *)src {
    [self resetTransform];
    [self load:src];
}

- (void)setCss_close:(KuiklyRenderCallback)callback {
    self.closeCallback = callback;
}

- (void)drawRect:(CGRect)rect {
    UIImage *image = self.previewImage;
    if (image == nil || CGRectIsEmpty(self.bounds)) {
        return;
    }
    CGFloat drawScale = [self fitScale:image] * self.userScale;
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextSaveGState(context);
    CGContextTranslateCTM(context, CGRectGetWidth(self.bounds) / 2.0 + self.transX, CGRectGetHeight(self.bounds) / 2.0 + self.transY);
    CGContextScaleCTM(context, drawScale, drawScale);
    CGSize size = image.size;
    [image drawInRect:CGRectMake(-size.width / 2.0, -size.height / 2.0, size.width, size.height)];
    CGContextRestoreGState(context);
}

- (void)onPinch:(UIPinchGestureRecognizer *)gesture {
    if (gesture.state == UIGestureRecognizerStateBegan) {
        self.pinchBase = self.userScale;
    }
    CGFloat next = MIN(kMaxScale, MAX(kMinScale, self.pinchBase * gesture.scale));
    [self applyScale:next focus:[gesture locationInView:self]];
    if (gesture.state == UIGestureRecognizerStateEnded || gesture.state == UIGestureRecognizerStateCancelled) {
        if (self.userScale <= 1.01) {
            [self resetTransform];
            [self setNeedsDisplay];
        }
    }
}

- (void)onPan:(UIPanGestureRecognizer *)gesture {
    if (self.pinch.state == UIGestureRecognizerStateBegan || self.pinch.state == UIGestureRecognizerStateChanged) {
        return;
    }
    if (self.userScale <= 1.01 || self.previewImage == nil) {
        return;
    }
    if (gesture.state == UIGestureRecognizerStateBegan) {
        self.panBaseX = self.transX;
        self.panBaseY = self.transY;
    }
    CGPoint translation = [gesture translationInView:self];
    self.transX = self.panBaseX + translation.x;
    self.transY = self.panBaseY + translation.y;
    [self clamp:self.previewImage];
    [self setNeedsDisplay];
}

- (void)onTap {
    if (self.userScale <= 1.01 && self.closeCallback) {
        self.closeCallback(nil);
    }
}

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer
    shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    return gestureRecognizer == self.pinch || otherGestureRecognizer == self.pinch;
}

- (void)applyScale:(CGFloat)next focus:(CGPoint)focus {
    CGFloat old = self.userScale;
    CGFloat width = CGRectGetWidth(self.bounds);
    CGFloat height = CGRectGetHeight(self.bounds);
    if (old > 0) {
        CGFloat localX = (focus.x - width / 2.0 - self.transX) / old;
        CGFloat localY = (focus.y - height / 2.0 - self.transY) / old;
        self.userScale = next;
        self.transX = focus.x - width / 2.0 - localX * next;
        self.transY = focus.y - height / 2.0 - localY * next;
    } else {
        self.userScale = next;
    }
    if (self.userScale <= 1.01) {
        [self resetTransform];
    } else if (self.previewImage) {
        [self clamp:self.previewImage];
    }
    [self setNeedsDisplay];
}

- (void)clamp:(UIImage *)image {
    CGFloat fit = [self fitScale:image];
    if (fit <= 0) {
        return;
    }
    CGFloat drawnW = image.size.width * fit * self.userScale;
    CGFloat drawnH = image.size.height * fit * self.userScale;
    CGFloat maxX = MAX(0, (drawnW - CGRectGetWidth(self.bounds)) / 2.0);
    CGFloat maxY = MAX(0, (drawnH - CGRectGetHeight(self.bounds)) / 2.0);
    self.transX = MIN(maxX, MAX(-maxX, self.transX));
    self.transY = MIN(maxY, MAX(-maxY, self.transY));
}

- (CGFloat)fitScale:(UIImage *)image {
    if (CGRectIsEmpty(self.bounds) || image.size.width <= 0 || image.size.height <= 0) {
        return 0;
    }
    return MIN(CGRectGetWidth(self.bounds) / image.size.width, CGRectGetHeight(self.bounds) / image.size.height);
}

- (void)resetTransform {
    self.userScale = kMinScale;
    self.transX = 0;
    self.transY = 0;
}

- (void)load:(NSString *)src {
    NSInteger token = ++self.loadToken;
    self.previewImage = nil;
    [self setNeedsDisplay];
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
        UIImage *decoded = ChatZoomDecodeImage(src);
        dispatch_async(dispatch_get_main_queue(), ^{
            if (token != self.loadToken) {
                return;
            }
            self.previewImage = decoded;
            [self setNeedsDisplay];
        });
    });
}

static UIImage *ChatZoomDecodeImage(NSString *src) {
    NSData *bytes = ChatZoomReadBytes(src);
    if (bytes.length == 0) {
        return nil;
    }
    CGImageSourceRef source = CGImageSourceCreateWithData((__bridge CFDataRef)bytes, NULL);
    if (source == NULL) {
        return [UIImage imageWithData:bytes];
    }
    NSDictionary *options = @{
        (id)kCGImageSourceCreateThumbnailFromImageAlways: @YES,
        (id)kCGImageSourceThumbnailMaxPixelSize: @(kMaxBitmapSide),
        (id)kCGImageSourceCreateThumbnailWithTransform: @YES,
    };
    CGImageRef image = CGImageSourceCreateThumbnailAtIndex(source, 0, (__bridge CFDictionaryRef)options);
    CFRelease(source);
    if (image == NULL) {
        return [UIImage imageWithData:bytes];
    }
    UIImage *result = [UIImage imageWithCGImage:image];
    CGImageRelease(image);
    return result;
}

static NSData *ChatZoomReadBytes(NSString *src) {
    if (src.length == 0) {
        return nil;
    }
    if ([src hasPrefix:@"data:image"]) {
        NSRange comma = [src rangeOfString:@","];
        if (comma.location == NSNotFound) {
            return nil;
        }
        return [[NSData alloc] initWithBase64EncodedString:[src substringFromIndex:comma.location + 1]
                                                    options:NSDataBase64DecodingIgnoreUnknownCharacters];
    }
    if ([src hasPrefix:@"http://"] || [src hasPrefix:@"https://"] || [src hasPrefix:@"file:"]) {
        return [NSData dataWithContentsOfURL:[NSURL URLWithString:src]];
    }
    if ([src hasPrefix:@"/"]) {
        return [NSData dataWithContentsOfFile:src];
    }
    return nil;
}

@end
