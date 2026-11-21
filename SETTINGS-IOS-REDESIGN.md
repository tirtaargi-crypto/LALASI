# Settings redesign (iOS style)
- Settings home rewritten: large title, iOS search field, profile card (the way to Profile), inset grouped lists with tinted icon tiles. No About button and no developer info here; About stays only in Profile.
- Shared SettingsGroup/SettingsItem now render iOS inset-grouped lists (hairline dividers, grey section headers, 44dp rows) so every sub-menu changes at once.
- AppSwitch is the iOS green switch (51x31). SubSettingsScaffold: blue back chevron, 16dp margins, 40dp iOS search field.
- Cards in Storage, Modules, Integrations, Import, Audio Lab use 12dp radius and iOS greys.
- About sheet rewritten (icon, name, version in a grouped card). Developer/social section and "More about license" removed.
- Not compiled here: run `./gradlew :app:assembleArm64GmsRelease`.
