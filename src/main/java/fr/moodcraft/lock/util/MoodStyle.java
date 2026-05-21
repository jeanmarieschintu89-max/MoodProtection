package fr.moodcraft.lock.util;

import org.bukkit.command.CommandSender;

public final class MoodStyle {

    private MoodStyle() {
    }

    public static final String BRAND = "§aMood§6Craft";
    public static final String FRAME = "§8-----------------------------";

    public static String header(String module) {
        return "§8----- §6✦ " + module + " ✦ §8-----";
    }

    public static String info(String text) {
        return "§e➜ §f" + cleanPrefix(text);
    }

    public static String successLine(String text) {
        return "§a✔ §f" + cleanPrefix(text);
    }

    public static String errorLine(String text) {
        return "§c✖ §f" + cleanPrefix(text);
    }

    public static String bullet(String text) {
        return "§8• §7" + cleanPrefix(text);
    }

    public static void send(
            CommandSender sender,
            String module,
            String main,
            String... lines
    ) {

        sender.sendMessage("");
        sender.sendMessage(header(module));
        sender.sendMessage("");

        if (main != null && !main.isEmpty()) {
            sender.sendMessage(normalize(main));
            sender.sendMessage("");
        }

        if (lines != null) {
            for (String line : lines) {
                if (line == null) {
                    continue;
                }
                sender.sendMessage(normalize(line));
            }
        }

        sender.sendMessage("");
        sender.sendMessage(FRAME);
    }

    public static void error(
            CommandSender sender,
            String message,
            String... details
    ) {

        send(
                sender,
                "Sécurité Coffres",
                errorLine(message),
                details
        );
    }

    public static void success(
            CommandSender sender,
            String message,
            String... details
    ) {

        send(
                sender,
                "Sécurité Coffres",
                successLine(message),
                details
        );
    }

    public static void admin(
            CommandSender sender,
            String message,
            String... details
    ) {

        send(
                sender,
                "Administration Coffres",
                info(message),
                details
        );
    }

    private static String normalize(String line) {

        if (line == null || line.isBlank()) {
            return "";
        }

        String trimmed = line.trim();

        if (trimmed.startsWith("§e➜")
                || trimmed.startsWith("§a✔")
                || trimmed.startsWith("§c✖")
                || trimmed.startsWith("§c✘")
                || trimmed.startsWith("§8•")
                || trimmed.startsWith("§8-----")
                || trimmed.startsWith("§8----------------")) {

            return trimmed.replace("§c✘", "§c✖");
        }

        if (trimmed.startsWith("§a")) {
            return successLine(trimmed);
        }

        if (trimmed.startsWith("§c")) {
            return errorLine(trimmed);
        }

        if (trimmed.startsWith("§7")
                || trimmed.startsWith("§8")) {
            return bullet(trimmed);
        }

        return info(trimmed);
    }

    private static String cleanPrefix(String text) {

        if (text == null) {
            return "";
        }

        return text
                .replaceFirst("^§[0-9a-fk-or]", "")
                .replaceFirst("^➜\\s*", "")
                .replaceFirst("^✔\\s*", "")
                .replaceFirst("^✘\\s*", "")
                .replaceFirst("^✖\\s*", "")
                .replaceFirst("^•\\s*", "")
                .trim();
    }
}
