# 🤖 AI Idler

Addictive clicker game about training AI models. Tap to generate GPU compute, buy upgrades, and reach the next breakthrough.

## Visual Features

- **Canvas-based GPU farm visualization** — grows with each upgrade
- **Particle system** — 12 energy particles on every tap
- **Breakthrough events** — screen flash, haptic double-pulse, epic sound
- **Animated background** — pulsing tech grid, light effects
- **Dark theme** — Editorial aesthetic (inspired by Novagram / Singularity)

## Audio & Haptics

- **Tap sound** — crisp 200ms beep
- **Upgrade sound** — ascending tones
- **Breakthrough sound** — deep bass + high freq
- **Haptic feedback** — 20ms tap pulse, breakthrough double-pulse

## Gameplay

**Core Loop:**
1. Tap screen to generate GPU (1 GPU per tap, scaled by multiplier)
2. Buy upgrades: GPU Farm, Worker Bot, Processing Core, AI Booster
3. Worker Bot grants offline progression (10 GPU/sec per worker)
4. Reach level thresholds for breakthrough events (10% multiplier boost)

**Economy:**
- Workers enable 2-hour offline progression (no stagnation longer)
- Upgrade costs scale by 1.15x per purchase
- Multiplier increases with upgrades and breakthroughs

## Build & Deploy

### One-command Setup (macOS/Linux):

```bash
bash setup.sh
# Enter GitHub username, repo name, and PAT token
```

This will:
1. Create local git repo
2. Push initial commit
3. Create GitHub repo (via `gh` CLI or manual)
4. Tag v1.0 and push → triggers GitHub Actions

### Manual Build:

```bash
./gradlew assembleRelease
# APK appears in app/build/outputs/apk/release/
```

### Auto-Release via GitHub Actions:

Push a tag to trigger build:
```bash
git tag v1.1 && git push --tags
# GitHub Actions builds and publishes APK to Releases
```

## Tech Stack

- **Kotlin 2.0.21** + Jetpack Compose
- **Canvas rendering** for GPU farm + particles
- **StateFlow** for reactive game state
- **SoundPool** for audio (lightweight)
- **Vibrator API** for haptics
- **Gradle** with GitHub Actions

## File Structure

```
ai-idler/
├── app/
│   ├── src/main/kotlin/com/secondream/aiidler/
│   │   ├── MainActivity.kt          (entry point)
│   │   ├── GameScreen.kt            (UI + Canvas)
│   │   ├── GameLogic.kt             (economy)
│   │   ├── AudioManager.kt          (sounds)
│   │   └── HapticManager.kt         (vibrations)
│   ├── src/main/AndroidManifest.xml
│   └── build.gradle.kts
├── .github/workflows/release.yml     (auto-build on tag)
└── build.gradle.kts
```

## Next Steps

- Add proper sound files (res/raw/) with Kotlin OpenSL++ or ExoPlayer
- Implement SaveGame (JSON serialization to SharedPreferences)
- Add new upgrade trees (Neural Net, Quantum, etc.)
- Particle effects library (shockrings, shimmer)
- IAP for cosmetic skins / worker pack
- Leaderboard (Firebase or custom backend)

---

Made with 🖤 by Second Dream Studio
