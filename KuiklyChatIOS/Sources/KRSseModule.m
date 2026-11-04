#import "KRSseModule.h"

#import <OpenKuiklyIOSRender/NSObject+KR.h>

NSUInteger KRSseAppendSanitizedUTF8(const uint8_t *bytes, NSUInteger length, BOOL flush, NSMutableData *output);
NSString *KRSseFormURLEncodedString(NSDictionary *params);
NSURL *KRSseRequestURL(id urlString, NSString *method, id params);
NSString *KRSseExtractJsonObject(NSString *key, NSString *jsonStr);

@interface KRSseSessionDelegate : NSObject <NSURLSessionDataDelegate>

@property (nonatomic, copy) KuiklyRenderCallback callback;
@property (nonatomic, copy) NSString *requestId;
@property (nonatomic, assign) BOOL isFirstCallback;
@property (nonatomic, strong) NSHTTPURLResponse *httpResponse;
@property (nonatomic, strong) NSMutableData *pendingUTF8Data;
@property (nonatomic, copy) void (^onFinish)(NSString *requestId);

@end

@implementation KRSseSessionDelegate

- (instancetype)initWithCallback:(KuiklyRenderCallback)callback requestId:(NSString *)requestId {
    self = [super init];
    if (self) {
        _callback = callback;
        _requestId = requestId;
        _isFirstCallback = YES;
        _pendingUTF8Data = [NSMutableData data];
    }
    return self;
}

- (void)URLSession:(NSURLSession *)session
          dataTask:(NSURLSessionDataTask *)dataTask
didReceiveResponse:(NSURLResponse *)response
 completionHandler:(void (^)(NSURLSessionResponseDisposition))completionHandler {
    if ([response isKindOfClass:[NSHTTPURLResponse class]]) {
        self.httpResponse = (NSHTTPURLResponse *)response;
        NSInteger statusCode = self.httpResponse.statusCode;
        if (statusCode < 200 || statusCode > 299) {
            completionHandler(NSURLSessionResponseCancel);
            if (self.callback) {
                NSString *headers = [self.httpResponse.allHeaderFields hr_dictionaryToString] ?: @"";
                self.callback(@{
                    @"event": @"error",
                    @"data": [NSString stringWithFormat:@"HTTP error %ld", (long)statusCode],
                    @"headers": headers,
                    @"statusCode": @(statusCode)
                });
            }
            return;
        }
    }
    completionHandler(NSURLSessionResponseAllow);
}

- (void)URLSession:(NSURLSession *)session dataTask:(NSURLSessionDataTask *)dataTask didReceiveData:(NSData *)data {
    NSString *text = [self decodeUTF8Data:data flush:NO];
    if (text.length > 0) {
        [self emitDataText:text];
    }
}

- (void)emitDataText:(NSString *)text {
    if (!self.callback) return;
    NSMutableDictionary *eventData = [@{ @"event": @"data", @"data": text } mutableCopy];
    if (self.isFirstCallback && self.httpResponse) {
        self.isFirstCallback = NO;
        eventData[@"headers"] = [self.httpResponse.allHeaderFields hr_dictionaryToString] ?: @"";
        eventData[@"statusCode"] = @(self.httpResponse.statusCode);
    }
    self.callback([eventData copy]);
}

- (NSString *)decodeUTF8Data:(NSData *)data flush:(BOOL)flush {
    if (data.length > 0) {
        [self.pendingUTF8Data appendData:data];
    }
    NSUInteger length = self.pendingUTF8Data.length;
    if (length == 0) return @"";
    NSMutableData *sanitized = [NSMutableData dataWithCapacity:length];
    NSUInteger consumed = KRSseAppendSanitizedUTF8(self.pendingUTF8Data.bytes, length, flush, sanitized);
    [self.pendingUTF8Data replaceBytesInRange:NSMakeRange(0, consumed) withBytes:NULL length:0];
    if (sanitized.length == 0) return @"";
    return [[NSString alloc] initWithData:sanitized encoding:NSUTF8StringEncoding] ?: @"";
}

- (void)URLSession:(NSURLSession *)session task:(NSURLSessionTask *)task didCompleteWithError:(NSError *)error {
    if (self.callback) {
        if (error) {
            if (error.code != NSURLErrorCancelled) {
                BOOL isTimeout = error.code == NSURLErrorTimedOut;
                self.callback(@{
                    @"event": @"error",
                    @"data": isTimeout ? @"request timeout" : (error.localizedDescription ?: @"unknown error"),
                    @"statusCode": @(isTimeout ? -1001 : -1000)
                });
            }
        } else {
            NSString *tail = [self decodeUTF8Data:nil flush:YES];
            if (tail.length > 0) {
                [self emitDataText:tail];
            }
            self.callback(@{ @"event": @"complete", @"data": @"" });
        }
    }
    if (self.onFinish) {
        self.onFinish(self.requestId);
        self.onFinish = nil;
    }
}

@end

@implementation KRSseModule

@synthesize hr_rootView;

TDF_EXPORT_MODULE(KRSseModule)

- (NSMutableDictionary<NSString *, NSURLSessionDataTask *> *)activeStreamTasks {
    if (!_activeStreamTasks) _activeStreamTasks = [NSMutableDictionary dictionary];
    return _activeStreamTasks;
}

