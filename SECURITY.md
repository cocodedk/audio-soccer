# Security policy

## Report a problem privately

Please do not report a security problem in a public issue. Report it privately, in one of two ways:

- Open the [Security tab of this repository](https://github.com/cocodedk/audio-soccer/security) and choose **Report a
  vulnerability**. This is GitHub's private vulnerability reporting, and the report stays private.
- Or email [bb@cocode.dk](mailto:bb@cocode.dk).

Say what you found, how to repeat it, and which version or commit it affects.

## What is in scope

The Android app has no internet permission, so it cannot connect to the internet. The main risks are in how the app is
built, signed and published. Reports about any of these are in scope:

- the signing key and the release workflow, including any way to publish a changed `AudioSoccer.apk` under this
  project's name;
- the GitHub Actions workflows in `.github/workflows/`;
- the build's dependencies: Gradle, its plugins and the libraries the app uses;
- [the Audio Soccer website](https://soccer.cocode.dk/), which hosts the browser trial;
- the app's settings and records on the phone, if another app can read or change them.

The speech engine you install, such as eSpeak NG, is out of scope. Please report its problems to its own makers.

## Supported versions

No release exists yet. The first, 0.1.0, is being prepared. Once releases begin, security fixes go into a new release,
and older releases are not updated.
