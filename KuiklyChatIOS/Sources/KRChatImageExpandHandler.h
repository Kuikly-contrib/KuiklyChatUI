#import <Foundation/Foundation.h>
#import <OpenKuiklyIOSRender/KuiklyRenderBridge.h>

NS_ASSUME_NONNULL_BEGIN

/// Default Kuikly image expand handler for Compose Chat (HTTP + assets + SVG).
@interface KRChatImageExpandHandler : NSObject<KuiklyRenderComponentExpandProtocol>

/// Registers SDWebImage SVGCoder and this handler with [KuiklyRenderBridge].
+ (void)registerDefaultHandler;

@end

NS_ASSUME_NONNULL_END
