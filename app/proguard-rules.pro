# Nothing to keep beyond the defaults: the app has no reflection-based
# serialisation surface that R8 cannot see, since kotlinx.serialization
# generates its own serialisers at compile time.

# The install-result receiver is built by name from the manifest. Keeping its
# constructor explicitly rather than trusting a manifest rule to cover members
# — that assumption is exactly what silently broke the MoonWidget panel, where
# R8 kept the class and dropped the constructor Glance needed to reflect on.
-keep class dev.mahourigan.fooddiary.update.InstallResultReceiver { <init>(); }
