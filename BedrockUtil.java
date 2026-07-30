package com.rpgcore.plugin.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * ⭐ 베드락 에디션 대응 유틸.
 *
 * 이 서버는 자바 서버(Paper)이고, 베드락(휴대폰/콘솔/윈도우10) 플레이어는
 * Geyser + Floodgate 플러그인을 통해 같은 서버에 접속합니다.
 * 즉 "자바 · 베드락이 같은 월드에서 같이 플레이" 하는 구조입니다.
 *
 * Floodgate API 를 컴파일 의존성으로 넣으면 Floodgate 가 없는 서버에서
 * 클래스 로딩 오류가 날 수 있으므로, 여기서는 리플렉션으로 안전하게 조회합니다.
 * Floodgate 가 없으면 모든 플레이어를 자바로 취급합니다. (기능 정상 작동)
 */
public final class BedrockUtil {

    private static boolean initialized = false;
    private static Object floodgateApi = null;
    private static Method isFloodgatePlayer = null;

    private BedrockUtil() {
    }

    private static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            floodgateApi = apiClass.getMethod("getInstance").invoke(null);
            isFloodgatePlayer = apiClass.getMethod("isFloodgatePlayer", UUID.class);
            Bukkit.getLogger().info("[RpgCore] Floodgate 감지됨 — 베드락 플레이어 호환 모드 활성화");
        } catch (Throwable ignored) {
            // Floodgate 미설치: 전부 자바 플레이어로 취급
            floodgateApi = null;
            isFloodgatePlayer = null;
        }
    }

    /** Floodgate(=베드락 접속 지원)가 서버에 설치되어 있는가 */
    public static boolean isFloodgateInstalled() {
        init();
        return floodgateApi != null;
    }

    /** 이 플레이어가 베드락 에디션으로 접속했는가 */
    public static boolean isBedrock(Player player) {
        if (player == null) {
            return false;
        }
        init();
        if (floodgateApi == null || isFloodgatePlayer == null) {
            return false;
        }
        try {
            Object result = isFloodgatePlayer.invoke(floodgateApi, player.getUniqueId());
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
