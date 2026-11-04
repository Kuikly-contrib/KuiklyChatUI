import { hilog } from '@kit.PerformanceAnalysisKit';
import { http } from '@kit.NetworkKit';
import { BusinessError } from '@kit.BasicServicesKit';
import Url from '@ohos.url';
import { util } from '@kit.ArkTS';
import { KRAny, KuiklyRenderBaseModule, KuiklyRenderCallback } from '@kuikly-open/render';

interface SseParams {
  url: string;
  method: string;
  param: Record<string, Object> | null;
  headers: Record<string, string> | null;
  cookie: string | null;
  timeout: number;
  requestId: string;
}

interface SseEvent {
  event: string;
  data: string;
  headers?: string;
  statusCode?: number;
}

interface SseState {
  httpRequest: http.HttpRequest;
  idleTimer: number;
}

const DEFAULT_TIMEOUT_S = 30;
const STATE_CODE_UNKNOWN = -1000;
const STATE_CODE_TIMEOUT = -1001;
const HTTP_ERROR_CODE_TIMEOUT = 2300028;
const STREAM_NATIVE_READ_TIMEOUT_MS = 0x7FFFFFFF;
const CONTENT_TYPE_FORM = 'application/x-www-form-urlencoded; charset=UTF-8';

function sseLog(message: string): void {
  hilog.error(0x0001, 'KRSseModule', '%{public}s', message);
}

function sseEvent(event: string, data: string, statusCode?: number, headers?: string): SseEvent {
  const payload: SseEvent = { event: event, data: data };
  if (statusCode !== undefined) {
    payload.statusCode = statusCode;
  }
  if (headers !== undefined) {
    payload.headers = headers;
  }
  return payload;
}

function asRecord(params: KRAny): Record<string, Object> {
  if (typeof params === 'string') {
    return JSON.parse(params) as Record<string, Object>;
  }
  if (typeof params === 'object' && params !== null) {
    return params as Record<string, Object>;
  }
  return {};
}

function toFormValue(value: Object | null | undefined): string {
  if (value === null || value === undefined) {
    return '';
  }
  if (typeof value === 'string') {
    return value;
  }
  if (typeof value === 'object') {
    return JSON.stringify(value);
  }
  return `${value}`;
}

/**
 * HarmonyOS SSE transport copied from KuiklyUI main `KRNetworkModule` stream methods.
 */
