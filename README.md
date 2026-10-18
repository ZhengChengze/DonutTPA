# 🍩 DonutTPA

**DonutTPA** is a fast, lightweight teleport request plugin designed for modern Minecraft servers. It focuses entirely on delivering a clean, lag-free TPA experience without unnecessary bloat.

---

## ✨ Features

* **Simple TPA & TPAHere System** — Send teleport requests to players (`/tpa`) or invite them to teleport to you (`/tpahere`).
* **Interactive Chat Buttons** — Incoming requests include clickable **[✔ Accept]** and **[✖ Deny]** buttons with hover tooltips.
* **Request Expiration** — Pending requests automatically expire after a configurable duration.
* **Teleport Warmup Delay** — Configurable countdown before teleportation occurs.
* **Smart Movement Cancellation** — Optionally cancels the teleport warmup if the player walks or jumps (allows looking around with the mouse).
* **Anti-Spam Cooldowns** — Configurable cooldown timer between sending requests.
* **100% Customizable** — Every timer, toggle, prefix, and chat message can be customized in `config.yml`.
* **Lightweight & Fast** — Uses `O(1)` memory lookups and cleans up tasks automatically when players leave.

---

## 💻 Commands

| Command | Aliases | Description |
| :--- | :--- | :--- |
| `/tpa <player>` | — | Send a request to teleport to another player. |
| `/tpahere <player>` | — | Request another player to teleport to your location. |
| `/tpaccept [player]` | `/tpyes` | Accept an incoming teleport request. |
| `/tpdeny [player]` | `/tpno` | Deny an incoming teleport request. |
| `/tpacancel` | — | Cancel your currently active outgoing teleport request. |

---

## 🔐 Permissions

| Permission | Default | Description |
| :--- | :--- | :--- |
| `donuttpa.use` | `true` (Everyone) | Allows access to all core TPA commands. |
| `donuttpa.bypass.cooldown` | `op` | Bypass the cooldown between sending TPA requests. |
| `donuttpa.bypass.delay` | `op` | Bypass the warmup countdown when teleporting. |

---

## ⚙️ Default Configuration (`config.yml`)

```yaml
settings:
  request-expire-seconds: 60
  teleport-delay-seconds: 5
  cooldown-seconds: 10
  cancel-on-move: true

messages:
  prefix: "&6[DonutTPA] &r"
  no-permission: "&cYou do not have permission to use this command."
  only-players: "&cOnly players can execute this command."
  usage-tpa: "&cUsage: /tpa <player>"
  usage-tpahere: "&cUsage: /tpahere <player>"
  player-not-found: "&cPlayer &e%player% &cis not online."
  cannot-tpa-self: "&cYou cannot send a teleport request to yourself."
  cooldown-active: "&cPlease wait &e%time%s &cbefore sending another request."
  already-pending: "&cYou already have a pending request to &e%player%&c."
  request-sent: "&aTeleport request sent to &e%target%&a. It expires in &e%expire%s&a."
  request-received-tpa: "&e%sender% &ahas requested to teleport to you."
  request-received-tpahere: "&e%sender% &ahas requested that you teleport to them."
  buttons-prefix: "&7Click an option: "
  button-accept-text: "&a&l[✔ Accept]"
  button-accept-hover: "&aClick to accept %sender%'s request"
  button-deny-text: "&c&l[✖ Deny]"
  button-deny-hover: "&cClick to deny %sender%'s request"
  no-pending-requests: "&cYou do not have any pending teleport requests."
  no-outgoing-requests: "&cYou do not have any outgoing teleport requests to cancel."
  request-accepted-target: "&aYou accepted &e%sender%'s &ateleport request."
  request-accepted-sender: "&e%target% &aaccepted your teleport request!"
  request-denied-target: "&cYou denied &e%sender%'s &cteleport request."
  request-denied-sender: "&e%target% &cdenied your teleport request."
  request-cancelled: "&eYou cancelled your teleport request to &6%target%&e."
  request-expired-sender: "&cYour teleport request to &e%target% &chas expired."
  request-expired-target: "&cThe teleport request from &e%sender% &chas expired."
  teleport-warmup: "&aTeleporting in &e%delay% seconds&a... Do not move!"
  teleport-success: "&aTeleported successfully!"
  teleport-cancelled-moved: "&cTeleport cancelled because you moved!"
  teleport-cancelled-moved-other: "&cTeleport cancelled because &e%player% &cmoved!"
  teleport-cancelled-offline: "&cTeleport cancelled because the other player went offline."
