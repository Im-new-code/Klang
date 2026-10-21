// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'playlist_artwork.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint, type=warning

@ProviderFor(playlistArtwork)
final playlistArtworkProvider = PlaylistArtworkFamily._();

final class PlaylistArtworkProvider
    extends
        $FunctionalProvider<
          AsyncValue<List<String?>>,
          List<String?>,
          Stream<List<String?>>
        >
    with $FutureModifier<List<String?>>, $StreamProvider<List<String?>> {
  PlaylistArtworkProvider._({
    required PlaylistArtworkFamily super.from,
    required List<String> super.argument,
  }) : super(
         retry: null,
         name: r'playlistArtworkProvider',
         isAutoDispose: true,
         dependencies: null,
         $allTransitiveDependencies: null,
       );

  @override
  String debugGetCreateSourceHash() => _$playlistArtworkHash();

  @override
  String toString() {
    return r'playlistArtworkProvider'
        ''
        '($argument)';
  }

  @$internal
  @override
  $StreamProviderElement<List<String?>> $createElement(
    $ProviderPointer pointer,
  ) => $StreamProviderElement(pointer);

  @override
  Stream<List<String?>> create(Ref ref) {
    final argument = this.argument as List<String>;
    return playlistArtwork(ref, argument);
  }

  @override
  bool operator ==(Object other) {
    return other is PlaylistArtworkProvider && other.argument == argument;
  }

  @override
  int get hashCode {
    return argument.hashCode;
  }
}

String _$playlistArtworkHash() => r'e14d51c8bb5938b25fbc111b7ce38d0e728b08c2';

final class PlaylistArtworkFamily extends $Family
    with $FunctionalFamilyOverride<Stream<List<String?>>, List<String>> {
  PlaylistArtworkFamily._()
    : super(
        retry: null,
        name: r'playlistArtworkProvider',
        dependencies: null,
        $allTransitiveDependencies: null,
        isAutoDispose: true,
      );

  PlaylistArtworkProvider call(List<String> songPaths) =>
      PlaylistArtworkProvider._(argument: songPaths, from: this);

  @override
  String toString() => r'playlistArtworkProvider';
}
