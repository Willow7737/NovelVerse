package com.novelverse.app.domain.models;

public class ReaderLevel {
    public enum Level {
        BOOKWORM(1, "Bookworm", 0, 9),
        SCHOLAR(2, "Scholar", 10, 49),
        SAGE(3, "Sage", 50, 99),
        LOREKEEPER(4, "Lorekeeper", 100, 499),
        ORACLE(5, "Oracle", 500, Integer.MAX_VALUE);

        public final int tier, chaptersRequired, chaptersMax;
        public final String name;
        Level(int tier, String name, int req, int max) {
            this.tier = tier; this.name = name;
            this.chaptersRequired = req; this.chaptersMax = max;
        }
    }

    public static Level fromChaptersCompleted(int chapters) {
        for (Level l : Level.values()) {
            if (chapters >= l.chaptersRequired && chapters <= l.chaptersMax) return l;
        }
        return Level.ORACLE;
    }

    public static float progressToNextLevel(int chapters, Level current) {
        if (current == Level.ORACLE) return 1f;
        Level[] levels = Level.values();
        Level next = levels[current.ordinal() + 1];
        int range = next.chaptersRequired - current.chaptersRequired;
        int done  = chapters - current.chaptersRequired;
        return Math.min(1f, (float) done / range);
    }
}
