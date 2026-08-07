Pod::Spec.new do |spec|
  spec.name                     = 'KuiklyChatIOS'
  spec.version                  = '1.0.0'
  spec.homepage                 = 'https://git.woa.com/kuikly/KuiklyChatUI'
  spec.source                   = { :git => 'https://git.woa.com/kuikly/KuiklyChatUI.git' }
  spec.authors                  = 'Kuikly Team'
  spec.license                  = { :type => 'MIT' }
  spec.summary                  = 'KuiklyChat iOS native bridge for chat media byte cache'
  spec.description              = <<-DESC
    KuiklyChatIOS provides KRChatByteCacheModule for persistent chat media caching on iOS.
    Pair with the KuiklyChat KMP artifact and register chatByteCacheExternalModules() on the Kotlin side.
  DESC

  spec.ios.deployment_target    = '14.1'
  spec.source_files             = 'KuiklyChatIOS/**/*.{h,m}'
  spec.public_header_files      = 'KuiklyChatIOS/**/*.h'
  # Compose Chat 内置 SVG 图标（paperclip / file 等），打包进主 App bundle 的 common/ 目录
  spec.resources                = ['KuiklyChat/src/commonMain/assets/**']
  spec.dependency 'OpenKuiklyIOSRender', '~> 2.17.0'
  spec.dependency 'SDWebImage', '~> 5.0'
  spec.dependency 'SDWebImageSVGCoder', '~> 1.0'
  spec.frameworks               = 'Foundation'
  spec.requires_arc             = true
end
