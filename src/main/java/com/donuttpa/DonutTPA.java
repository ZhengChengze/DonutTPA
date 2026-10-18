package com.donuttpa;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class DonutTPA extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {

    public enum RequestType {
        TPA, TPAHERE
    }

    public static class TpaRequest {
        private final UUID sender;
        private final UUID target;
        private final RequestType type;
        private final BukkitTask expiryTask;

        public TpaRequest(UUID sender, UUID target, RequestType type, BukkitTask expiryTask) {
            this.sender = sender;
            this.target = target;
            this.type = type;
            this.expiryTask = expiryTask;
        }

        public UUID getSender() { return sender; }
        public UUID getTarget() { return target; }
        public RequestType getType() { return type; }
        public void cancelExpiry() {
            if (expiryTask != null && !expiryTask.isCancelled()) {
                expiryTask.cancel();
            }
        }
    }

    public static class WarmupSession {
        private final UUID teleportingPlayer;
        private final UUID otherPlayer;
        private final Location initialLocation;
        private final BukkitTask task;

        public WarmupSession(UUID teleportingPlayer, UUID otherPlayer, Location initialLocation, BukkitTask task) {
            this.teleportingPlayer = teleportingPlayer;
            this.otherPlayer = otherPlayer;
            this.initialLocation = initialLocation;
            this.task = task;
        }

        public UUID getTeleportingPlayer() { return teleportingPlayer; }
        public UUID getOtherPlayer() { return otherPlayer; }
        public Location getInitialLocation() { return initialLocation; }
        public void cancel() {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
    }

    private final Map<UUID, TpaRequest> outgoingRequests = new HashMap<>();
    private final Map<UUID, LinkedHashMap<UUID, TpaRequest>> incomingRequests = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, WarmupSession> activeWarmups = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        registerConfigDefaults();

        String[] commands = {"tpa", "tpahere", "tpaccept", "tpdeny", "tpacancel"};
        for (String cmd : commands) {
            if (getCommand(cmd) != null) {
                getCommand(cmd).setExecutor(this);
                getCommand(cmd).setTabCompleter(this);
            }
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("DonutTPA has been enabled!");
    }

    @Override
    public void onDisable() {
        outgoingRequests.values().forEach(TpaRequest::cancelExpiry);
        activeWarmups.values().forEach(WarmupSession::cancel);
        outgoingRequests.clear();
        incomingRequests.clear();
        cooldowns.clear();
        activeWarmups.clear();
    }

    private void registerConfigDefaults() {
        FileConfiguration config = getConfig();
        config.addDefault("settings.request-expire-seconds", 60);
        config.addDefault("settings.teleport-delay-seconds", 5);
        config.addDefault("settings.cooldown-seconds", 10);
        config.addDefault("settings.cancel-on-move", true);

        config.addDefault("messages.prefix", "&6[DonutTPA] &r");
        config.addDefault("messages.no-permission", "&cYou do not have permission to use this command.");
        config.addDefault("messages.only-players", "&cOnly players can execute this command.");
        config.addDefault("messages.usage-tpa", "&cUsage: /tpa <player>");
        config.addDefault("messages.usage-tpahere", "&cUsage: /tpahere <player>");
        config.addDefault("messages.player-not-found", "&cPlayer &e%player% &cis not online.");
        config.addDefault("messages.cannot-tpa-self", "&cYou cannot send a teleport request to yourself.");
        config.addDefault("messages.cooldown-active", "&cPlease wait &e%time%s &cbefore sending another request.");
        config.addDefault("messages.already-pending", "&cYou already have a pending request to &e%player%&c.");
        config.addDefault("messages.request-sent", "&aTeleport request sent to &e%player%&a. It expires in &e%expire%s&a.");
        config.addDefault("messages.request-received", "&e%sender% &ahas requested to teleport to you.");
        config.addDefault("messages.request-received-tpa", "&e%sender% &ahas requested to teleport to you.");
        config.addDefault("messages.request-received-tpahere", "&e%sender% &ahas requested that you teleport to them.");
        config.addDefault("messages.buttons-prefix", "&7Click an option: ");
        config.addDefault("messages.button-accept-text", "&a&l[✔ Accept]");
        config.addDefault("messages.button-accept-hover", "&aClick to accept %sender%'s request");
        config.addDefault("messages.button-deny-text", "&c&l[✖ Deny]");
        config.addDefault("messages.button-deny-hover", "&cClick to deny %sender%'s request");
        config.addDefault("messages.no-pending-requests", "&cYou do not have any pending teleport requests.");
        config.addDefault("messages.no-outgoing-requests", "&cYou do not have any outgoing teleport requests to cancel.");
        config.addDefault("messages.no-pending-outgoing", "&cYou do not have any outgoing teleport requests to cancel.");
        config.addDefault("messages.request-accepted-target", "&aYou accepted &e%sender%'s &ateleport request.");
        config.addDefault("messages.request-accepted-sender", "&e%target% &aaccepted your teleport request!");
        config.addDefault("messages.request-denied-target", "&cYou denied &e%sender%'s &cteleport request.");
        config.addDefault("messages.request-denied-sender", "&e%target% &cdenied your teleport request.");
        config.addDefault("messages.request-cancelled", "&eYou cancelled your teleport request to &6%target%&e.");
        config.addDefault("messages.request-expired-sender", "&cYour teleport request to &e%target% &chas expired.");
        config.addDefault("messages.request-expired-target", "&cThe teleport request from &e%sender% &chas expired.");
        config.addDefault("messages.teleport-warmup", "&aTeleporting in &e%delay% seconds&a... Do not move!");
        config.addDefault("messages.teleport-success", "&aTeleported successfully!");
        config.addDefault("messages.teleport-cancelled-moved", "&cTeleport cancelled because you moved!");
        config.addDefault("messages.teleport-cancelled-moved-other", "&cTeleport cancelled because &e%player% &cmoved!");
        config.addDefault("messages.teleport-cancelled-offline", "&cTeleport cancelled because the other player went offline.");

        config.options().copyDefaults(true);
        saveConfig();
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private String getMsg(String key, String fallback) {
        String prefix = getConfig().getString("messages.prefix", "&6[DonutTPA] &r");
        String msg = getConfig().getString("messages." + key);
        if (msg == null || msg.isEmpty()) {
            msg = fallback;
        }
        return color(prefix + msg);
    }

    private String formatPlaceholders(String message, String senderName, String targetName, String otherPlayerName) {
        String result = message;
        if (senderName != null) {
            result = result.replace("%sender%", senderName);
        }
        if (targetName != null) {
            result = result.replace("%target%", targetName);
        }
        if (otherPlayerName != null) {
            result = result.replace("%player%", otherPlayerName);
        }
        return result;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMsg("only-players", "&cOnly players can execute this command."));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("donuttpa.use")) {
            player.sendMessage(getMsg("no-permission", "&cYou do not have permission to use this command."));
            return true;
        }

        String cmd = command.getName().toLowerCase();
        switch (cmd) {
            case "tpa":
                handleSendRequest(player, args, RequestType.TPA);
                break;
            case "tpahere":
                handleSendRequest(player, args, RequestType.TPAHERE);
                break;
            case "tpaccept":
                handleAccept(player, args);
                break;
            case "tpdeny":
                handleDeny(player, args);
                break;
            case "tpacancel":
                handleCancel(player);
                break;
        }
        return true;
    }

    private void handleSendRequest(Player sender, String[] args, RequestType type) {
        if (args.length < 1) {
            if (type == RequestType.TPA) {
                sender.sendMessage(getMsg("usage-tpa", "&cUsage: /tpa <player>"));
            } else {
                sender.sendMessage(getMsg("usage-tpahere", "&cUsage: /tpahere <player>"));
            }
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(formatPlaceholders(
                    getMsg("player-not-found", "&cPlayer &e%player% &cis not online."),
                    sender.getName(), args[0], args[0]));
            return;
        }

        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(getMsg("cannot-tpa-self", "&cYou cannot send a teleport request to yourself."));
            return;
        }

        if (!sender.hasPermission("donuttpa.bypass.cooldown")) {
            Long cooldownEnd = cooldowns.get(sender.getUniqueId());
            if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
                long remaining = (cooldownEnd - System.currentTimeMillis() + 999) / 1000;
                sender.sendMessage(getMsg("cooldown-active", "&cPlease wait &e%time%s &cbefore sending another request.")
                        .replace("%time%", String.valueOf(remaining)));
                return;
            }
        }

        TpaRequest existing = outgoingRequests.get(sender.getUniqueId());
        if (existing != null) {
            if (existing.getTarget().equals(target.getUniqueId())) {
                sender.sendMessage(formatPlaceholders(
                        getMsg("already-pending", "&cYou already have a pending request to &e%player%&c."),
                        sender.getName(), target.getName(), target.getName()));
                return;
            }
            removeRequest(existing);
        }

        int expireSeconds = getConfig().getInt("settings.request-expire-seconds", 60);
        int cooldownSeconds = getConfig().getInt("settings.cooldown-seconds", 10);
        String senderName = sender.getName();
        String targetName = target.getName();

        BukkitTask expiryTask = Bukkit.getScheduler().runTaskLater(this, () -> {
            TpaRequest req = outgoingRequests.get(sender.getUniqueId());
            if (req != null && req.getTarget().equals(target.getUniqueId())) {
                removeRequest(req);
                Player s = Bukkit.getPlayer(req.getSender());
                Player t = Bukkit.getPlayer(req.getTarget());
                if (s != null) {
                    s.sendMessage(formatPlaceholders(
                            getMsg("request-expired-sender", "&cYour teleport request to &e%target% &chas expired."),
                            senderName, targetName, targetName));
                }
                if (t != null) {
                    t.sendMessage(formatPlaceholders(
                            getMsg("request-expired-target", "&cThe teleport request from &e%sender% &chas expired."),
                            senderName, targetName, senderName));
                }
            }
        }, expireSeconds * 20L);

        TpaRequest newRequest = new TpaRequest(sender.getUniqueId(), target.getUniqueId(), type, expiryTask);
        outgoingRequests.put(sender.getUniqueId(), newRequest);
        incomingRequests.computeIfAbsent(target.getUniqueId(), k -> new LinkedHashMap<>())
                .put(sender.getUniqueId(), newRequest);

        if (cooldownSeconds > 0 && !sender.hasPermission("donuttpa.bypass.cooldown")) {
            cooldowns.put(sender.getUniqueId(), System.currentTimeMillis() + (cooldownSeconds * 1000L));
        }

        // Send confirmation to sender (replaces both %player% and %target%)
        sender.sendMessage(formatPlaceholders(
                getMsg("request-sent", "&aTeleport request sent to &e%player%&a. It expires in &e%expire%s&a."),
                senderName, targetName, targetName)
                .replace("%expire%", String.valueOf(expireSeconds)));

        // Send notification to target
        String receivedMsg;
        if (type == RequestType.TPA) {
            String raw = getConfig().getString("messages.request-received-tpa",
                    getConfig().getString("messages.request-received", "&e%sender% &ahas requested to teleport to you."));
            receivedMsg = color(getConfig().getString("messages.prefix", "&6[DonutTPA] &r") + raw);
        } else {
            receivedMsg = getMsg("request-received-tpahere", "&e%sender% &ahas requested that you teleport to them.");
        }
        target.sendMessage(formatPlaceholders(receivedMsg, senderName, targetName, senderName));
        sendInteractiveButtons(target, senderName);
    }

    @SuppressWarnings("deprecation")
    private void sendInteractiveButtons(Player target, String senderName) {
        String prefix = color(getConfig().getString("messages.prefix", "&6[DonutTPA] &r"));
        String btnPrefix = color(getConfig().getString("messages.buttons-prefix", "&7Click an option: "));

        TextComponent message = new TextComponent(prefix + btnPrefix);

        String acceptText = color(getConfig().getString("messages.button-accept-text", "&a&l[✔ Accept]"));
        String acceptHover = formatPlaceholders(
                color(getConfig().getString("messages.button-accept-hover", "&aClick to accept %sender%'s request")),
                senderName, target.getName(), senderName);

        TextComponent acceptBtn = new TextComponent(acceptText);
        acceptBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + senderName));
        acceptBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(acceptHover).create()));

        TextComponent space = new TextComponent("  ");

        String denyText = color(getConfig().getString("messages.button-deny-text", "&c&l[✖ Deny]"));
        String denyHover = formatPlaceholders(
                color(getConfig().getString("messages.button-deny-hover", "&cClick to deny %sender%'s request")),
                senderName, target.getName(), senderName);

        TextComponent denyBtn = new TextComponent(denyText);
        denyBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny " + senderName));
        denyBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(denyHover).create()));

        message.addExtra(acceptBtn);
        message.addExtra(space);
        message.addExtra(denyBtn);

        target.spigot().sendMessage(message);
    }

    private TpaRequest getIncomingRequest(Player target, String[] args) {
        LinkedHashMap<UUID, TpaRequest> map = incomingRequests.get(target.getUniqueId());
        if (map == null || map.isEmpty()) {
            return null;
        }

        if (args.length > 0) {
            Player specificSender = Bukkit.getPlayerExact(args[0]);
            if (specificSender != null) {
                return map.get(specificSender.getUniqueId());
            }
            return null;
        }

        TpaRequest last = null;
        for (TpaRequest req : map.values()) {
            last = req;
        }
        return last;
    }

    private void handleAccept(Player target, String[] args) {
        TpaRequest req = getIncomingRequest(target, args);
        if (req == null) {
            target.sendMessage(getMsg("no-pending-requests", "&cYou do not have any pending teleport requests."));
            return;
        }

        removeRequest(req);
        Player sender = Bukkit.getPlayer(req.getSender());
        if (sender == null || !sender.isOnline()) {
            target.sendMessage(getMsg("teleport-cancelled-offline", "&cTeleport cancelled because the other player went offline."));
            return;
        }

        target.sendMessage(formatPlaceholders(
                getMsg("request-accepted-target", "&aYou accepted &e%sender%'s &ateleport request."),
                sender.getName(), target.getName(), sender.getName()));
        sender.sendMessage(formatPlaceholders(
                getMsg("request-accepted-sender", "&e%target% &aaccepted your teleport request!"),
                sender.getName(), target.getName(), target.getName()));

        Player teleporting = (req.getType() == RequestType.TPA) ? sender : target;
        Player destination = (req.getType() == RequestType.TPA) ? target : sender;

        startTeleportWarmup(teleporting, destination);
    }

    private void handleDeny(Player target, String[] args) {
        TpaRequest req = getIncomingRequest(target, args);
        if (req == null) {
            target.sendMessage(getMsg("no-pending-requests", "&cYou do not have any pending teleport requests."));
            return;
        }

        removeRequest(req);
        Player sender = Bukkit.getPlayer(req.getSender());
        String senderName = (sender != null) ? sender.getName() : "Player";

        target.sendMessage(formatPlaceholders(
                getMsg("request-denied-target", "&cYou denied &e%sender%'s &cteleport request."),
                senderName, target.getName(), senderName));

        if (sender != null && sender.isOnline()) {
            sender.sendMessage(formatPlaceholders(
                    getMsg("request-denied-sender", "&e%target% &cdenied your teleport request."),
                    senderName, target.getName(), target.getName()));
        }
    }

    private void handleCancel(Player sender) {
        TpaRequest req = outgoingRequests.get(sender.getUniqueId());
        if (req == null) {
            String raw = getConfig().getString("messages.no-outgoing-requests",
                    getConfig().getString("messages.no-pending-outgoing", "&cYou do not have any outgoing teleport requests to cancel."));
            sender.sendMessage(color(getConfig().getString("messages.prefix", "&6[DonutTPA] &r") + raw));
            return;
        }

        removeRequest(req);
        Player target = Bukkit.getPlayer(req.getTarget());
        String targetName = (target != null) ? target.getName() : "Player";
        sender.sendMessage(formatPlaceholders(
                getMsg("request-cancelled", "&eYou cancelled your teleport request to &6%target%&e."),
                sender.getName(), targetName, targetName));
    }

    private void startTeleportWarmup(Player teleporting, Player destination) {
        WarmupSession oldWarmup = activeWarmups.remove(teleporting.getUniqueId());
        if (oldWarmup != null) {
            oldWarmup.cancel();
        }

        int delay = getConfig().getInt("settings.teleport-delay-seconds", 5);
        if (delay <= 0 || teleporting.hasPermission("donuttpa.bypass.delay")) {
            teleporting.teleport(destination.getLocation());
            teleporting.sendMessage(getMsg("teleport-success", "&aTeleported successfully!"));
            return;
        }

        teleporting.sendMessage(getMsg("teleport-warmup", "&aTeleporting in &e%delay% seconds&a... Do not move!")
                .replace("%delay%", String.valueOf(delay))
                .replace("%time%", String.valueOf(delay)));
        Location startLoc = teleporting.getLocation().clone();

        BukkitTask task = Bukkit.getScheduler().runTaskLater(this, () -> {
            activeWarmups.remove(teleporting.getUniqueId());
            if (!teleporting.isOnline() || !destination.isOnline()) {
                if (teleporting.isOnline()) {
                    teleporting.sendMessage(getMsg("teleport-cancelled-offline", "&cTeleport cancelled because the other player went offline."));
                }
                return;
            }
            teleporting.teleport(destination.getLocation());
            teleporting.sendMessage(getMsg("teleport-success", "&aTeleported successfully!"));
        }, delay * 20L);

        activeWarmups.put(teleporting.getUniqueId(),
                new WarmupSession(teleporting.getUniqueId(), destination.getUniqueId(), startLoc, task));
    }

    private void removeRequest(TpaRequest req) {
        req.cancelExpiry();
        outgoingRequests.remove(req.getSender());
        LinkedHashMap<UUID, TpaRequest> targetMap = incomingRequests.get(req.getTarget());
        if (targetMap != null) {
            targetMap.remove(req.getSender());
            if (targetMap.isEmpty()) {
                incomingRequests.remove(req.getTarget());
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!getConfig().getBoolean("settings.cancel-on-move", true)) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        WarmupSession session = activeWarmups.remove(player.getUniqueId());
        if (session != null) {
            session.cancel();
            player.sendMessage(getMsg("teleport-cancelled-moved", "&cTeleport cancelled because you moved!"));
            Player other = Bukkit.getPlayer(session.getOtherPlayer());
            if (other != null && other.isOnline()) {
                other.sendMessage(formatPlaceholders(
                        getMsg("teleport-cancelled-moved-other", "&cTeleport cancelled because &e%player% &cmoved!"),
                        player.getName(), other.getName(), player.getName()));
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        TpaRequest outgoing = outgoingRequests.get(uuid);
        if (outgoing != null) {
            removeRequest(outgoing);
        }

        LinkedHashMap<UUID, TpaRequest> incoming = incomingRequests.remove(uuid);
        if (incoming != null) {
            for (TpaRequest req : new ArrayList<>(incoming.values())) {
                removeRequest(req);
            }
        }

        WarmupSession session = activeWarmups.remove(uuid);
        if (session != null) {
            session.cancel();
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String cmd = command.getName().toLowerCase();
            if (cmd.equals("tpa") || cmd.equals("tpahere") || cmd.equals("tpaccept") || cmd.equals("tpdeny")) {
                String prefix = args[0].toLowerCase();
                List<String> matches = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.equals(sender) && p.getName().toLowerCase().startsWith(prefix)) {
                        matches.add(p.getName());
                    }
                }
                return matches;
            }
        }
        return Collections.emptyList();
    }
}