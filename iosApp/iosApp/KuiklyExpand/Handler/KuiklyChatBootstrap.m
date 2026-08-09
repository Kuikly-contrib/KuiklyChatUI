#import <Foundation/Foundation.h>
#import <KuiklyChatIOS/KRChatImageExpandHandler.h>

/// Ensures KuiklyChat image/SVG handler is registered even when +load in the pod is not linked.
@interface KuiklyChatBootstrap : NSObject
@end

@implementation KuiklyChatBootstrap

+ (void)load {
#if DEBUG
    NSLog(@"[KuiklyChatImage] KuiklyChatBootstrap +load");
#endif
    [KRChatImageExpandHandler registerDefaultHandler];
}

@end
