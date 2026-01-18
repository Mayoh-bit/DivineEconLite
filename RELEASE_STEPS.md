# DivineEconLite Release Steps

This repo includes a GitHub Actions workflow that builds the plugin with Java 17 and publishes a GitHub Release with the built jar attached.

## Option A: Manual release via workflow_dispatch

1. Push this workflow file to your default branch.
2. Go to **Actions** tab.
3. Select **Build and Release**.
4. Click **Run workflow**.
5. Enter a tag such as `v1.0.0`.
6. Keep `draft=true` for safety.
7. Run.

When the workflow completes, a draft release will be created and `out/*.jar` will be attached.

## Option B: Tag push release

1. Create a tag locally:

   git tag v1.0.0
   git push origin v1.0.0

2. The workflow will run on tag push automatically.

## If you still hit 403 from Maven Central

The workflow writes a local Maven `settings.xml` that mirrors `central` to `https://repo1.maven.org/maven2/`. You can replace this URL with any mirror that is reachable from your network.

## Notes

- This workflow uses `contents: write` permission to create releases.
- It uses `softprops/action-gh-release` to create and upload assets.
