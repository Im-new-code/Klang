// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'playback_notifier.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint, type=warning

@ProviderFor(Playback)
final playbackProvider = PlaybackProvider._();

final class PlaybackProvider
    extends $NotifierProvider<Playback, PlaybackState> {
  PlaybackProvider._()
    : super(
        from: null,
        argument: null,
        retry: null,
        name: r'playbackProvider',
        isAutoDispose: false,
        dependencies: null,
        $allTransitiveDependencies: null,
      );

  @override
  String debugGetCreateSourceHash() => _$playbackHash();

  @$internal
  @override
  Playback create() => Playback();

  /// {@macro riverpod.override_with_value}
  Override overrideWithValue(PlaybackState value) {
    return $ProviderOverride(
      origin: this,
      providerOverride: $SyncValueProvider<PlaybackState>(value),
    );
  }
}

String _$playbackHash() => r'8b0d0eea371c19e8e5ae7e6c4c037c8fa84d25a2';

abstract class _$Playback extends $Notifier<PlaybackState> {
  PlaybackState build();
  @$mustCallSuper
  @override
  void runBuild() {
    final ref = this.ref as $Ref<PlaybackState, PlaybackState>;
    final element =
        ref.element
            as $ClassProviderElement<
              AnyNotifier<PlaybackState, PlaybackState>,
              PlaybackState,
              Object?,
              Object?
            >;
    element.handleCreate(ref, build);
  }
}