- (NSMutableDictionary<NSString *, KRSseSessionDelegate *> *)activeStreamDelegates {
    if (!_activeStreamDelegates) _activeStreamDelegates = [NSMutableDictionary dictionary];
    return _activeStreamDelegates;
}

- (NSMutableDictionary<NSString *, NSURLSession *> *)activeStreamSessions {
    if (!_activeStreamSessions) _activeStreamSessions = [NSMutableDictionary dictionary];
    return _activeStreamSessions;
}

- (void)kr_finalizeStreamRequestWithId:(NSString *)requestId {
    if (requestId.length == 0) return;
    NSURLSession *session = self.activeStreamSessions[requestId];
    [session invalidateAndCancel];
    [self.activeStreamSessions removeObjectForKey:requestId];
    [self.activeStreamTasks removeObjectForKey:requestId];
    [self.activeStreamDelegates removeObjectForKey:requestId];
}

- (void)httpStreamRequest:(NSDictionary *)args {
    NSString *argsJson = args[KR_PARAM_KEY];
    NSDictionary *param = [argsJson hr_stringToDictionary];
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    NSString *url = param[@"url"];
    NSString *method = param[@"method"];
    NSDictionary *requestParam = param[@"param"];
    NSDictionary *headers = param[@"headers"];
    NSString *cookie = param[@"cookie"];
    NSInteger timeout = [param[@"timeout"] intValue];
    NSString *requestId = param[@"requestId"];
    if (requestId.length == 0) {
        if (callback) callback(@{ @"event": @"error", @"data": @"requestId is empty", @"statusCode": @(-1000) });
        return;
    }
    if (self.activeStreamTasks[requestId] != nil) {
        if (callback) callback(@{ @"event": @"error", @"data": @"duplicate requestId", @"statusCode": @(-1000) });
        return;
    }
    NSURL *requestURL = KRSseRequestURL(url, method, requestParam);
    if (!requestURL) {
        if (callback) {
            callback(@{
                @"event": @"error",
                @"data": [NSString stringWithFormat:@"invalid url: %@", url ?: @""],
                @"statusCode": @(-1000)
            });
        }
        return;
    }
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] initWithURL:requestURL];
    request.HTTPMethod = method ?: @"GET";
    request.timeoutInterval = timeout > 0 ? timeout : 30;
    if (cookie.length > 0) {
        [request setValue:cookie forHTTPHeaderField:@"Cookie"];
    }
    for (NSString *key in headers) {
        [request setValue:[headers[key] description] forHTTPHeaderField:key];
    }
    if ([method isEqualToString:@"POST"] && [requestParam isKindOfClass:[NSDictionary class]]) {
        NSString *contentType = [request valueForHTTPHeaderField:@"Content-Type"];
        BOOL isJSON = contentType.length > 0 &&
            [contentType rangeOfString:@"json" options:NSCaseInsensitiveSearch].location != NSNotFound;
        if (isJSON) {
            NSString *rawParam = KRSseExtractJsonObject(@"param", argsJson);
            request.HTTPBody = rawParam ? [rawParam dataUsingEncoding:NSUTF8StringEncoding]
                                        : [NSJSONSerialization dataWithJSONObject:requestParam options:0 error:nil];
        } else {
            if (contentType.length == 0) {
                [request setValue:@"application/x-www-form-urlencoded; charset=UTF-8" forHTTPHeaderField:@"Content-Type"];
            }
            request.HTTPBody = [KRSseFormURLEncodedString(requestParam) dataUsingEncoding:NSUTF8StringEncoding];
        }
    }
    KRSseSessionDelegate *delegate = [[KRSseSessionDelegate alloc] initWithCallback:callback requestId:requestId];
    NSURLSessionConfiguration *config = [NSURLSessionConfiguration defaultSessionConfiguration];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:config
                                                          delegate:delegate
                                                     delegateQueue:[NSOperationQueue mainQueue]];
    NSURLSessionDataTask *task = [session dataTaskWithRequest:request];
    __weak typeof(self) weakSelf = self;
    delegate.onFinish = ^(NSString *finishedRequestId) {
        [weakSelf kr_finalizeStreamRequestWithId:finishedRequestId];
    };
    self.activeStreamTasks[requestId] = task;
    self.activeStreamDelegates[requestId] = delegate;
    self.activeStreamSessions[requestId] = session;
    [task resume];
}

- (void)closeStreamRequest:(NSDictionary *)args {
    NSDictionary *param = [args[KR_PARAM_KEY] hr_stringToDictionary];
    [self kr_finalizeStreamRequestWithId:param[@"requestId"]];
}

- (void)dealloc {
    for (NSString *requestId in [self.activeStreamSessions.allKeys copy]) {
        [self kr_finalizeStreamRequestWithId:requestId];
    }
    for (NSURLSessionDataTask *task in self.activeStreamTasks.allValues) {
        [task cancel];
    }
    [self.activeStreamTasks removeAllObjects];
    [self.activeStreamDelegates removeAllObjects];
    [self.activeStreamSessions removeAllObjects];
}

@end
