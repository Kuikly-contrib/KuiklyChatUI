import {
  IKuiklyViewDelegate,
  KRRenderModuleExportCreator,
  KRRenderViewExportCreator
} from '@kuikly-open/render';
import { KRMyView } from './components/KRMyView';
import { ChatZoomImage } from './components/ChatZoomImage';
import { KRBridgeModule } from './modules/KRBridgeModule';
import { KRMyModule } from './modules/KRMyModule';
import { KRChatAttachmentPickerModule, KRChatByteCacheModule, KRSseModule } from '@kuikly-open/chat-cache';

export class KuiklyViewDelegate extends IKuiklyViewDelegate {
  getCustomRenderViewCreatorRegisterMap(): Map<string, KRRenderViewExportCreator> {
    const map: Map<string, KRRenderViewExportCreator> = new Map();
    map.set(KRMyView.VIEW_NAME, () => new KRMyView());
    map.set(ChatZoomImage.VIEW_NAME, () => new ChatZoomImage());
    return map;
  }

  getCustomRenderModuleCreatorRegisterMap(): Map<string, KRRenderModuleExportCreator> {
    const map: Map<string, KRRenderModuleExportCreator> = new Map();
    map.set(KRBridgeModule.MODULE_NAME, () => new KRBridgeModule());
    map.set(KRMyModule.MODULE_NAME, () => new KRMyModule())
    map.set(KRChatByteCacheModule.MODULE_NAME, () => new KRChatByteCacheModule())
    map.set(KRSseModule.MODULE_NAME, () => new KRSseModule())
    map.set(KRChatAttachmentPickerModule.MODULE_NAME, () => new KRChatAttachmentPickerModule())
    return map;
  }
}