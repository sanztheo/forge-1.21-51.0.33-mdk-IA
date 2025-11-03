package net.frealac.iamod.common.story;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;

import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Phase 1 deterministic generator seeded by worldSeed ^ villagerUUID bits ^ coarse position bucket.
 */
public class StoryGenerator {
    private static final List<String> CULTURES = List.of(
            "plaine_nord", "colline_est", "marais_ouest", "montagne_sud",
            "riviere_ouest", "forêt_grise", "cote_vent", "hauts_plateaux", "delta_saumatre",
            "caverne_silex", "lande_cendree", "archipel_brisé", "steppe_doree", "toundra_bleue",
            "desert_ambre", "bassin_azur", "falaises_ocre", "verger_mousse", "tourbiere_fauve", "plaine_argile");
    private static final List<String> PROF_ROOTS = List.of("forg", "bouch", "moul", "tuil", "cord", "pêch", "chass", "terr", "charpent", "maç", "tisser", "menuis", "meun", "briol", "lapidar", "herbor", "apicul", "vigner", "pot", "charr", "tann", "sell", "criv");
    private static final List<String> PROF_SUFFIX = List.of("er", "eur", "ier", "iste", "on", "eur", "euse", "rin", "in");
    private static final List<String> TRAITS = List.of(
            "généreux", "prudent", "rancunier", "loyal", "superstitieux", "méticuleux", "bavard", "timide",
            "courageux", "mélancolique", "ambitieux", "farceur", "patient", "curieux", "altruiste", "sarcastique",
            "susceptible", "perfectionniste", "taciturne", "énergique", "rêveur", "stoïque", "franc", "calculateur",
            "créatif", "réservé", "empathique", "rigide", "flexible", "maniaque", "désinvolte", "moqueur", "protecteur");

