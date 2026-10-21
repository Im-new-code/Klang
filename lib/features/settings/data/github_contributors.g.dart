// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'github_contributors.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint, type=warning
/// The repo's contributors from GitHub, bots left out. Emits the last cached
/// list (or the bundled one) straight away, then the fresh list once GitHub
/// answers, so the row never waits on the network.

@ProviderFor(githubContributors)
final githubContributorsProvider = GithubContributorsProvider._();

/// The repo's contributors from GitHub, bots left out. Emits the last cached
/// list (or the bundled one) straight away, then the fresh list once GitHub
/// answers, so the row never waits on the network.

final class GithubContributorsProvider
    extends
        $FunctionalProvider<
          AsyncValue<List<GithubContributor>>,
          List<GithubContributor>,
          Stream<List<GithubContributor>>
        >
    with
        $FutureModifier<List<GithubContributor>>,
        $StreamProvider<List<GithubContributor>> {
  /// The repo's contributors from GitHub, bots left out. Emits the last cached
  /// list (or the bundled one) straight away, then the fresh list once GitHub
  /// answers, so the row never waits on the network.
  GithubContributorsProvider._()
    : super(
        from: null,
        argument: null,
        retry: null,
        name: r'githubContributorsProvider',
        isAutoDispose: false,
        dependencies: null,
        $allTransitiveDependencies: null,
      );

  @override
  String debugGetCreateSourceHash() => _$githubContributorsHash();

  @$internal
  @override
  $StreamProviderElement<List<GithubContributor>> $createElement(
    $ProviderPointer pointer,
  ) => $StreamProviderElement(pointer);

  @override
  Stream<List<GithubContributor>> create(Ref ref) {
    return githubContributors(ref);
  }
}

String _$githubContributorsHash() =>
    r'6b70555810078d5206f6bb2384d962a1275efde8';
