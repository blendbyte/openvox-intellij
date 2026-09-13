# OpenVox and Puppet Language for IntelliJ Platform IDEs

Language support for [OpenVox](https://voxpupuli.org/openvox/) 8 and Puppet manifests
(`.pp`) and Embedded Puppet templates (`.epp`), for PhpStorm, IntelliJ IDEA, RubyMine,
GoLand and the rest of the IntelliJ Platform family.

## Why this exists

JetBrains' own Puppet plugin is marked deprecated on the Marketplace, which hides it from
in-IDE search, and its `until-build` is pinned to the current release, so it stops loading
the moment you move to an EAP. Its bundled knowledge of functions, facts and data types
also predates Puppet 4's type system. This plugin is a clean-room replacement built from
the OpenVox 8 reference implementation.

## What it does

**Editing**

- Syntax highlighting for manifests and EPP templates, including string interpolation, with a
  configurable colour scheme
- Brace matching, commenting, folding, live templates (`class`, `define`, `res`, `fn`, `ta`,
  `each`, `case`) and a structure view
- A formatter that aligns the `=>` of every attribute in a resource body, hash and selector,
  the way the style guide asks and nobody wants to do by hand, with its own code style page
  and two-space defaults

**Navigation**

- Go to the declaration of a class, defined type, function, type alias, parameter or variable
- Find usages, rename, Go to Class and Go to Symbol
- `Apache::Vhost['x']` finds the lower-case `apache::vhost` define; a parameter type finds its
  type alias
- Jump from `epp()`, `template()` and `source => 'puppet:///modules/...'` to the file named
- A gutter icon on a parameter that some Hiera data file sets, pointing at the file

**Completion and documentation**

- Resource attribute names and their allowed values, driven by the real type definitions, so
  `service { 'x': ensure => ` offers `running` and `stopped`
- Data types and project type aliases where a type is expected, resource types where a
  resource is being declared, fact keys inside `$facts[...]`
- Built-in functions with their signatures, project functions, classes and in-scope variables
- Quick documentation for all of the above

**Diagnostics**

- Unknown function, unknown resource attribute (a defined type's parameters are its
  attributes), unresolved variable, unused parameter
- Legacy top-scope facts such as `$::osfamily`, with a quick fix that rewrites them as
  `$facts['os']['family']`
- Optional `puppet parser validate` and `puppet-lint` integration, off unless the binary is
  found or configured

**Compatibility**

- Compatible from build 252 with no upper bound, so EAP builds keep working

## Where the knowledge comes from

`tools/generate-openvox-data.py` reads an OpenVox checkout and writes the tab-separated
resources under `src/main/resources/openvox/`: 104 built-in function signatures, 14 core
resource types with 160 attributes and their allowed values, the metaparameters, and 41 data
types. Only the Facter fact list is curated by hand. To refresh for a new release:

```sh
git clone --depth 1 --branch 8.x https://github.com/OpenVoxProject/openvox.git /tmp/openvox
python3 tools/generate-openvox-data.py /tmp/openvox
```

## Parser coverage

Verified against a corpus of 527 real manifests and templates (`puppetlabs-stdlib`,
`puppetlabs-apache`, `voxpupuli/puppet-nginx` and the OpenVox spec fixtures): 422 of 423 `.pp`
files and 103 of 104 `.epp` files parse without errors. The one failing manifest is an
intentionally invalid OpenVox test fixture.

### Known limitations

- Heredoc bodies are lexed but skipped by the parser, because the body begins after the next
  newline and can interrupt an unrelated construct. They are highlighted as strings;
  interpolation inside a heredoc body is not yet parsed.
- An EPP `case` whose options are split across several tags, with template text between them,
  is not parsed as a single case expression.
- A resource-style expression in an `if` condition (`if $x =~ Boolean { ... }`) relies on
  backtracking rather than the precedence trick the reference grammar uses.
- Incremental re-lexing is approximate inside heredocs. The platform restarts the lexer from a
  cached integer state, and the pending-heredoc tag and interpolation stack are not encoded in
  it, so editing above a long heredoc can briefly mis-colour its body.
- Puppetfile support and parameter info (Ctrl+P) are not implemented yet.

## Building

Requires JDK 21.

```sh
./gradlew build          # compile, generate the lexer and parser, run tests
./gradlew runIde         # launch a sandbox IDE with the plugin installed
./gradlew buildPlugin    # produce build/distributions/*.zip
./gradlew verifyPlugin   # run the JetBrains plugin verifier
```

Parse a corpus of your own manifests to find grammar gaps:

```sh
./gradlew test --tests '*CorpusTest*' \
  -Dopenvox.corpus=/path/to/modules:/path/to/control-repo
```

## Layout

| Path | What it is |
| --- | --- |
| `src/main/grammar/OpenVox.flex` | JFlex lexer, including heredoc and interpolation state |
| `src/main/grammar/OpenVox.bnf` | Grammar-Kit grammar; the lexer and PSI are generated from these two |
| `src/main/kotlin/.../lexer` | Lexer adapters for manifests and templates |
| `src/main/kotlin/.../parser` | Parser definitions and the EPP entry point |
| `src/main/kotlin/.../psi` | PSI element types, token sets and mixins |
| `src/main/kotlin/.../highlighting` | Syntax highlighter and colour settings page |

Generated sources land in `build/generated/sources/grammar` and are not committed.

## Roadmap

- Parameter info (Ctrl+P) for functions and resource attributes
- Puppetfile and r10k awareness, module dependency completion
- Heredoc PSI, so interpolation inside a heredoc body resolves
- Hiera lookups in both directions, including `hiera.yaml` hierarchy awareness
- A `RestartableLexer` so incremental re-lexing is exact inside heredocs

## Licence and attribution

Apache 2.0. See `NOTICE` for attribution: the grammar was written from the OpenVox
reference implementation, and no code from any other IDE plugin was used or decompiled.

Puppet is a trademark of Perforce Software, Inc. OpenVox is a project of Vox Pupuli. This
plugin is independent and not affiliated with either.
