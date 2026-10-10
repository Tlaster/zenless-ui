# Publishing to Maven Central and snapshots

The library coordinate is `moe.tlaster.zenlessui:zenless-ui:0.1.0`. The `zenless-ui` module publishes Kotlin metadata, Android, desktop JVM, iOS ARM64, iOS simulator ARM64 and Wasm variants with sources and POM metadata. Publishing runs on an Apple Silicon Mac so native variants are built on a supported host.

1. Verify the `moe.tlaster.zenlessui` namespace (or an accepted parent namespace) in Sonatype Central Portal using the owner's account.
2. Create a Central publishing token and an ASCII-armored PGP signing key. Keep private material out of Git and chat messages.
3. Set GitHub repository Actions secrets: `OSSRH_USERNAME`, `OSSRH_PASSWORD`, `SIGNING_KEY`, `SIGNING_KEY_ID`, `SIGNING_PASSWORD`. The `OSSRH_*` names follow [mfm-multiplatform](https://github.com/Tlaster/mfm-multiplatform/blob/master/.github/workflows/ci.yml), but their values must be a **Central Portal user token**, not legacy OSSRH credentials. `SIGNING_KEY` is the complete ASCII-armored private key; `SIGNING_KEY_ID` is its 8-character key ID (needed when selecting a signing subkey), and `SIGNING_PASSWORD` is its passphrase. No GitHub environment is required.
4. Enable **SNAPSHOTs** for the namespace in [Central Portal](https://central.sonatype.org/publish/publish-portal-snapshots/).
5. Ensure **Build and verify** is green for the exact commit to release, then use **Publish Maven Central and snapshots** as described below. The workflow runs desktop and iOS simulator library tests before signing and uploading all targets. Central release coordinates are immutable once released.
6. Confirm the Central Portal release and resolve the published coordinate in a clean consumer project before announcing it.

## CI triggers and versions

`VERSION_NAME` in `gradle.properties` is the shared library version and must end in `-SNAPSHOT` on `main` (initially `0.1.0-SNAPSHOT`). The publication coordinates use that same value. CI and local commands override it with `-PVERSION_NAME=...`.

| Trigger | Version | Destination |
| --- | --- | --- |
| Push to `main` | `VERSION_NAME` from `gradle.properties` | Central Portal snapshots |
| Push a `v*` tag, e.g. `v0.1.0` | Tag without `v`, e.g. `0.1.0` | Maven Central, automatically released |
| Manual workflow run | Optional `version` input, otherwise `VERSION_NAME`; a tag always takes precedence | Snapshots if the version ends in `-SNAPSHOT`, otherwise Maven Central |

Versions must have three numeric components, optionally followed by a suffix such as `-rc.1` or `-SNAPSHOT`. Snapshot tags and non-snapshot versions on a branch push are rejected. PRs and forks do not publish. Publication jobs are serialized without cancelling a running upload.

For the first release, push `v0.1.0` on the tested commit. Afterwards, bump `VERSION_NAME` to `0.1.1-SNAPSHOT` for the next development cycle. Gallery installer and Android app versions remain separately configured in their module build files.

Snapshots use the [plugin's snapshot publishing support](https://vanniktech.github.io/gradle-maven-publish-plugin/central/#publishing-snapshots). Consumers must add the snapshot repository alongside `mavenCentral()` in their dependency repositories:

```kotlin
maven("https://central.sonatype.com/repository/maven-snapshots/") {
    mavenContent { snapshotsOnly() }
}
```

Then use `implementation("moe.tlaster.zenlessui:zenless-ui:0.1.0-SNAPSHOT")`. A successful snapshot upload does not imply that `0.1.0` has been released to Maven Central.

## Local inspection

Check workflow version selection, destination routing and missing-credential handling without uploading:

```sh
python3 tools/check_publish_workflow.py
```

To inspect the publication locally without uploading to Central:

```sh
./gradlew :zenless-ui:publishToMavenLocal
```

Run the aggregate task on the publishing Mac to inspect all native targets. On Windows or Linux, use `publishAndroidPublicationToMavenLocal`, `publishDesktopPublicationToMavenLocal` and `publishWasmJsPublicationToMavenLocal` on the `zenless-ui` module. Local publishing does not require Central credentials. Do not claim Central availability from task configuration or a Maven Local build alone.

Desktop binaries are unsigned. Android CI artifacts use debug signing. iOS simulator apps run in a simulator; Apple device distribution needs an Apple team, provisioning and distribution credentials configured by the owner. The repository does not contain private signing material.
