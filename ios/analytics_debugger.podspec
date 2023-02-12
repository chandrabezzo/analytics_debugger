#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint analytics_debugger.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'analytics_debugger'
  s.version          = '1.0.0'
  s.summary          = 'Tools for analytics debugger.'
  s.description      = <<-DESC
A new Flutter plugin project.
                       DESC
  s.homepage         = 'https://github.com/chandrabezzo/analytics_debugger'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Solusi Bejo' => 'chandrashibezzo@gmail.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.dependency 'IosAnalyticsDebugger'
  s.platform = :ios, '9.0'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'
end
