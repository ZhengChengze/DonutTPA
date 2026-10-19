# 🍩 DonutTPA — Modern Teleport Requests

**DonutTPA** is a sleek, DonutSMP-inspired teleport request plugin built for modern **Paper**, **Purpur**, **Spigot**, and **Folia** servers. It upgrades standard player-to-player teleportation with interactive confirmation GUIs, customizable sound effects, per-player toggle preferences, and live warmup countdowns while keeping server performance fast and lightweight.

---

## ✨ Key Features

* 🖥️ **DonutSMP-Inspired Confirmation & Accept GUIs** — Opens a clean 27-slot menu when sending or accepting requests, displaying:
  * **Slot 10:** Cancel / Deny (`RED_STAINED_GLASS_PANE`)
  * **Slot 12:** Target's Player Head (`PLAYER_HEAD`)
  * **Slot 13:** Request Type Indicator (`ENDER_PEARL` for `/tpa` or `FEATHER` for `/tpahere`)
  * **Slot 14:** World Dimension Indicator (`GRASS_BLOCK` for Overworld, `NETHERRACK` for Nether, `END_STONE` for The End)
  * **Slot 16:** Confirm / Accept (`LIME_STAINED_GLASS_PANE`)
* 💬 **Interactive Chat Buttons** — Incoming requests include clickable **`[✔ Accept]`** and **`[✖ Deny]`** chat buttons with hover tooltips for instant responses.
* 🔒 **Per-Player Privacy & Preference Toggles** — Players can individually toggle incoming `/tpa` requests, `/tpahere` requests, confirmation GUIs, or enable automatic request acceptance (`/tpauto`).
* ⏱️ **Live Per-Second Warmup Countdown** — Displays a second-by-second countdown (`5, 4, 3, 2, 1...`) with configurable sound ticks before teleporting.
* 🏃 **Smart Movement Cancellation** — Cancels the active teleport warmup if the player walks or jumps, while still allowing them to look around freely with their mouse.
* 🔊 **Fully Configurable Sounds & Messages** — Every chat message, GUI title, timer, and sound effect can be customized or translated in `config.yml`.
* ⚡ **Anti-Spam Cooldowns & Auto-Expiration** — Prevents request spam with configurable cooldowns and automatically expires unanswered requests.

