package net.frealac.iamod.common.story;

import java.util.*;

public class VarietyUtil {
    private static final List<String> INTENSIFIERS = List.of("très", "plutôt", "un peu", "vraiment", "assez", "profondément");

    public static String intensify(Random r, String baseTrait) {
        if (r.nextDouble() < 0.6) return baseTrait;
        return INTENSIFIERS.get(r.nextInt(INTENSIFIERS.size())) + " " + baseTrait;
    }

    public static <T> T pick(List<T> list, Random r) { return list.get(r.nextInt(list.size())); }

    public static <T> void addDistinct(List<T> out, List<T> pool, Random r, int count) {
        int guard = 0;
        while (out.size() < count && guard++ < count*4) {
            T v = pick(pool, r);
            if (!out.contains(v)) out.add(v);
        }
    }
}

