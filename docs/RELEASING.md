# Publishing 0.1.0

The library coordinate is `moe.tlaster.zenlessui:zenless-ui:0.1.0`. The `zenless-ui` module publishes Kotlin metadata, Android, desktop JVM, iOS ARM64, iOS simulator ARM64 and Wasm variants with sources and POM metadata. Publishing runs on an Apple Silicon Mac so native variants are built on a supported host.

1. Verify the `moe.tlaster.zenlessui` namespace (or an accepted parent namespace) in Sonatype Central Portal using the owner's account.
2. Create a Central publishing token and an ASCII-armored PGP signing key. Keep private material out of Git and chat messages.
3. Set GitHub environment `maven-central` secrets: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY`, `SIGNING_PASSWORD`.
4. Ensure **Build and verify** is green for the exact commit to publish. Run **Publish to Maven Central** manually. This signs and uploads the artifacts and requests release; Central coordinates are immutable once released.
5. Confirm the Central Portal release and resolve the published coordinate in a clean consumer project before announcing it.

To inspect the publication locally without uploading to Central:

```sh
./gradlew :zenless-ui:publishToMavenLocal
```

Run the aggregate task on the publishing Mac to inspect all native targets. On Windows or Linux, use `publishAndroidPublicationToMavenLocal`, `publishDesktopPublicationToMavenLocal` and `publishWasmJsPublicationToMavenLocal` on the `zenless-ui` module. Local publishing does not require Central credentials. Do not claim Central availability from task configuration or a Maven Local build alone.

Desktop binaries are unsigned. Android CI artifacts use debug signing. iOS simulator apps run in a simulator; Apple device distribution needs an Apple team, provisioning and distribution credentials configured by the owner. The repository does not contain private signing material.
