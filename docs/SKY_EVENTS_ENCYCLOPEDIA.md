# Sky: Children of the Light — Master Event & Timing Encyclopedia

> **Source:** Extracted, verified, and cross-referenced from [SkyGame-Data](https://github.com/Silverfeelin/SkyGame-Data), [SkyGame-Planner](https://github.com/Silverfeelin/SkyGame-Planner), and Thatgamecompany official specifications.
> **Timezone Standard:** All in-game clocks, resets, and timestamps operate on **Sky Standard Time: `America/Los_Angeles` (PST / PDT)**.

---

## 1. Daily Timed Wax Cycles (Every Single Day, Year-Round)

These events occur at recurring hourly intervals around the clock.

| Event Name | Exact Start Time (Sky Time) | Active Duration | Realm & Location | Wax / Light Yield | Mechanics & Notes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Polluted Geyser** | **Every Even Hour :05**<br>`00:05, 02:05, 04:05, 06:05, 08:05, 10:05, 12:05, 14:05, 16:05, 18:05, 20:05, 22:05` | 10 mins<br>*(ends at :15)* | **Daylight Prairie**<br>Sanctuary Islands | **~1,000 Light**<br>*(pool soft cap)* | Geyser erupts with dark urchins every minute for 10 minutes. Burn urchins to release light droplets. |
| **Grandma's Dinner**<br>*(Belonging Ancestor)* | **Every Even Hour :35**<br>`00:35, 02:35, 04:35, 06:35, 08:35, 10:35, 12:35, 14:35, 16:35, 18:35, 20:35, 22:35` | 10 mins<br>*(ends at :45)* | **Hidden Forest**<br>Elevated Clearing (Treehouse) | **~1,000 Light**<br>*(pool soft cap)* | Grandma serves roasted buns on the stone dining table. Burn buns or melt with white candle to release wax. |
| **Sunset Sanctuary Turtle** | **Every Even Hour :50**<br>`00:50, 02:50, 04:50, 06:50, 08:50, 10:50, 12:50, 14:50, 16:50, 18:50, 20:50, 22:50` | 10 mins<br>*(ends at :00)* | **Daylight Prairie**<br>Sanctuary Islands Trench | **~310 – 488 Light** | Sunset colors the sky; giant moss turtle swims in the trench. Melt darkness on turtle's back + collect underwater clams. |
| **Dreams Skater** | **Every Hour :00**<br>*(Friday, Saturday, Sunday only)* | 15 mins<br>*(ends at :15)* | **Valley of Triumph**<br>Village of Dreams Ice Rink | **~400 Light** | Skater performs tricks around the ice rink, scattering light droplets behind them. |

---

## 2. Daily Fixed & Variable Wax Sources

| Source | Cadence | Location | Reward | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Campfires** | Daily | Prairie Caves, Forest Brook, Valley Temple, Wasteland Graveyard | **250 Light each**<br>*(1,000 Light total)* | Passive campfire wax pools with separate limits per realm. |
| **Passage of Reliance** | Daily | Forest Brook | **112 Light** | Season of Passage mask trial. |
| **Passage of Assistance** | Daily | Prairie Caves | **180 Light** | Season of Passage mask trial. |
| **Passage of Coordination** | Daily | Butterfly Fields | **128 Light** | Season of Passage mask trial. |
| **Passage of Alignment** | Daily | Boneyard (Forest) | **200 Light** | Season of Passage mask trial. |
| **Treasure Reef Eel** | Daily | Treasure Reef | **500 Light** | Collect wax trailing behind the giant deep-sea eel. |
| **Harmony Hall Challenge** | Daily | Harmony Hall (Valley) | **100 Light** | Daily musical sheet rhythm mini-game. |
| **Lightmending** | Daily | Aviary Village | **400 Light** | Daily light pool in central Aviary. |
| **Treasure Reef Clams** | Daily (Variable) | Treasure Reef seabed | **159 – 245 Light** | Large clams resting on underwater shipwrecks. |
| **Village Theater Bouquets** | Daily (Variable) | Village Theater (Valley) | **21 – 44 Light** | Rose bouquets scattered throughout the backstage. |
| **Yeti Race** | Daily (Variable) | Hermit Valley | **150 – 300 Light** | Downhill slope skiing race with fragments. |

---

## 3. Shard Eruption Mathematical Engine

Shard eruptions follow the exact Plutoy algorithm (`PlutoyDev/sky-shards`).

### A. Schedule & Category Determination
* Based on day of month in `America/Los_Angeles`:
  * **Odd Days of Month** $\implies$ **Red Shards** (Ascended Candles + Ancient Memories).
  * **Even Days of Month** $\implies$ **Black Shards** (Regular Wax).
* **Realm Cycle:** Realm index $= (\text{DayOfMonth} - 1) \pmod 5$:
  * 0: Daylight Prairie
  * 1: Hidden Forest
  * 2: Valley of Triumph
  * 3: Golden Wasteland
  * 4: Vault of Knowledge

### B. Shardless Days
* **Black Shards:** Saturdays & Sundays have **no black shard drops**.
* **Red Shards:** Mondays & Tuesdays have **no red shard drops**.

### C. Timing Offsets & Lifespan
* **Pre-Eruption Sky Darkening:** $-32\text{m } 10\text{s}$ before first wave.
* **Volcano Eruption:** $+7\text{m } 00\text{s}$ after start.
* **Crystals Land on Ground:** $+8\text{m } 40\text{s}$ after start.
* **Despawn:** Exactly **4 hours** after landing.
* **Cadence Intervals:**
  * Black Shard: **Every 8 Hours** (3 eruptions/day).
  * Red Shard: **Every 6 Hours** (4 eruptions/day).

### D. Ascended Candle (AC) Payout Rules (Red Shards)
| Realm & Map | Ascended Candles (AC) Yield |
| :--- | :--- |
| **Prairie Caves / Butterfly Fields** | **2.0 AC** |
| **Forest Brook / Forest Garden** | **2.5 AC** |
| **Valley Village of Dreams / Hermit Valley** | **2.5 AC** |
| **Wasteland Graveyard / Crab Field / Battlefield** | **2.0 – 2.5 AC** |
| **Vault Starlight Desert / Jellyfish Cove** | **3.5 AC** |
| **Forest Treehouse / Forgotten Ark** | **3.5 AC** |

---

## 4. Reset Times & Daily Rotations

### A. Daily Reset
* **Time:** **00:00 PST / 00:00 PDT** (Midnight Sky Standard Time).
* **Resets:** Daily quests, candle forging meter (chevrons), wax collection pools, daily seasonal candles.

### B. Weekly Reset (Eye of Eden / PNR)
* **Time:** **Every Sunday at 00:00 PST / Midnight**.
* **Resets:** All 63 statues in the Eye of Eden.
* **Max Return:** **15.75 Ascended Candles** per week (4 statues $= 1$ AC).

### C. Daily Quest Realm Sequence
The daily quests and 4 seasonal candle clusters rotate across realms each day starting from anchor `2024-09-30`:
$$\text{Daylight Prairie} \longrightarrow \text{Hidden Forest} \longrightarrow \text{Valley of Triumph} \longrightarrow \text{Golden Wasteland} \longrightarrow \text{Vault of Knowledge} \longrightarrow \text{(repeat)}$$

---

## 5. Traveling Spirits (TS) & Special Visits

### A. Traveling Spirits (TS)
* **Frequency:** **Every 2 Weeks** (alternating Thursdays).
* **Schedule:** Arrives Thursday at 00:00 Sky Time, departs Sunday at 23:59 Sky Time (**stays 96 hours**).
* **Offerings:** Emotes (Levels 1–4), Wing Buff (+1 permanent Winged Light for 2 AC), Cosmetics (capes, hair, masks, props, pants), Heart (3 regular candles).

### B. Special Visits (Group Returning Spirits)
* **Frequency:** **3 to 4 times per year**.
* **Duration:** Typically **14 days**.
* **Format:** Group of 4 spirits from past seasons arrive together in Aviary Village or a dedicated event area with all spirit trees unlocked simultaneously.

---

## 6. Annual Recurring Sky Events (12-Month Calendar)

| Event Name | Annual Month Window | Duration | Featured Activities & Currencies |
| :--- | :--- | :--- | :--- |
| **Days of Fortune** | **Late January – Early February** | 14 days | Lunar New Year dragon dance, firecrackers, dragon fireworks spell, ~5 tickets/day. |
| **Days of Love** | **Mid February (Valentine's)** | 14 days | Pink petal arches, pier at Home/Starlight Desert, message boats, ~5 tickets/day. |
| **Days of Bloom** | **Late March – Early April** | 21 days | Cherry blossom & wisteria trees blooming across realms, raining wax petals, ~5 tickets/day. |
| **Days of Nature** | **Mid – Late April (Earth Day)** | 14 days | Ocean vortex cleaning, Sanctuary plastic removal, dark darkness cleansing, ~5 tickets/day. |
| **Days of Treasure** | **May** | 14 days | Underwater diving challenges, sunken ship exploration, oceanic spells. |
| **Days of Color** | **Late May – Mid June** | 14 days | Rainbow geysers, 8-player colored capes puzzle in Orbit, rainbow trail spells, ~5 tickets/day. |
| **Workshop Show & Tell** | **Late June – Early July** | 14 days | Aviary furniture building showcase, Shared Space decoration contest. |
| **Sky Anniversary** | **Mid – Late July** | 21 days | Secret Office disco party, birthday cakes, TGC developer stage, birthday hats, ~5 tickets/day. |
| **Tournament of Triumph** | **Late July – Early August** | 14 days | Coliseum 4-team athletic trials (speed race, crab toss, wing gliding), ~5 tickets/day. |
| **Days of Sunlight** | **August** | 21 days | Forest campfire marshmallows, water slides, diving boards, jamming circle, ~5 tickets/day. |
| **Days of Moonlight** | **Late September – Early October** | 14 days | Moonlit lake, Crescent Oasis tea ceremonies, lotus message lanterns, ~5 tickets/day. |
| **Days of Style** | **Early October** | 14 days | Fashion runway, photo studio, runway catwalk challenges, ~5 tickets/day. |
| **Days of Mischief** | **Late October – Mid November** | 21 days | Cackling Crab cave, boiling cauldron wax, dark dragon rides, witch hats, spider capes, ~5 tickets/day. |
| **Days of Giving** | **Late November (Thanksgiving)** | 7–14 days | **Double Hearts & Double Heart Pieces** when trading light with friends. |
| **Days of Feast** | **Late December – Early January** | 21 days | Ice skating rink, Yeti races, snowmen, Grandma's festive fireplace, ~5 tickets/day. |

---

## 7. Seasons Standard Framework

* **Seasons Per Year:** Exactly **4 Seasons**.
* **Duration:** **70 to 77 days (~10 to 11 weeks)** per season.
* **Inter-Season Break:** Approximately **14 days (2 weeks)** between end of one season and start of the next.
* **Daily Seasonal Candles:**
  * **Non-Pass:** 5 candles per day (4 from Daily Quests + 1 from 4 Realm Clusters).
  * **Season Pass:** 6 candles per day (+1 extra daily bonus candle at Home/Aviary statue).
* **Recent Canon Seasons:**
  * *Season of Revival* (Oct 2023 – Dec 2023)
  * *Season of the Nine-Colored Deer* (Jan 2024 – Mar 2024)
  * *Season of Nesting* (Apr 2024 – Jun 2024)
  * *Season of Duets* (Jul 2024 – Sep 2024)
  * *Season of Moomin* (Oct 2024 – Dec 2024)
  * *Season of Radiance* (Jan 2025 – Apr 2025)
  * *Season of the Blue Bird* (Apr 2025 – Jul 2025)
  * *Season of The Two Embers - Part 1* (Jul 2025 – Oct 2025)
  * *Season of Migration* (Oct 2025 – Jan 2026)
  * *Season of Lightmending* (Jan 2026 – Apr 2026)
  * *Season of Carnival* (Apr 2026 – Jul 2026)
  * *Dear Van Gogh* (Jul 2026 – Oct 2026)
  * *Season of Pearl* (Oct 2026 – Dec 2026)

---

## 8. Currency Architecture Reference

| Currency Key | Name | Symbol / Icon | Acquisition Method |
| :---: | :--- | :---: | :--- |
| `c` | **Candles** | 🕯️ White Wax Candle | Forged from daily light pools, geyser, grandma, turtle, campfires (max ~20–21/day). |
| `h` | **Hearts** | ❤️ Heart | Gifted from friends (3 candles $= 1$ heart) or bought from Spirit Trees. |
| `sc` | **Seasonal Candles** | 🕯️ Orange Season Candle | Completed daily quests + season candle realm clusters (max 5–6/day). |
| `sh` | **Seasonal Hearts** | 🧡 Orange Heart | Unlocked at the top of seasonal spirit trees with a Season Pass. |
| `ac` | **Ascended Candles** | 💎 Glowing Red/Gold Candle | Eye of Eden statue saves (max 15.75/wk) + Red Shard completions (2.0–3.5 AC). |
| `ec` | **Event Currency** | 🎟️ Event Tickets | Discovered daily around active event areas (4–5 tickets/day). Converts to regular wax upon event end. |
