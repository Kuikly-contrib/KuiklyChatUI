import SwiftUI

@main
struct iOSApp: App {
	init() {
#if DEBUG
		print("[KuiklyChatImage] iOSApp.init registerDefaultHandler")
#endif
		KRChatImageExpandHandler.registerDefaultHandler()
	}

	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}