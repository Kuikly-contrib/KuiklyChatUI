import { KRAny, KuiklyRenderBaseModule, KuiklyRenderCallback } from '@kuikly-open/render';
import fs from '@ohos.file.fs';
import util from '@ohos.util';

const DEFAULT_NAMESPACE = 'kuikly_chat_media';

interface ConfigureParams {
  namespace?: string;
}

class ChatFileByteStore {
  private readonly directory: string;

  constructor(namespace: string, cacheRoot: string) {
    this.directory = `${cacheRoot}/${normalizeNamespace(namespace)}`;
  }

  private ensureDirectory(): void {
    if (!fs.accessSync(this.directory)) {
      fs.mkdirSync(this.directory, true);
    }
  }

  private filePath(key: string): string {
    return `${this.directory}/${sha256Hex(key)}`;
  }

  get(key: string): Uint8Array | null {
    const path = this.filePath(key);
    if (!fs.accessSync(path)) {
      return null;
    }
    const file = fs.openSync(path, fs.OpenMode.READ_ONLY);
    try {
      const stat = fs.statSync(path);
      const buffer = new ArrayBuffer(stat.size);
      fs.readSync(file.fd, buffer);
      return new Uint8Array(buffer);
    } finally {
      fs.closeSync(file);
    }
  }

  set(key: string, value: Uint8Array): void {
    this.ensureDirectory();
    const path = this.filePath(key);
    const temporary = `${path}.tmp`;
    const file = fs.openSync(temporary, fs.OpenMode.READ_WRITE | fs.OpenMode.CREATE | fs.OpenMode.TRUNC);
    try {
      fs.writeSync(file.fd, value.buffer);
    } finally {
      fs.closeSync(file);
    }
    if (fs.accessSync(path)) {
      fs.unlinkSync(path);
    }
    fs.renameSync(temporary, path);
  }

  contains(key: string): boolean {
    return fs.accessSync(this.filePath(key));
  }

  delete(key: string): void {
    const path = this.filePath(key);
    if (fs.accessSync(path)) {
      fs.unlinkSync(path);
    }
  }

  updateKey(oldKey: string, newKey: string): void {
    this.ensureDirectory();
    const source = this.filePath(oldKey);
    if (!fs.accessSync(source)) {
      throw new Error('Cache miss');
    }
    const destination = this.filePath(newKey);
    if (fs.accessSync(destination)) {
      fs.unlinkSync(destination);
    }
    fs.renameSync(source, destination);
  }

  clear(): void {
    if (!fs.accessSync(this.directory)) {
      return;
    }
    const names = fs.listFileSync(this.directory);
    names.forEach((name) => {
      fs.unlinkSync(`${this.directory}/${name}`);
    });
  }
}

function normalizeNamespace(namespace: string): string {
  const normalized = namespace.trim().replace(/[^A-Za-z0-9._-]/g, '_').replace(/^\.+|\.+$/g, '');
  return normalized.length > 0 ? normalized : DEFAULT_NAMESPACE;
}

function sha256Hex(value: string): string {
  const digest = new util.Hash('sha256');
  digest.update(value);
  return digest.digest('hex');
}

export class KRChatByteCacheModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME = 'KRChatByteCacheModule';

  private store: ChatFileByteStore | null = null;

  syncMode(): boolean {
    return true;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    if (method === 'configure') {
      const payload = parseJsonParams<ConfigureParams>(params);
      const namespace = payload.namespace ?? DEFAULT_NAMESPACE;
      const cacheRoot = `${getContext(this).cacheDir}`;
      this.store = new ChatFileByteStore(namespace, cacheRoot);
      return null;
    }

    const activeStore = this.store;
    if (!activeStore) {
      throw new Error('Chat byte cache is not configured');
    }

    const arrayParams = parseArrayParams(params);
    switch (method) {
      case 'get':
        return activeStore.get(arrayParams[0] as string);
      case 'set':
        activeStore.set(arrayParams[0] as string, toUint8Array(arrayParams[1]));
        return null;
      case 'contains':
        return activeStore.contains(arrayParams[0] as string);
      case 'delete':
        activeStore.delete(arrayParams[0] as string);
        return null;
      case 'updateKey':
        activeStore.updateKey(arrayParams[0] as string, arrayParams[1] as string);
        return null;
      case 'clear':
        activeStore.clear();
        return null;
      case 'readLocalFile': {
        const path = (arrayParams[0] as string).trim().replace(/^file:\/\//, '');
        if (!fs.accessSync(path)) {
          return null;
        }
        const file = fs.openSync(path, fs.OpenMode.READ_ONLY);
        try {
          const stat = fs.statSync(path);
          const buffer = new ArrayBuffer(stat.size);
          fs.readSync(file.fd, buffer);
          return new Uint8Array(buffer);
        } finally {
          fs.closeSync(file);
        }
      }
      default:
        return null;
    }
  }
}

function parseJsonParams<T>(params: KRAny): T {
  if (typeof params === 'string') {
    return JSON.parse(params) as T;
  }
  if (typeof params === 'object' && params !== null) {
    return params as T;
  }
  return {} as T;
}

function parseArrayParams(params: KRAny): KRAny[] {
  if (Array.isArray(params)) {
    return params;
  }
  return [];
}

function toUint8Array(value: KRAny): Uint8Array {
  if (value instanceof Uint8Array) {
    return value;
  }
  if (value instanceof ArrayBuffer) {
    return new Uint8Array(value);
  }
  throw new Error('Cache value must be binary');
}
