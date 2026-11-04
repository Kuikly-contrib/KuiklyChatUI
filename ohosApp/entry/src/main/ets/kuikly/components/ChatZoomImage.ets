import { KRAny, KuiklyRenderBaseView, KuiklyRenderCallback } from '@kuikly-open/render';
import { fileUri } from '@kit.CoreFileKit';
import { ComponentContent } from '@kit.ArkUI';

const SRC = 'src';
const CLOSE = 'close';
const MIN_SCALE = 1;
const MAX_SCALE = 4;

/**
 * Full-screen preview. Pinch zoom matches Android ChatZoomImageView:
 * scale around the pinch center, 1x–4x, pan only while zoomed.
 */
@Observed
export class ChatZoomImage extends KuiklyRenderBaseView {
  static readonly VIEW_NAME = 'ChatZoomImage';
  src: string = '';
  userScale: number = MIN_SCALE;
  transX: number = 0;
  transY: number = 0;
  contentWidth: number = 0;
  contentHeight: number = 0;
  viewWidth: number = 0;
  viewHeight: number = 0;
  pinchBase: number = MIN_SCALE;
  panBaseX: number = 0;
  panBaseY: number = 0;
  pinching: boolean = false;
  closeCallback: KuiklyRenderCallback | null = null;

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
    const uiContext = this.getUIContext() as UIContext;
    return new ComponentContent<KuiklyRenderBaseView>(uiContext, wrapBuilder<[KuiklyRenderBaseView]>(createChatZoomImage), this);
  }

  setProp(propKey: string, propValue: KRAny | KuiklyRenderCallback): boolean {
    switch (propKey) {
      case SRC:
        this.resetTransform();
        this.src = `${propValue ?? ''}`;
        return true;
      case CLOSE:
        this.closeCallback = propValue as KuiklyRenderCallback;
        return true;
      default:
        return super.setProp(propKey, propValue);
    }
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    return null;
  }

  resetTransform(): void {
    this.userScale = MIN_SCALE;
    this.transX = 0;
    this.transY = 0;
  }

  applyScale(next: number, focusX: number, focusY: number): void {
    const old = this.userScale;
    const scale = Math.min(MAX_SCALE, Math.max(MIN_SCALE, next));
    if (old > 0 && this.viewWidth > 0 && this.viewHeight > 0) {
      const localX = (focusX - this.viewWidth / 2 - this.transX) / old;
      const localY = (focusY - this.viewHeight / 2 - this.transY) / old;
      this.userScale = scale;
      this.transX = focusX - this.viewWidth / 2 - localX * scale;
      this.transY = focusY - this.viewHeight / 2 - localY * scale;
    } else {
      this.userScale = scale;
    }
    if (this.userScale <= 1.01) {
      this.resetTransform();
    } else {
      this.clamp();
    }
  }

  clamp(): void {
    const drawnW = this.contentWidth * this.userScale;
    const drawnH = this.contentHeight * this.userScale;
    const maxX = Math.max(0, (drawnW - this.viewWidth) / 2);
    const maxY = Math.max(0, (drawnH - this.viewHeight) / 2);
    this.transX = Math.min(maxX, Math.max(-maxX, this.transX));
    this.transY = Math.min(maxY, Math.max(-maxY, this.transY));
  }

  closeIfResting(): void {
    if (this.userScale <= 1.01) {
      this.closeCallback?.({});
    }
  }
}

@Component
struct ChatZoomImageComponent {
  @ObjectLink host: ChatZoomImage;

  build() {
    Stack() {
      Image(zoomImageSource(this.host.src))
        .width('100%')
        .height('100%')
        .objectFit(ImageFit.Contain)
        .scale({ x: this.host.userScale, y: this.host.userScale })
        .translate({ x: this.host.transX, y: this.host.transY })
        .onComplete((event) => {
          if (event !== undefined && event.loadingStatus === 0) {
            this.host.contentWidth = event.contentWidth > 0 ? event.contentWidth : event.width;
            this.host.contentHeight = event.contentHeight > 0 ? event.contentHeight : event.height;
          }
        })
        .onAreaChange((_old, area) => {
          this.host.viewWidth = Number(area.width);
          this.host.viewHeight = Number(area.height);
        })
    }
    .width('100%')
    .height('100%')
    .clip(true)
    .gesture(
      GestureGroup(GestureMode.Parallel,
        PinchGesture({ fingers: 2 })
          .onActionStart(() => {
            this.host.pinching = true;
            this.host.pinchBase = this.host.userScale;
          })
          .onActionUpdate((event: GestureEvent) => {
            this.host.applyScale(this.host.pinchBase * event.scale, event.pinchCenterX, event.pinchCenterY);
          })
          .onActionEnd(() => {
            this.host.pinching = false;
            if (this.host.userScale <= 1.01) {
              this.host.resetTransform();
            }
          }),
        PanGesture({ fingers: 1 })
          .onActionStart(() => {
            this.host.panBaseX = this.host.transX;
            this.host.panBaseY = this.host.transY;
          })
          .onActionUpdate((event: GestureEvent) => {
            if (this.host.pinching || this.host.userScale <= 1.01) {
              return;
            }
            this.host.transX = this.host.panBaseX + event.offsetX;
            this.host.transY = this.host.panBaseY + event.offsetY;
            this.host.clamp();
          }),
        TapGesture()
          .onAction(() => {
            this.host.closeIfResting();
          })
      )
    )
  }
}

function zoomImageSource(src: string): string {
  if (src.startsWith('/')) {
    return fileUri.getUriFromPath(src);
  }
  return src;
}

@Builder
function createChatZoomImage(view: KuiklyRenderBaseView) {
  ChatZoomImageComponent({ host: view as ChatZoomImage })
}
