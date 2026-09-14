# OpenVox 8 and 9 compatibility

The plugin supports both versions with one parser and shared completion data. OpenVox 9 support is provisional through [9.0.0-rc1, released September 4, 2026](https://github.com/OpenVoxProject/openvox/releases/tag/9.0.0-rc1). Recheck the final release before claiming stable 9 support.

## What was compared

Source review on September 14, 2026 compared OpenVox's `8.x` branch at `0ae463dd1f482ba2f73b6a72cf32f4dd946359a6` with RC1 at `78bd4057490c110b89cc7811a86d04db7738e370`.

- `lib/puppet/pops/parser/egrammar.ra` is byte-identical. Lexer changes are comment placement; parser support changes are Ruby refactoring, with no new manifest or EPP syntax.
- The generator finds the same 104 function names, 14 resource types, 173 attribute rows, 11 metaparameters, and 42 data types. These counts describe the plugin's bundled inventory, not every resource shipped by optional modules.
- The only extracted function signature change is removal of `regsubst()`'s fifth argument. Resource attribute names, kinds, and enumerated values match.
- `puppet parser validate` and `puppet epp validate` application sources are unchanged between these revisions.

These findings support sharing the editor implementation. They do not imply that a catalog behaves identically on both runtimes.

Validation passed: 78 JVM tests, four generator tests, and all 26 manifests plus two EPP templates from the two revisions' `examples/` and `benchmarks/evaluations/manifests/` directories. This checks the plugin against upstream source fixtures; it is not a live agent or server integration test.

## Changes that affect manifests and modules

| OpenVox 9 change | Effect on compatibility |
| --- | --- |
| `regsubst()` drops the ignored fifth `encoding` argument | Omit it for code shared with 8. Completion presents the shared signature first; quick documentation retains and explains the 8-only overload. [Source](https://github.com/OpenVoxProject/openvox/pull/391). |
| Checksum-looking `file.content` strings become literal content | A manifest relying on implicit filebucket retrieval needs changing. The attribute's quick documentation notes the distinction. [Source](https://github.com/OpenVoxProject/openvox/pull/170). |
| `preprocess_deferred` defaults to `true` again | Deferred calls may run before resources are applied. Review calls that rely on resources being installed first. [Source](https://github.com/OpenVoxProject/openvox/pull/462). |
| The legacy Hiera indirector and data-binding settings are removed | Review old data providers and use Hiera 5 configuration. The deprecated `hiera*()` functions still exist in RC1; `lookup()` remains available. Hiera gutter navigation is unaffected. [Indirector removal](https://github.com/OpenVoxProject/openvox/pull/384), [settings removal](https://github.com/OpenVoxProject/openvox/pull/385). |
| OpenFact 6 is required | Custom Ruby facts need review for removed `ldapname` accessors and execution API deprecations. Structured fact completion remains a curated suggestion list, not a target-machine schema. [OpenFact 6 notes](https://docs.openvoxproject.org/openfact/6.x/release_notes.html). |
| `pe_serverversion` is removed and `zone_core` is no longer bundled | Review references to the old fact and explicitly manage modules you depend on. [Fact removal](https://github.com/OpenVoxProject/openvox/pull/397), [module removal](https://github.com/OpenVoxProject/openvox/pull/592). |

Other deployment changes include Ruby 3.2 minimum, opt-in report processing (`reports = none`), removal of `pluginsync`, `configprint` and legacy PAL script evaluation APIs, explicit server configuration for privileged agent runs, and removal of the systemd provider's Debian SysV fallback. See the [RC1 changelog](https://github.com/OpenVoxProject/openvox/blob/9.0.0-rc1/CHANGELOG.md) for the full upgrade list. OpenVox Server has separate Java, JRuby and service-management changes in its [9.x release notes](https://docs.openvoxproject.org/openvox-server/9.x/release_notes.html).

## Validation and release policy

The IDE does not infer an OpenVox version or reject version-specific function arity. Configure the appropriate executable under **Settings → Tools → OpenVox / Puppet** for external syntax validation. Compile and test catalogs with each target runtime to check function arguments, module dependencies and runtime behavior; `parser validate` alone does not cover those checks.

Before updating the supported 9 prerelease or declaring stable support, compare the parser and generated inventory again, run the plugin tests and corpus checks, and update this record. Keep 8 as the generated-data baseline so 8-only signatures remain documented.

GitHub Actions runs build and compatibility checks only. Build and verify locally, then upload `build/distributions/*.zip` to JetBrains Marketplace manually. There is no release or publishing workflow.