![Accept request menu](https://cdn.modrinth.com/data/cached_images/7b2eb0ac368a384cf8c92af54d54b63027221fa7.png)
![Confirm](https://cdn.modrinth.com/data/cached_images/5ff0fd1790f7337d11deb77837c18ce8d4ab9b49.png)

---

## 💻 Commands

| Command | Aliases | Permission | Description |
| :--- | :--- | :--- | :--- |
| `/tpa <player>` | — | `donuttpa.use` | Open the confirmation GUI or send a teleport request to a player. |
| `/tpahere <player>` | — | `donuttpa.use` | Request another player to teleport to your location. |
| `/tpaccept [player]` | `/tpaaccept`, `/tpyes` | `donuttpa.use` | Open the accept GUI or accept an incoming teleport request. |
| `/tpdeny [player]` | `/tpadeny`, `/tpno` | `donuttpa.use` | Deny an incoming teleport request. |
| `/tpacancel` | — | `donuttpa.use` | Cancel your currently active outgoing teleport request. |
| `/tpatoggle` | — | `donuttpa.use` | Toggle receiving incoming `/tpa` requests on or off. |
| `/tpaheretoggle` | — | `donuttpa.use` | Toggle receiving incoming `/tpahere` requests on or off. |
| `/tpaguitoggle` | — | `donuttpa.use` | Toggle the confirmation and accept GUIs on or off for yourself. |
| `/tpauto` | — | `donuttpa.use` | Automatically accept incoming teleport requests. |
| `/dtpa reload` | — | `donuttpa.admin` | Reload the plugin configuration and messages in-game. |

---

## 🔐 Permissions

| Permission Node | Default | Description |
| :--- | :--- | :--- |
| `donuttpa.use` | `true` (Everyone) | Grants access to all player TPA and toggle commands. |
| `donuttpa.admin` | `op` | Grants access to `/dtpa reload` and bypasses all restrictions. |
| `donuttpa.cooldown.bypass` | `op` | Bypasses the cooldown timer between sending TPA requests. |
| `donuttpa.limit.bypass` | `op` | Bypasses target player TPA toggle restrictions and pending limits. |
| `donuttpa.bypass.delay` | `op` | Bypasses the teleport warmup countdown for instant teleportation. |

---

## ⚙️ Configuration (`config.yml`)

<details>
<summary><b>📂 Click to view default <code>config.yml</code></b></summary>

```yaml
# ==========================================
#     DonutTPA v26.10.2 (DonutSMP Edition)
# ==========================================

settings:
  request-expire-seconds: 60
  teleport-delay-seconds: 5
  cooldown-seconds: 10
  cancel-on-move: true
  use-gui-by-default: true

gui:
  size: 27
  confirm-title: "&8CONFIRM REQUEST"
  accept-title: "&8ACCEPT REQUEST"
  filler-enabled: true
  filler-material: "GRAY_STAINED_GLASS_PANE"

sounds:
  enabled: true
  gui-click: "UI_BUTTON_CLICK"
  request-sent: "ENTITY_EXPERIENCE_ORB_PICKUP"
  request-received: "BLOCK_NOTE_BLOCK_PLING"
  request-accepted: "ENTITY_PLAYER_LEVELUP"
  request-denied: "ENTITY_VILLAGER_NO"
  countdown-tick: "BLOCK_NOTE_BLOCK_HAT"
  teleport-success: "ENTITY_ENDERMAN_TELEPORT"
  teleport-cancelled: "ENTITY_VILLAGER_NO"

messages:
  prefix: ""
  no-permission: "&cYou do not have permission to use this command."
  only-players: "&cOnly players can execute this command."
  reload-success: "&aDonutTPA configuration reloaded successfully!"
  usage-tpa: "&cUsage: /tpa <player>"
  usage-tpahere: "&cUsage: /tpahere <player>"
  player-not-found: "&cPlayer &e%player% &cis not online."
  cannot-tpa-self: "&cYou cannot send a teleport request to yourself."
  cooldown-active: "&cPlease wait &e%time%s &cbefore sending another request."
  already-pending: "&cYou already have a pending request to &e%player%&c."
  
  # Toggle Restriction Messages (DonutSMP Style)
  target-tpa-disabled: "&cThat user is not accepting tpa requests."
  target-tpahere-disabled: "&cThat user is not accepting tpahere requests."
  
  # Request Sent & Received (DonutSMP Style)
  request-sent: "&eYou sent &b%target% &ea tpa request."
  request-sent-tpahere: "&eYou sent &b%target% &ea tpahere request."
  request-received-tpa: "&b%sender% &esent you a tpa request."
  request-received-tpahere: "&b%sender% &esent you a tpahere request."
  
  # Interactive Chat Buttons
  buttons-prefix: "&7Click an option: "
  button-accept-text: "&a&l[✔ Accept]"
  button-accept-hover: "&aClick to accept %sender%'s request"
  button-deny-text: "&c&l[✖ Deny]"
  button-deny-hover: "&cClick to deny %sender%'s request"
  
  # Accept / Deny / Cancel / Expiration
  no-pending-requests: "&cYou do not have any pending teleport requests."
  no-outgoing-requests: "&cYou do not have any outgoing teleport requests to cancel."
  request-accepted-target: "&aYour teleport request has been accepted."
  request-accepted-sender: "&aYour teleport request has been accepted."
  request-denied-target: "&cYou denied &e%sender%'s &cteleport request."
  request-denied-sender: "&cYour teleport request to &e%target% &cwas denied."
  request-cancelled: "&eYou cancelled your teleport request to &b%target%&e."
  request-expired-sender: "&cThis teleport request might be expired."
  request-expired-target: "&cThe teleport request from &e%sender% &chas expired."
  
  # Per-Second Countdown & Status (DonutSMP Style)
  teleport-countdown: "&7Teleporting in &b%time% &7seconds..."
  teleport-success: "&aTeleported successfully!"
  teleport-cancelled-moved: "&cTeleport cancelled because you moved!"
  teleport-cancelled-moved-other: "&cTeleport cancelled because &e%player% &cmoved!"
  teleport-cancelled-offline: "&cTeleport cancelled because the other player went offline."
  
  # Toggle Feedback Messages
  toggle-tpa-enabled: "&aYou are now accepting TPA requests."
  toggle-tpa-disabled: "&cYou are no longer accepting TPA requests."
  toggle-tpahere-enabled: "&aYou are now accepting TPAHere requests."
  toggle-tpahere-disabled: "&cYou are no longer accepting TPAHere requests."
  toggle-gui-enabled: "&aTPA Confirmation GUI has been &2enabled&a."
  toggle-gui-disabled: "&cTPA Confirmation GUI has been &4disabled&c."
  toggle-auto-enabled: "&aAuto-Accept TPA requests has been &2enabled&a."
  toggle-auto-disabled: "&cAuto-Accept TPA requests has been &4disabled&c."
```

</details>

---

## 🏷️ Message Placeholders

| Placeholder | Description |
| :--- | :--- |
| `%sender%` | The username of the player who sent the request. |
| `%target%` | The username of the player who received the request. |
| `%player%` | Universal fallback for the other player's username. |
| `%expire%` | Remaining seconds until the teleport request expires. |
| `%time%` / `%delay%` | Remaining cooldown or warmup countdown seconds. |

---

## 🚀 Installation

1. Download the latest `DonutTPA-2.0.0.jar` from Modrinth.
2. Drop the `.jar` file into your server's `plugins/` directory.
3. Restart your server to generate the configuration files.
4. Customize `plugins/DonutTPA/config.yml` and apply changes anytime using `/dtpa reload`.

---

## 👨‍💻 Credits & Contributors

* **Chéngzé** — *Lead Developer & Author*
* **MonoromX & Ly_No** — *In-Game Testing & QA*
