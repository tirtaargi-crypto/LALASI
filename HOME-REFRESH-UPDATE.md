# Home Refresh Update

- Added a refresh control to the Home top bar.
- Tapping it calls the existing `HomeViewModel.refresh()` flow, which reloads Home data and triggers the existing automatic sync without requiring sign-out or app restart.
- The control is disabled while a refresh is running and shows an inline progress indicator until the refresh completes.

Build verification: `:app:compileDebugKotlin` could not be completed in this environment because the Gradle distribution could not be downloaded (network/DNS unavailable).
