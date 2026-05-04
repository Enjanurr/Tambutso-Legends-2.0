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
            "Lawaan",                                // Stop 1
            "Tabunok Public Market",                 // Stop 2
            "Bulacao",                               // Stop 3
            "Pardo Church",                          // Stop 4
            "Mambaling",                             // Stop 5
            "Kinasang-an",                           // Stop 6
            "Shopwise",                              // Stop 7
            "CIT-University",                        // Stop 8
            "USJR",                                  // Stop 9
            "E-Mall (Elizabeth Mall)",               // Stop 10
            "Mango Avenue",                          // Stop 11
            "Fuente Osmeña Circle",                  // Stop 12
            "Robinsons Fuente",                      // Stop 13
            "Cebu Doctor’s University",              // Stop 14
            "Ayala Center Cebu",                     // Stop 15
            "SM City Cebu",                          // Stop 16
            "North Bus Terminal",                    // Stop 17
            "Colon Street",                          // Stop 18
            "Carbon Market",                         // Stop 19
            "San Nicolas Church"                     // Stop 20
    );

    private static final List<String> LEVEL_3_STOPS = Arrays.asList(
            "Plaza Independencia",                   // Stop 1 (replaces Carbon Market)
            "Fort San Pedro",                        // Stop 2
            "Lapu-Lapu Monument",                    // Stop 3
            "Magellan’s Cross",                      // Stop 4
            "Basilica Minore del Santo Niño",        // Stop 5
            "Cebu Metropolitan Cathedral",           // Stop 6
            "Santo Rosario Parish Church",           // Stop 7
            "University of San Carlos",              // Stop 8
            "University of the Visayas",             // Stop 9
            "Cebu City Hall",                        // Stop 10
            "Pier 1",                                // Stop 11 (replaces Colon Street)
            "Pier 3",                                // Stop 12
            "Osmeña Boulevard",                      // Stop 13 (replaces Fuente)
            "Ramos Street",                          // Stop 14 (replaces Robinsons)
            "Cebu Provincial Capitol",               // Stop 15 (replaces Cebu Doctor's)
            "N. Escario Street",                     // Stop 16
            "Camputhaw",                             // Stop 17
            "Gorordo Avenue",                        // Stop 18 (replaces Ayala)
            "Mabolo Church",                         // Stop 19
            "Archbishop Reyes Avenue",               // Stop 20
            "Cebu Business Park",                    // Stop 21
            "JY Square Mall",                        // Stop 22
            "Waterfront Hotel",                      // Stop 23
            "Apas",                                  // Stop 24
            "IT Park"                                // Stop 25
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
