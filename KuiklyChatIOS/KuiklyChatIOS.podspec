Pod::Spec.new do |spec|
  spec.name                     = 'KuiklyChatIOS'
  spec.version                  = '1.0.0'
  spec.homepage                 = 'https://git.woa.com/kuikly/KuiklyChatUI'
  spec.source                   = { :git => 'https://git.woa.com/kuikly/KuiklyChatUI.git' }
  spec.authors                  = 'Kuikly Team'
  spec.license                  = { :type => 'MIT' }
  spec.summary                  = 'KuiklyChat iOS native bridges and Compose Chat assets'
  spec.description              = <<-DESC
    KuiklyChatIOS provides persistent media caching, native attachment picking, image/SVG loading,
  and bundled Compose Chat SVG icons under Resources/common.
    Pair with the KuiklyChat KMP artifact and register the matching external Kotlin modules.
  DESC

  spec.ios.deployment_target    = '14.1'
  spec.static_framework         = true
  spec.source_files             = 'Sources/**/*.{h,m}'
  spec.public_header_files      = 'Sources/**/*.h'
  # Bundles as App.app/common/kuikly_chat_ic_*.svg for assets://common/... URLs.
  spec.resources                = ['Resources/common']
  spec.dependency 'OpenKuiklyIOSRender', '2.30.0'
  spec.dependency 'SDWebImage', '~> 5.0'
  spec.dependency 'SDWebImageSVGCoder', '~> 1.0'
  spec.frameworks               = 'Foundation', 'UIKit', 'PhotosUI', 'UniformTypeIdentifiers'
  spec.requires_arc             = true
  spec.pod_target_xcconfig = {
    'OTHER_LDFLAGS' => '$(inherited) -ObjC'
  }
end
