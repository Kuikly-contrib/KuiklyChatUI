import { common } from '@kit.AbilityKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { camera, cameraPicker } from '@kit.CameraKit';
import { fileIo } from '@kit.CoreFileKit';
import { image } from '@kit.ImageKit';
import { picker } from '@kit.CoreFileKit';
import { KRAny, KuiklyRenderBaseModule, KuiklyRenderCallback } from '@kuikly-open/render';
import util from '@ohos.util';

const THUMBNAIL_MAX_EDGE = 256;
const CAMERA_JPEG_QUALITY = 88;

interface PickParams {
  source?: string;
}

interface AttachmentPayload {
  id: string;
  kind: string;
  name: string;
  mimeType: string;
  url: string;
  bytesBase64: string;
  thumbnailBase64: string;
  sizeBytes: number;
}

interface PickResult {
  attachments: AttachmentPayload[];
  error: string;
}

export class KRChatAttachmentPickerModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME = 'KRChatAttachmentPickerModule';

  private pendingCallback: KuiklyRenderCallback | null = null;

  syncMode(): boolean {
    return false;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    switch (method) {
      case 'supportedSources':
        return this.supportedSources();
      case 'pick':
        this.pick(params, callback);
        return null;
      case 'cancel':
        this.cancel(callback);
        return null;
      default:
        return null;
    }
  }

  private supportedSources(): string {
    return 'PHOTO_LIBRARY,FILE,CAMERA';
  }

  private pick(params: KRAny, callback: KuiklyRenderCallback | null): void {
    if (callback == null) {
      return;
    }
    if (this.pendingCallback != null) {
      callback(this.emptyResult('Another attachment picker is active'));
      return;
    }

    const payload = parseJsonParams<PickParams>(params);
    const source = payload.source ?? '';
    this.pendingCallback = callback;

    const finish = (result: PickResult) => {
      const active = this.pendingCallback;
      this.pendingCallback = null;
      active?.(result);
    };

    if (source === 'PHOTO_LIBRARY') {
      this.pickFromPhotoLibrary(finish);
      return;
    }
    if (source === 'CAMERA') {
      this.pickFromCamera(finish);
      return;
    }
    if (source === 'FILE') {
      this.pickFromDocuments(finish);
      return;
    }
    finish(this.emptyResult('Unsupported attachment source'));
  }

  private cancel(callback: KuiklyRenderCallback | null): void {
    const active = this.pendingCallback;
    this.pendingCallback = null;
    active?.(this.emptyResult(''));
    callback?.(this.emptyResult(''));
  }

  private async pickFromPhotoLibrary(finish: (result: PickResult) => void): Promise<void> {
    try {
      const options = new picker.PhotoSelectOptions();
      options.MIMEType = picker.PhotoViewMIMETypes.IMAGE_TYPE;
      options.maxSelectNumber = 10;
      const photoPicker = new picker.PhotoViewPicker();
      const selection = await photoPicker.select(options);
      const uris = selection.photoUris ?? [];
      if (uris.length === 0) {
        finish(this.emptyResult(''));
        return;
      }
      const attachments = await this.attachmentsFromUris(uris, true);
      finish({
        attachments,
        error: attachments.length === 0 ? 'Unable to read selected photos' : '',
      });
    } catch (error) {
      finish(this.emptyResult(readErrorMessage(error)));
    }
  }

  private async pickFromCamera(finish: (result: PickResult) => void): Promise<void> {
    try {
      const context = getContext(this) as common.Context;
      const profile: cameraPicker.PickerProfile = {
        cameraPosition: camera.CameraPosition.CAMERA_POSITION_BACK,
      };
      const pickerResult = await cameraPicker.pick(
        context,
        [cameraPicker.PickerMediaType.PHOTO],
        profile,
      );
      if (pickerResult.resultCode !== 0 || pickerResult.resultUri.length === 0) {
        finish(this.emptyResult(''));
        return;
      }
      const attachment = await this.attachmentFromUri(pickerResult.resultUri, 'camera.jpg', 'image/jpeg');
      finish({
        attachments: attachment != null ? [attachment] : [],
        error: attachment != null ? '' : 'Camera did not return an image',
      });
    } catch (error) {
      finish(this.emptyResult(readErrorMessage(error)));
    }
  }

  private async pickFromDocuments(finish: (result: PickResult) => void): Promise<void> {
    try {
      const context = getContext(this) as common.UIAbilityContext;
      const options = new picker.DocumentSelectOptions();
      options.maxSelectNumber = 10;
      const documentPicker = new picker.DocumentViewPicker(context);
      const uris = await documentPicker.select(options);
      if (uris.length === 0) {
        finish(this.emptyResult(''));
        return;
      }
      const attachments = await this.attachmentsFromUris(uris, false);
      finish({
        attachments,
        error: uris.length > 0 && attachments.length === 0 ? 'Unable to read selected files' : '',
      });
    } catch (error) {
      finish(this.emptyResult(readErrorMessage(error)));
    }
  }

  private async attachmentsFromUris(uris: string[], imageOnly: boolean): Promise<AttachmentPayload[]> {
    const attachments: AttachmentPayload[] = [];
    for (const uri of uris) {
      const name = fileNameFromUri(uri, imageOnly ? 'image.jpg' : 'file');
      const mimeType = mimeTypeFromName(name);
      if (imageOnly && !mimeType.startsWith('image/')) {
        continue;
      }
      const attachment = await this.attachmentFromUri(uri, name, mimeType);
      if (attachment != null) {
        attachments.push(attachment);
      }
    }
    return attachments;
  }

  private async attachmentFromUri(
    uri: string,
    fallbackName: string,
    fallbackMimeType: string,
  ): Promise<AttachmentPayload | null> {
    const bytes = await readUriBytes(uri);
    if (bytes == null || bytes.length === 0) {
      return null;
    }
    const name = fallbackName;
    const mimeType = fallbackMimeType;
    return {
      id: util.generateRandomUUID(false),
      kind: attachmentKind(mimeType),
      name,
      mimeType,
      url: '',
      bytesBase64: encodeBase64(bytes),
      thumbnailBase64: await createThumbnailBase64(bytes, mimeType),
      sizeBytes: bytes.length,
    };
  }

  private emptyResult(error: string): PickResult {
    return { attachments: [], error };
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

function readErrorMessage(error: Object): string {
  const businessError = error as BusinessError;
  if (businessError?.message?.length > 0) {
    return businessError.message;
  }
  return 'Attachment picker failed';
}

function fileNameFromUri(uri: string, fallback: string): string {
  const decoded = decodeURIComponent(uri);
  const segments = decoded.split('/');
  const last = segments[segments.length - 1];
  return last.length > 0 ? last : fallback;
}

function mimeTypeFromName(name: string): string {
  const lower = name.toLowerCase();
  if (lower.endsWith('.png')) {
    return 'image/png';
  }
  if (lower.endsWith('.jpg') || lower.endsWith('.jpeg')) {
    return 'image/jpeg';
  }
  if (lower.endsWith('.webp')) {
    return 'image/webp';
  }
  if (lower.endsWith('.gif')) {
    return 'image/gif';
  }
  if (lower.endsWith('.heic')) {
    return 'image/heic';
  }
  if (lower.endsWith('.pdf')) {
    return 'application/pdf';
  }
  if (lower.endsWith('.txt')) {
    return 'text/plain';
  }
  if (lower.endsWith('.mp3')) {
    return 'audio/mpeg';
  }
  if (lower.endsWith('.mp4')) {
    return 'video/mp4';
  }
  return 'application/octet-stream';
}

function attachmentKind(mimeType: string): string {
  if (mimeType.startsWith('image/')) {
    return 'IMAGE';
  }
  if (mimeType.startsWith('audio/')) {
    return 'AUDIO';
  }
  return 'FILE';
}

async function readUriBytes(uri: string): Promise<Uint8Array | null> {
  let file: File | null = null;
  try {
    file = fileIo.openSync(uri, fileIo.OpenMode.READ_ONLY);
    const stat = fileIo.statSync(file.fd);
    const buffer = new ArrayBuffer(stat.size);
    fileIo.readSync(file.fd, buffer);
    return new Uint8Array(buffer);
  } catch (_error) {
    return null;
  } finally {
    if (file != null) {
      fileIo.closeSync(file);
    }
  }
}

function encodeBase64(bytes: Uint8Array): string {
  const helper = new util.Base64Helper();
  return helper.encodeToStringSync(bytes);
}

async function createThumbnailBase64(bytes: Uint8Array, mimeType: string): Promise<string> {
  if (!mimeType.startsWith('image/')) {
    return '';
  }
  let imageSource: image.ImageSource | null = null;
  let pixelMap: image.PixelMap | null = null;
  try {
    imageSource = image.createImageSource(
      bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength),
    );
    pixelMap = await imageSource.createPixelMap();
    const info = await pixelMap.getImageInfo();
    const maxEdge = Math.max(info.size.width, info.size.height);
    if (maxEdge > THUMBNAIL_MAX_EDGE) {
      const scale = THUMBNAIL_MAX_EDGE / maxEdge;
      await pixelMap.scale(scale, scale);
    }
    const packer = image.createImagePacker();
    const packed = await packer.packToData(pixelMap, {
      format: 'image/jpeg',
      quality: CAMERA_JPEG_QUALITY,
    });
    return encodeBase64(new Uint8Array(packed));
  } catch (_error) {
    return '';
  } finally {
    await pixelMap?.release();
    await imageSource?.release();
  }
}