    public static VillagerStory generate(ServerLevel level, Villager villager) {
        long worldSeed = level.getSeed();
        long uuidBits = villager.getUUID().getLeastSignificantBits() ^ villager.getUUID().getMostSignificantBits();
        BlockPos pos = villager.blockPosition();
        long bucket = ((long) (pos.getX() >> 4) << 32) ^ (pos.getZ() >> 4);
        long seed = worldSeed ^ uuidBits ^ bucket;
        Random r = new Random(seed);

        VillagerStory s = new VillagerStory();
        s.worldSeed = worldSeed;
        s.uuid = villager.getUUID();
        s.villageId = String.format(Locale.ROOT, "C%dxC%d", (pos.getX() >> 4), (pos.getZ() >> 4));

        s.cultureId = pick(CULTURES, r);
        s.villageName = PlaceNameMaker.name(new Random(seed ^ 0x9E3779B97F4A7C15L));
        s.sex = r.nextBoolean() ? "male" : "female";
        s.nameGiven = NameMaker.given(r, s.sex);
        s.nameFamily = NameMaker.family(r);
        s.ageYears = 16 + r.nextInt(60); // 16..75

        s.profession = makeProfession(r);
        s.traits.add(VarietyUtil.intensify(r, pick(TRAITS, r)));
        VarietyUtil.addDistinct(s.traits, TRAITS, r, 2 + r.nextInt(2));

        // Logical family names only (Phase 1); no world entity bindings yet
        if (r.nextDouble() < 0.85) s.parents.add(NameMaker.given(r, r.nextBoolean()?"male":"female") + " " + s.nameFamily);
        if (r.nextDouble() < 0.85) s.parents.add(NameMaker.given(r, r.nextBoolean()?"male":"female") + " " + s.nameFamily);
        int childCount = r.nextInt(3); // 0..2 children
        for (int i = 0; i < childCount; i++) {
            s.children.add(NameMaker.given(r, r.nextBoolean()?"male":"female") + " " + s.nameFamily);
        }
        if (r.nextDouble() < 0.4) s.siblings.add(NameMaker.given(r, r.nextBoolean()?"male":"female") + " " + s.nameFamily);

        // Memories (short)
        s.memories.add("a aidé au marché du village");
        if (r.nextBoolean()) s.memories.add("a réparé le puits avec les voisins");
        if (r.nextDouble() < 0.3) s.memories.add("a survécu à une mauvaise saison");

        // Phase 2: health & psychology basic generation
        VillagerStory.Health h = new VillagerStory.Health();
        if (r.nextDouble() < 0.2) h.allergies.add("pollen");
        if (r.nextDouble() < 0.15) { VillagerStory.ScaleItem ph = new VillagerStory.ScaleItem(); ph.type = "orage"; ph.severity = 0.4 + r.nextDouble() * 0.4; h.phobias.add(ph); }
        if (r.nextDouble() < 0.25) { VillagerStory.Wound w = new VillagerStory.Wound(); w.type = "coupure"; w.date = (200 + r.nextInt(60)) + "-" + (1 + r.nextInt(12)); w.severity = r.nextDouble(); w.permanent = r.nextBoolean(); h.wounds.add(w); }
        h.stamina = 0.4 + r.nextDouble() * 0.5;
        h.painTolerance = 0.3 + r.nextDouble() * 0.6;
        h.sleepQuality = 0.3 + r.nextDouble() * 0.6;
        s.health = h;

        VillagerStory.Psychology psy = new VillagerStory.Psychology();
        if (r.nextDouble() < 0.25) {
            VillagerStory.TraumaEvent te = new VillagerStory.TraumaEvent();
            te.id = "t1";
            te.type = r.nextBoolean()?"incendie":"accident_travail";
            te.ageAt = 10 + r.nextInt(Math.max(1, s.ageYears - 10));
            te.description = te.type.equals("incendie")?"a perdu des biens" : "blessure au chantier";
            te.severity = 0.4 + r.nextDouble() * 0.5;
            te.tags.add(te.type);
            psy.trauma.events.add(te);
            psy.trauma.coping.add(r.nextBoolean()?"travail":"isolement");
        }
        psy.moodBaseline = -0.2 + r.nextDouble()*0.4; // around neutral
        psy.stress = r.nextDouble()*0.5;
        psy.resilience = 0.4 + r.nextDouble()*0.5;
        if (r.nextDouble() < 0.3) psy.fears.add("orage");
        if (r.nextDouble() < 0.4) psy.hopes.add("meilleure récolte");
        s.psychology = psy;

        // Phase 2: timeline & memoriesDetailed — full life coverage from current age down to childhood
        s.lifeTimeline.clear();
        java.util.HashSet<Integer> usedAges = new java.util.HashSet<>();
        java.util.function.Consumer<VillagerStory.LifeEvent> add = ev -> { if (ev.age >= 4 && ev.age <= s.ageYears && usedAges.add(ev.age)) s.lifeTimeline.add(ev); };

        // Childhood marker
        VillagerStory.LifeEvent enf = new VillagerStory.LifeEvent();
        enf.age = 6 + r.nextInt(Math.max(1, Math.min(5, s.ageYears-6)));
        enf.type = "enfance_marquee";
        enf.place = s.villageName;
        enf.details = r.nextBoolean()?"cabane au bord du champ":"premières corvées";
        add.accept(enf);

        // Apprenticeship
        VillagerStory.LifeEvent app = new VillagerStory.LifeEvent();
        app.age = Math.min(s.ageYears, 12 + r.nextInt(7)); // 12–18
        app.type = "apprentissage";
        app.place = s.villageName;
        app.details = r.nextBoolean()?"chez l’oncle":"à l’atelier communal";
        add.accept(app);

        // Optional migration
        if (r.nextDouble() < 0.5 && s.ageYears > 18) {
            VillagerStory.LifeEvent mig = new VillagerStory.LifeEvent();
            mig.age = 16 + r.nextInt(Math.max(1, s.ageYears - 15));
            mig.type = "migration";
            mig.place = s.villageName;
            mig.details = r.nextBoolean()?"vers un hameau voisin":"après une mauvaise saison";
            add.accept(mig);
        }

        // Optional promotion or milestone
        if (r.nextDouble() < 0.6 && s.ageYears > 20) {
            VillagerStory.LifeEvent prom = new VillagerStory.LifeEvent();
            prom.age = 20 + r.nextInt(Math.max(1, s.ageYears - 19));
            prom.type = "promotion";
            prom.place = s.villageName;
            prom.details = r.nextBoolean()?"après des mois d’efforts":"grâce à un chantier réussi";
            add.accept(prom);
        }

        // Random incidents
        int extras = 2 + r.nextInt(3);
        for (int i = 0; i < extras; i++) {
            VillagerStory.LifeEvent le = new VillagerStory.LifeEvent();
            le.age = 4 + r.nextInt(Math.max(1, s.ageYears - 3));
            le.type = r.nextBoolean()?"accident":"incident_village";
            le.place = s.villageName;
            le.details = le.type.equals("accident") ? (r.nextBoolean()?"outil défectueux":"glissade sur échafaud")
                    : (r.nextBoolean()?"fête troublée":"litige au marché");
            add.accept(le);
        }
        int mcount = 1 + r.nextInt(3);
        for (int i = 0; i < mcount; i++) {
            VillagerStory.MemoryEntry me = new VillagerStory.MemoryEntry();
            me.id = "m" + (i+1);
            me.date = (210 + r.nextInt(50)) + "-" + (1 + r.nextInt(12));
            me.topic = r.nextBoolean()?"marché":"chantier";
            me.moodDelta = -0.3 + r.nextDouble()*0.6;
            me.importance = 0.2 + r.nextDouble()*0.6;
            me.tags.add(me.topic);
            s.memoriesDetailed.add(me);
        }

        s.routines = new VillagerStory.Routines();
        s.routines.daily.add("travail");
        s.routines.daily.add("repos");
        if (r.nextDouble() < 0.5) s.routines.daily.add("commérage");

        s.preferences = new VillagerStory.Preferences();
        s.preferences.likes.add("pain");
        if (r.nextDouble() < 0.4) s.preferences.dislikes.add("boue");
        s.preferences.foods.favorite.add("ragoût");

        // Bio brief (one-liner)
        String t1 = s.traits.isEmpty() ? "travailleur" : s.traits.get(0);
        s.bioBrief = s.nameGiven + " " + s.nameFamily + ", " + t1 + "·e, " + s.profession + ", culture " + s.cultureId + ".";
        return s;
    }

    private static <T> T pick(List<T> list, Random r) { return list.get(r.nextInt(list.size())); }
    private static String makeProfession(Random r) {
        if (r.nextDouble() < 0.6) return pick(List.of(
                "farmer", "fisherman", "shepherd", "fletcher", "librarian", "cartographer",
                "cleric", "armorer", "weaponsmith", "toolsmith", "mason", "leatherworker", "butcher"), r);
        String root = pick(PROF_ROOTS, r);
        String suf = pick(PROF_SUFFIX, r);
        String prof = root + suf;
        // gender agreement adjustments minimal
        if (suf.equals("euse")) prof = root + "eur"; // default to masculine form
        return prof;
    }
}
