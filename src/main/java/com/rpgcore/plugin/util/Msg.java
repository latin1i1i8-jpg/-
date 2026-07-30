package com.rpgcore.plugin.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * ⭐ 베드락 호환 메시지 전송기.
 *
 * 베드락 클라이언트의 기본 폰트는 🎉 🪐 🗺️ 같은 이모지를 지원하지 않아서
 * 네모(□)로 깨져 보입니다. 그래서 베드락 플레이어에게 보낼 때만
 * 이모지를 지우고, 화살표 같은 기호는 ASCII 로 바꿔서 보냅니다.
 * 자바 플레이어는 원본 그대로 받습니다.
 *
 * 사용법: Msg.send(player, ChatColor.GREEN + "..."), Msg.title(player, "...", "...")
 */
public final class Msg {

    /** config: bedrock.strip-emoji (RpgCorePlugin 이 시작할 때 넣어줌) */
    public static boolean stripEmoji = true;

    private Msg() {
    }

    public static void send(CommandSender sender, String message) {
        if (sender instanceof Player player) {
            sender.sendMessage(format(player, message));
        } else {
            sender.sendMessage(message);
        }
    }

    /** 플레이어에 맞춰 문자열을 다듬는다 (베드락이면 이모지 제거) */
    public static String format(Player player, String message) {
        if (message == null) {
            return "";
        }
        if (!stripEmoji || !BedrockUtil.isBedrock(player)) {
            return message;
        }
        return sanitize(message);
    }

    /**
     * 베드락 폰트에서 깨지는 문자를 정리한다.
     *  - BMP 밖의 문자(대부분의 이모지, 서로게이트 쌍) 제거
     *  - 자주 쓰는 기호는 ASCII 대체
     * 한글(가~힣), 색 코드(§) 는 베드락에서도 정상이므로 그대로 둡니다.
     */
    public static String sanitize(String message) {
        String out = message
                .replace("→", "->")
                .replace("←", "<-")
                .replace("↑", "^")
                .replace("↓", "v")
                .replace("▶", ">")
                .replace("◀", "<")
                .replace("■", "#")
                .replace("●", "o")
                .replace("※", "*")
                .replace("…", "...")
                .replace("·", "-")
                .replace("⭐", "*")
                .replace("★", "*")
                .replace("☆", "*")
                .replace("✨", "*")
                .replace("✔", "v")
                .replace("✖", "x");

        StringBuilder sb = new StringBuilder(out.length());
        for (int i = 0; i < out.length(); i++) {
            char c = out.charAt(i);
            if (Character.isHighSurrogate(c)) {
                // 이모지 등 BMP 밖 문자 -> 통째로 건너뛰기
                if (i + 1 < out.length() && Character.isLowSurrogate(out.charAt(i + 1))) {
                    i++;
                }
                continue;
            }
            if (Character.isLowSurrogate(c)) {
                continue;
            }
            // 그림문자 영역(♠♥⚔☠ 등)도 베드락에서 잘 깨짐
            if (c >= '\u2600' && c <= '\u27BF') {
                continue;
            }
            if (c >= '\uFE00' && c <= '\uFE0F') { // 이모지 변형 선택자
                continue;
            }
            sb.append(c);
        }
        // 이모지를 지우고 남은 이중 공백 정리
        return sb.toString().replaceAll(" {2,}", " ").trim();
    }

    /** 화면 중앙 큰 글씨 (자바/베드락 모두 지원) */
    public static void title(Player player, String title, String subtitle) {
        player.sendTitle(format(player, title), format(player, subtitle), 5, 50, 10);
    }
}
