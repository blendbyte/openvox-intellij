# OpenVox Language

Language support for **OpenVox 8 and 9** and Puppet manifests (`.pp`) and Embedded Puppet templates (`.epp`) in IntelliJ Platform IDEs, including IntelliJ IDEA, PhpStorm, RubyMine, and GoLand. Requires IDE build **252 (2025.2) or newer**. OpenVox 9 support is provisional, checked against **9.0.0-rc1**; see [version compatibility](docs/compatibility.md). CI checks recommended stable and EAP IDE releases.

## Features

- Syntax highlighting, brace matching, folding, live templates, and a manifest structure view.
- Manifest formatting with two-space indentation and configurable alignment of resource, hash, and selector arrows.
- Declaration navigation, find usages, and rename for classes, defined types, functions, type aliases, parameters, and local variables.
- Completion for resource attributes and values, data types, facts, project declarations, and variables in scope, plus quick documentation.
- Inspections for unknown functions and attributes, unresolved variables, unused parameters, and legacy facts, with a structured-fact quick fix.
- Navigation to module templates and files, and Hiera gutter links for matching parameter keys in `data/*.yaml` and `data/*.yml`, including subdirectories.

Optional `puppet parser validate` and `puppet-lint` checks run when their executables are available on PATH. Configure paths or disable the checks under **Settings → Tools → OpenVox / Puppet**. Point validation at your target OpenVox installation; the editor supports both versions without a language switch.

If you installed an early build named **OpenVox and Puppet Language** (`net.blendbyte.openvox`), uninstall it and restart the IDE before installing **OpenVox Language** (`net.blendbyte.openvox-intellij`). The plugin ID changed, so the IDE treats these as separate plugins.

## Build and test

Requires JDK 21. Use the included Gradle wrapper:

```sh
./gradlew build             # compile, test, and package
./gradlew runIde            # launch a sandbox IDE
./gradlew verifyPlugin      # check IDE binary compatibility
python3 -m unittest discover -s tools -p 'test_*.py'
```

Releases are manual: run the checks above and upload the ZIP from `build/distributions/` to JetBrains Marketplace. GitHub Actions builds and verifies but does not publish.

To test an external manifest/template corpus:

```sh
./gradlew test --tests '*CorpusTest*' -Dopenvox.corpus=/path/to/modules:/path/to/control-repo
```

Use your operating system's path separator between corpus roots. Corpus tests are skipped when no roots are supplied.

## Limitations

- Heredoc bodies are highlighted, but references inside them are not parsed.
- Hiera links match qualified keys in data files; they do not evaluate `hiera.yaml` hierarchies.
- Puppetfile support and parameter information popups are not implemented.

## Language data

The shared grammar and built-in data use OpenVox 8 as the baseline, checked against OpenVox 9 RC1, with version differences noted in quick documentation. Fact paths are curated. To refresh generated data:

```sh
git clone --depth 1 --branch 8.x https://github.com/OpenVoxProject/openvox.git /tmp/openvox
python3 tools/generate-openvox-data.py /tmp/openvox
```

Grammar sources are in `src/main/grammar`, Kotlin implementation in `src/main/kotlin`, and bundled data in `src/main/resources/openvox`. Generated lexer and parser code stays under `build/`.

## License

Apache 2.0; see [LICENSE](LICENSE) and [NOTICE](NOTICE) for attribution. This community plugin is independent of Perforce and Vox Pupuli. Puppet is a Perforce trademark; OpenVox is a Vox Pupuli project.
