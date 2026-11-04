#import <Foundation/Foundation.h>
#import "KRBaseModule.h"

NS_ASSUME_NONNULL_BEGIN

@class KRSseSessionDelegate;

/// iOS SSE transport. Class name matches the Kotlin module name `KRSseModule`.
@interface KRSseModule : KRBaseModule

@property (nonatomic, strong) NSMutableDictionary<NSString *, NSURLSessionDataTask *> *activeStreamTasks;
@property (nonatomic, strong) NSMutableDictionary<NSString *, KRSseSessionDelegate *> *activeStreamDelegates;
@property (nonatomic, strong) NSMutableDictionary<NSString *, NSURLSession *> *activeStreamSessions;

@end

NS_ASSUME_NONNULL_END
