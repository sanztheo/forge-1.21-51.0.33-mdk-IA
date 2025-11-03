package net.frealac.iamod.common.story;

import java.util.Locale;
import java.util.Random;

/**
 * Generates thousands of plausible FR-like names by combining syllables.
 */
public class NameMaker {
    private static final String[] SYL_A = {"ma","me","mi","mo","mu","na","ne","no","nu","ra","re","ri","ro","ru","la","le","li","lo","lu","va","ve","vi","vo","vu","sa","se","si","so","su","el","al","an","en","ar","er","is","os","am","em","ol","ul"};
    private static final String[] SYL_B = {"elle","elle","ine","ine","ie","on","an","en","ou","ette","elle","ard","aud","eau","eaux","in","ain","ien","ion","oir","oire","eur","euse","ier","ié","ot","otte","u","el","ette","ille","y","ay","ey"};
    private static final String[] SYL_C = {"na","da","ta","ga","la","ra","sa","qua","cha","sha","tha","pha","cia","tia","lia","ria","nia","dra","bra","fra","cla","gla","pla","pru","dru","gru","tru"};

    public static String given(Random r, String sex) {
        // 2-3 syllables + optional ending, capitalized
        String s1 = pick(SYL_A, r);
        String s2 = pick(r.nextBoolean()?SYL_A:SYL_C, r);
        String end = pick(SYL_B, r);
        String base = s1 + s2 + (r.nextDouble()<0.7?end:"");
        if (sex != null && sex.equals("male") && base.endsWith("euse")) base = base.substring(0, base.length()-4) + "eur";
        if (sex != null && sex.equals("female") && base.endsWith("eur")) base = base.substring(0, base.length()-3) + "euse";
        return capitalize(base);
    }

    public static String family(Random r) {
        String[] roots = {"dur","mont","val","font","mart","petit","bern","roche","riv","bois","clair","long","fort","mer","ciel","fau","noir","roux","blanc","bleu","grand","petit","chaud","froid","beau","belle"};
        String[] suffix = {"and","and","and","ard","aud","eau","eaux","ier","ieux","et","ot","ot","in","ain","ier","mont","ville","court","fort","val","pont","champ","loup","fils"};
        String base = pick(roots, r) + pick(suffix, r);
        return capitalize(base);
    }

    private static String pick(String[] arr, Random r) { return arr[r.nextInt(arr.length)]; }
    private static String capitalize(String s) { return s.isEmpty()? s : s.substring(0,1).toUpperCase(Locale.ROOT) + s.substring(1); }
}

