package levels;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for each game level.
 * Contains all level-specific settings: loops, requirements, stop names, etc.
 */
public class LevelConfig {

    public final int levelId;
    public final int maxWorldLoops;
    public final int requiredPassengers;
    public final int requiredFare;
    public final int stopCount;
    public final String bossFolder;
    public final List<String> stopNames;

    // Level 1: Naga to Upper Linao (existing stops)
    private static final List<String> LEVEL_1_STOPS = Arrays.asList(
            "Naga City Jeepney Terminal",           // Stop 1
            "KEPCO",                                 // Stop 2
            "Inoburan Stop",                         // Stop 3
            "Tinaan Crossing",                       // Stop 4
            "Langtad Stop",                          // Stop 5
            "Cantao-an Junction",                    // Stop 6
            "Tunghaan Stop ",         // Stop 7
            "Calajo-an Stop",                        // Stop 8
            "Tulic Stop",                            // Stop 9
            "Minglanilla Public Market", // Stop 10
            "Poblacion Ward",                        // Stop 11
            "Tubod Crossing",                        // Stop 12
            "Pakigne Junction",                      // Stop 13
            "Cadulawan Area",                        // Stop 14
            "Upper Linao Stop"                       // Stop 15
    );

    // Level 2: Extended route (20 stops)
    private static final List<String> LEVEL_2_STOPS = Arrays.asList(
        "Lawaan",
        "Tabunok Public Market",
        "Bulacao",
        "Pardo Church",
        "USJR",
        "Shopwise",
        "Kinasang-an",
        "Mambaling",
        "CIT-University",
        "E-Mall",
        "Fuente Osmeña Circle",
        "Robinsons Fuente",
        "Cebu Doctor's University Hospital",
        "Mango Avenue",
        "Ayala Center Cebu",
        "IT Park",
        "SM City Cebu",
        "North Bus Terminal",
        "Colon Street",
        "Carbon Market"
    );

    // Level 3: Full city tour (25 stops)
    private static final List<String> LEVEL_3_STOPS = Arrays.asList(
        "Colon Street",
        "Cebu Metropolitan Cathedral",
        "Basilica Minore del Santo Niño",
        "Magellan's Cross",
        "Cebu City Hall",
        "Pier 1",
        "Pier 3",
        "SM City Cebu",
        "Mabolo Church",
        "Archbishop Reyes Avenue",
        "Ayala Center Cebu",
        "Cebu Business Park",
        "Gorordo Avenue",
        "JY Square Mall",
        "IT Park",
        "Fuente Osmeña Circle",
        "Robinsons Fuente",
        "Cebu Doctor's University Hospital",
        "Osmeña Boulevard",
        "Carbon Market",
        "University of San Carlos",
        "University of the Visayas",
        "Santo Rosario Parish Church",
        "Plaza Independencia",
        "Fort San Pedro"
    );

    public LevelConfig(int levelId, int maxWorldLoops, int requiredPassengers,
                       int requiredFare, int stopCount, String bossFolder,
                       List<String> stopNames) {
        this.levelId = levelId;
        this.maxWorldLoops = maxWorldLoops;
        this.requiredPassengers = requiredPassengers;
        this.requiredFare = requiredFare;
        this.stopCount = stopCount;
        this.bossFolder = bossFolder;
        this.stopNames = stopNames;
    }

    /**
     * Get the stop name for a given stop number (1-based)
     */
    public String getStopName(int stopNumber) {
        if (stopNumber < 1 || stopNumber > stopNames.size()) {
            return "Unknown Stop";
        }
        return stopNames.get(stopNumber - 1);
    }

    // Pre-defined level configurations
    public static final LevelConfig LEVEL_1 = new LevelConfig(
        1, 15, 6, 500, 15, "LevelOne", LEVEL_1_STOPS
    );

    public static final LevelConfig LEVEL_2 = new LevelConfig(
        2, 20, 12, 1000, 20, "LevelTwo", LEVEL_2_STOPS
    );

    public static final LevelConfig LEVEL_3 = new LevelConfig(
        3, 25, 16, 2000, 25, "LevelThree", LEVEL_3_STOPS
    );

    public static final LevelConfig[] ALL_LEVELS = { LEVEL_1, LEVEL_2, LEVEL_3 };

    /**
     * Get level config by ID (1-based)
     */
    public static LevelConfig getLevel(int levelId) {
        for (LevelConfig level : ALL_LEVELS) {
            if (level.levelId == levelId) {
                return level;
            }
        }
        return LEVEL_1; // Default to level 1
    }
}
