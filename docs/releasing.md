# Releasing to Modrinth and CurseForge

`gradlew publishMods` uploads the built JAR, with this version's section of `CHANGELOG.md` as its release notes, to both sites. It runs on [mod-publish-plugin](https://modmuss50.github.io/mod-publish-plugin/), configured at the end of `build.gradle`. Without both tokens it's a dry run: nothing is uploaded, and what would be is written to `build/publishMods/`.

Release Leyline before any mod that needs it: [Arcane Spells](https://github.com/CarsonKopec/ArcaneMod) lists it as a required mod by its slug, `leyline`, which must exist on both sites first.

Leyline's only requirement is Fabric API, which `build.gradle` lists for both sites.

## The first time

1. **Create the project** on each site, from the text in [listing.md](listing.md):

   | Field | Modrinth | CurseForge |
   | --- | --- | --- |
   | Name | Leyline | Leyline |
   | Slug | `leyline` (free on both as of 2026-11-01) | `leyline` |
   | Summary and description | from `listing.md` | from `listing.md` |
   | Categories | Library | API and Library |
   | Loader | Fabric | Fabric |
   | Environment | required on the client and the server | client and server |
   | Licence | CC0-1.0 | CC0 (or Custom, pasting `LICENSE`) |
   | Icon | `src/main/resources/assets/leyline/icon.png` (drawn by `python tools/generate_icon.py`) | the same |
   | Source and issues | https://github.com/CarsonKopec/Leyline, `/issues` | the same |

2. **Put the project IDs in `gradle.properties`:** `modrinth_project_id` (on the project page: the ⋮ menu, then Copy ID) and `curseforge_project_id` (the Project ID in the About Project panel). Commit them; they aren't secret.
3. **Make tokens.**
   - Modrinth: Settings, then Personal access tokens. Give it the Create versions, Read versions and Write versions scopes.
   - CurseForge: account settings, then API tokens.

   Keep them out of the repository. The same tokens work for Arcane Spells.

## Every release

1. Bump `version` in `gradle.properties`, and add a `## <version>` section at the top of `CHANGELOG.md`. Update the version in the README's Using it section.
2. Build: `gradlew.bat build`. Then publish to the local Maven repository with `gradlew.bat publishToMavenLocal`, and run Arcane Spells' game tests against it.
3. Dry-run the upload: `gradlew.bat publishMods` with no tokens set, then look in `build/publishMods/`.
4. Upload. Do one of these:
   - **From this machine:**
     ```powershell
     $env:MODRINTH_TOKEN = "..."; $env:CURSEFORGE_TOKEN = "..."; .\gradlew.bat build publishMods
     ```
   - **From GitHub:** add `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` as repository secrets (Settings, then Secrets and variables, then Actions). Then run the **release** workflow from the Actions tab.
5. Tag the release: `git tag v<version>`, then `git push origin v<version>`.

Modrinth reviews a new project before it goes public; later versions appear straight away. CurseForge reviews every file.