export class KRSseModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME = 'KRSseModule';

  private activeStreamRequests: Map<string, SseState> = new Map();

  syncMode(): boolean {
    return false;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    if (method === 'httpStreamRequest') {
      this.httpStreamRequest(params, callback);
    } else if (method === 'closeStreamRequest') {
      this.closeStreamRequest(params);
    }
    return null;
  }

  onDestroy(): void {
    this.activeStreamRequests.forEach((state) => {
      clearTimeout(state.idleTimer);
      this.cleanupHttpRequest(state.httpRequest);
    });
    this.activeStreamRequests.clear();
  }

  private cleanupHttpRequest(httpRequest: http.HttpRequest): void {
    try {
      httpRequest.off('dataReceive');
      httpRequest.off('dataEnd');
      httpRequest.off('headersReceive');
      httpRequest.destroy();
    } catch (error) {
      sseLog(`cleanup httpRequest error: ${(error as BusinessError).message}`);
    }
  }

  private finalizeStreamRequest(requestId: string): boolean {
    const state = this.activeStreamRequests.get(requestId);
    if (!state) {
      return false;
    }
    this.activeStreamRequests.delete(requestId);
    clearTimeout(state.idleTimer);
    this.cleanupHttpRequest(state.httpRequest);
    return true;
  }

  private finishStreamRequest(requestId: string, callback: KuiklyRenderCallback | null, events: SseEvent[]): void {
    if (!this.finalizeStreamRequest(requestId) || !callback) {
      return;
    }
    events.forEach((event) => callback(event));
  }

  private httpStreamRequest(rawParams: KRAny, callback: KuiklyRenderCallback | null): void {
    let requestId = '';
    let httpRequest: http.HttpRequest | null = null;
    try {
      const params = this.parseParams(rawParams);
      requestId = params.requestId;
      if (!requestId) {
        callback?.(sseEvent('error', 'requestId is empty', STATE_CODE_UNKNOWN));
        return;
      }
      if (this.activeStreamRequests.has(requestId)) {
        callback?.(sseEvent('error', 'duplicate requestId', STATE_CODE_UNKNOWN));
        return;
      }
      const url = Url.URL.parseURL(params.url);
      const options: http.HttpRequestOptions = {
        method: params.method === 'POST' ? http.RequestMethod.POST : http.RequestMethod.GET,
        readTimeout: STREAM_NATIVE_READ_TIMEOUT_MS,
        connectTimeout: (params.timeout > 0 ? params.timeout : DEFAULT_TIMEOUT_S) * 1000,
        usingCache: false,
        expectDataType: http.HttpDataType.STRING,
        header: this.requestHeaders(params),
      };
      this.applyBody(params, url, options);
      const currentRequest = http.createHttp();
      httpRequest = currentRequest;
      const state: SseState = { httpRequest: currentRequest, idleTimer: -1 };
      this.activeStreamRequests.set(requestId, state);
      const idleTimeoutMs = (params.timeout > 0 ? params.timeout : DEFAULT_TIMEOUT_S) * 1000;
      const armIdleTimer = (): void => {
        clearTimeout(state.idleTimer);
        state.idleTimer = setTimeout(() => {
          this.finishStreamRequest(requestId, callback, [
            sseEvent('error', 'request timeout', STATE_CODE_TIMEOUT),
          ]);
        }, idleTimeoutMs);
      };
      armIdleTimer();
      const decoder = util.TextDecoder.create('utf-8', { ignoreBOM: true });
      let isFirstCallback = true;
      let responseHeaders = '';
      currentRequest.on('headersReceive', (header: Object) => {
        if (!this.activeStreamRequests.has(requestId)) return;
        armIdleTimer();
        try {
          responseHeaders = JSON.stringify(header);
        } catch (_error) {
          responseHeaders = '';
        }
      });
      currentRequest.on('dataReceive', (data: ArrayBuffer) => {
        if (!this.activeStreamRequests.has(requestId) || !callback) return;
        armIdleTimer();
        const text = decoder.decodeToString(new Uint8Array(data), { stream: true });
        if (!text) return;
        const eventData = sseEvent('data', text);
        if (isFirstCallback) {
          isFirstCallback = false;
          eventData.headers = responseHeaders;
        }
        callback(eventData);
      });
      currentRequest.requestInStream(url.toString(), options).then((code: number) => {
        if (code >= 200 && code <= 299) {
          const events: SseEvent[] = [];
          const tail = decoder.decodeToString(new Uint8Array(0), { stream: false });
          if (tail) {
            events.push(sseEvent('data', tail, undefined, isFirstCallback ? responseHeaders : undefined));
          }
          events.push(sseEvent('complete', ''));
          this.finishStreamRequest(requestId, callback, events);
        } else {
          this.finishStreamRequest(requestId, callback, [
            sseEvent('error', `HTTP error ${code}`, code, responseHeaders),
          ]);
        }
      }).catch((error: BusinessError) => {
        sseLog(`Stream request error: ${error.code} ${error.message}`);
        const isTimeout = error.code === HTTP_ERROR_CODE_TIMEOUT;
        this.finishStreamRequest(requestId, callback, [
          isTimeout ?
            sseEvent('error', 'request timeout', STATE_CODE_TIMEOUT) :
            sseEvent('error', error.message, STATE_CODE_UNKNOWN),
        ]);
      });
    } catch (error) {
      const message = `stream request error: ${(error as BusinessError).message}`;
      sseLog(message);
      const finalized = requestId ? this.finalizeStreamRequest(requestId) : false;
      if (!finalized && httpRequest) {
        this.cleanupHttpRequest(httpRequest);
      }
      callback?.(sseEvent('error', message, STATE_CODE_UNKNOWN));
    }
  }

  private requestHeaders(params: SseParams): Record<string, string> {
    const headers: Record<string, string> = {};
    if (params.headers) {
      Object.keys(params.headers).forEach((key) => {
        headers[key] = params.headers?.[key] ?? '';
      });
    }
    if (params.cookie) {
      headers['Cookie'] = params.cookie;
    }
    return headers;
  }

  private applyBody(params: SseParams, url: Url.URL, options: http.HttpRequestOptions): void {
    if (params.param == null) return;
    if (params.method === 'GET') {
      const queryParams = new Url.URLParams(url.search);
      Object.keys(params.param).forEach((key) => {
        queryParams.append(key, toFormValue(params.param?.[key]));
      });
      url.search = queryParams.toString();
      return;
    }
    if (params.method !== 'POST') return;
    const headers = (options.header as Record<string, string>) ?? {};
    const contentTypeKey = Object.keys(headers).find((key) => key.toLowerCase() === 'content-type');
    const contentType = contentTypeKey ? headers[contentTypeKey] : undefined;
    if (contentType && contentType.toLowerCase().indexOf('json') >= 0) {
      options.extraData = JSON.stringify(params.param);
    } else {
      const formParams = new Url.URLParams();
      Object.keys(params.param).forEach((key) => {
        formParams.append(key, toFormValue(params.param?.[key]));
      });
      options.extraData = formParams.toString();
      if (!contentType) {
        headers['Content-Type'] = CONTENT_TYPE_FORM;
      }
    }
    options.header = headers;
  }

  private parseParams(params: KRAny): SseParams {
    const source = asRecord(params);
    const requestId = source['requestId'];
    return {
      url: `${source['url'] ?? ''}`,
      method: `${source['method'] ?? 'GET'}`,
      param: (source['param'] as Record<string, Object> | undefined) ?? null,
      headers: (source['headers'] as Record<string, string> | undefined) ?? null,
      cookie: (source['cookie'] as string | undefined) ?? null,
      timeout: Number(source['timeout'] ?? DEFAULT_TIMEOUT_S),
      requestId: typeof requestId === 'string' ? requestId : '',
    };
  }

  private closeStreamRequest(params: KRAny): void {
    try {
      const requestId = asRecord(params)['requestId'];
      if (typeof requestId === 'string' && requestId.length > 0) {
        this.finalizeStreamRequest(requestId);
      }
    } catch (error) {
      sseLog(`close stream error: ${(error as BusinessError).message}`);
    }
  }
}
