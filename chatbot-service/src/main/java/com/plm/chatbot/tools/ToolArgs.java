package com.plm.chatbot.tools;

import java.util.Map;

/** Small helpers for pulling typed values out of a tool call's {@code Map<String,Object>} args. */
final class ToolArgs {

    private ToolArgs() {
    }

    static String getString(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v == null ? null : v.toString();
    }

    static String requireString(Map<String, Object> args, String key) {
        String v = getString(args, key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing required argument: " + key);
        }
        return v;
    }

    static int getInt(Map<String, Object> args, String key, int defaultValue) {
        Object v = args.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(v.toString());
    }

    static <E extends Enum<E>> E getEnum(Map<String, Object> args, String key, Class<E> type) {
        String v = getString(args, key);
        if (v == null || v.isBlank()) {
            return null;
        }
        return Enum.valueOf(type, v.trim().toUpperCase());
    }
}
