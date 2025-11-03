package net.frealac.iamod.common.story;

import java.util.Locale;
import java.util.Random;

public class PlaceNameMaker {
    private static final String[] ROOTS = {
            "meule","roche","mousse","sapin","buis","chene","orme","tilleul","riviere","source",
            "pont","moulin","verger","lande","combe","val","mont","colline","falaise","gorge",
            "prairie","champ","marais","tourbiere","brume","plaine","sable","argile","creux","haie"
    };
    private static final String[] ADJ = {
            "bleu","noir","blanc","roux","ocre","gris","dore","argent","mousse","sombre","clair",
            "vif","rouge","fauve","azur","ambre","ivoire","pale","vieux","neuf","brise","sifflant"
    };
    private static final String[] FORM = {"-du-", "-de-la-", "-des-", "-aux-", "-sur-", "-sous-"};

    public static String name(Random r) {
        String a = pick(ROOTS, r);
        String b = pick(ADJ, r);
        String f = pick(FORM, r);
        String base = capitalize(a) + f + b.replace('_','-');
        base = base.replace("--","-");
        return base;
    }

    private static String pick(String[] arr, Random r) { return arr[r.nextInt(arr.length)]; }
    private static String capitalize(String s) { return s.isEmpty()? s : s.substring(0,1).toUpperCase(Locale.ROOT) + s.substring(1); }
}

