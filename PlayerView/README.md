# PlayerView

A client-side **Forge 1.8.9** mod that shows any player's Hypixel **BedWars** stats in-game
with `/pv`. Renders a full stats GUI (skin + three-column layout) and an optional compact
chat card. Core stats come from the **Hypixel API**; cosmetics/extra counters come from
**api.sukie.net**.

## Commands

| Command | What it does |
|---|---|
| `/pv` | Opens the GUI for **your own** stats |
| `/pv <player>` | Opens the full stats GUI for a player |
| `/pv <player> -c` | Prints the compact chat card |
| `/pv key <hypixelKey>` | Saves your Hypixel API key |
| `/pv sukie <sukieKey>` | Saves your sukie.net API key |

(The `-c` flag works in any position, e.g. `/pv -c` cards your own stats.)

In the GUI, the **Overall** / **Core Modes** buttons switch between overall BedWars stats
and the sum of the four core modes (solo/doubles/3v3v3v3/4v4v4v4). Press `Esc` to close.

## Setup

You need a Hypixel API key from <https://developer.hypixel.net> (the old in-game
`/api new` is deprecated). Then in game:

```
/pv key YOUR_HYPIXEL_KEY
/pv sukie YOUR_SUKIE_KEY        (optional, for cosmetics)
```

Keys are stored in `config/playerview.cfg`. **Keep that file private** — it holds your keys.

```properties
hypixelApiKey=
sukieApiKey=
sukieAuthHeader=Authorization   # header the sukie key is sent in
sukieAuthPrefix=                # e.g. "Bearer " if the API needs it
```

## Building

ForgeGradle 2 (the 1.8.9 toolchain) needs **JDK 8** and **Gradle 4.10.x** — it does *not*
work with modern Gradle 7/8 or newer JDKs.

**Easiest, most reliable path — drop into the official MDK:**

1. Download the **Forge 1.8.9 MDK** (`forge-1.8.9-11.15.1.2318-1.8.9-mdk.zip`) from
   <https://files.minecraftforge.net/net/minecraftforge/forge/index_1.8.9.html> and unzip it.
   The MDK already contains a working `gradlew` + wrapper jar.
2. Copy this project's `src/` folder and `build.gradle` over the MDK's (replacing them).
3. With JDK 8 active:
   ```bash
   ./gradlew setupDecompWorkspace
   ./gradlew build
   ```
4. The mod jar appears at `build/libs/PlayerView-1.0.0.jar`. Drop it in `.minecraft/mods`
   (1.8.9 Forge profile).

**If you already have a 1.8.9 dev setup:** just run the two `gradlew` commands above from
this folder. (If `./gradlew` is missing the wrapper jar, run `gradle wrapper --gradle-version
4.10.3` once with a local Gradle 4.10.3 to generate it.)

To develop in an IDE: `./gradlew setupDecompWorkspace`, then import as a Gradle project
(IntelliJ) or run `./gradlew eclipse`.

## The sukie.net cosmetics piece (now self-resolving)

`api.sukie.net` is private and undocumented, and every query-string auth style returns
**401**, so the key must go in a request header — but there's no public spec saying which
one. Rather than hard-code a guess, `SukieApi` **auto-detects** the right setup on first use:

- it probes common auth headers (`Authorization: Bearer`, `Authorization`, `x-api-key`,
  `X-API-Key`, `api-key`, `apikey`, `key`) until one gets past 401/403,
- then probes how the API addresses a player (`?player=`, `?uuid=`, `?name=`, dashed uuid,
  or path-style `/cosmetics/<uuid>`),
- **caches** the working combination in `playerview.cfg` (`sukieAuthHeader`,
  `sukieAuthPrefix`, `sukieQuery`, `sukieResolved=true`) so every later call is a single
  request,
- re-probes automatically if the cached combo ever stops authenticating (e.g. new key),
- and parses the response **schema-agnostically**: it flattens whatever JSON comes back
  into label/value rows shown in the GUI's "Cosmetics" section and best-effort extracts a
  "final kills" and "beds destroyed" counter.

So it works end-to-end on your real key without any manual setup. If you later paste a
real sample response, the only thing worth tightening is `SukieApi.map(...)` — to give the
fields nicer labels / typed getters on `Cosmetics`. While probing, any auth failure shows
a small grey diagnostic line at the bottom of the GUI (e.g. `cosmetics: sukie auth failed
- no known header accepted the key`) so you can see exactly what happened.

## Project layout

```
dev.pv
├─ PlayerView              mod entry point
├─ command/PvCommand       the /pv command (+ threading, tab-complete)
├─ config/PvConfig         flat-file config (keys)
├─ api/
│  ├─ HttpUtil             tiny HTTP client
│  ├─ MojangApi            name → uuid, skin url
│  ├─ HypixelApi           player fetch + parse
│  └─ SukieApi             cosmetics (configurable, defensive)
├─ stats/
│  ├─ BedwarsStats         data holder + Overall/Cores math
│  ├─ BedwarsLevel         star/prestige from Experience
│  ├─ NetworkLevel         network level from networkExp
│  └─ Cosmetics            sukie data holder
├─ gui/
│  ├─ GuiPlayerView        the full stats screen
│  └─ SkinRenderer         2D front-body skin draw
└─ util/
   ├─ Format               number formatting
   ├─ Prestige             star colours
   └─ ChatCard             compact card output
```

## Notes / things you may want to tweak

- **Player render is now true 3D.** The GUI builds a real client-side player entity from
  the target's signed GameProfile and draws it with the same routine the vanilla inventory
  uses, so it's a full rotating model that gently follows your cursor. If the world isn't
  loaded or the entity can't be built for any reason, it silently falls back to the
  self-contained 2D body renderer. Tweak the size via the `scale` arg (currently `42`) in
  `GuiPlayerView.drawSkin(...)`.
- Some labels in your screenshot (Tokens, Tickets, Clutch Rate, Skill Index) are not real
  Hypixel fields — they look like a proprietary overlay's derived metrics. I substituted
  real, accurate fields (Coins, Games, Win Rate, Winstreak). Say the word if you want me to
  compute approximations of those instead.
- The `Provider:` line is hardcoded to "Hypixel" since that's the real source.
- High-prestige (1000+) star colours are simplified to a single colour rather than the
  animated rainbow.
