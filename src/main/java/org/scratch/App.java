package org.scratch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class App {

    public static final String PROBABILITIES = "probabilities";
    public static final String WIN_COMBINATIONS = "win_combinations";
    public static final String STANDARD_SYMBOLS = "standard_symbols";
    public static final String BONUS_SYMBOLS = "bonus_symbols";
    public static final String SYMBOLS = "symbols";
    public static final String ROWS = "rows";
    public static final String COLUMNS = "columns";
    public static final String MULTIPLY_REWARD = "multiply_reward";
    public static final String EXTRA_BONUS = "extra_bonus";
    public static final String LINEAR_SYMBOLS = "linear_symbols";
    public static final String REWARD_MULTIPLIER = "reward_multiplier";
    public static final String COVERED_AREAS = "covered_areas";
    public static final String SAME_SYMBOLS = "same_symbols";

    public static void main(String[] args) throws IOException {
        double bet = Double.parseDouble(args[3]);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = objectMapper.readTree(new File(args[1]));
        int rows = node.get(ROWS).asInt();
        int columns = node.get(COLUMNS).asInt();
        Map<String, Symbol> symbolList = getSymbols(node.get(SYMBOLS));
        //get probabilities
        StandardProbability[][] probabilities = getProbabilities(rows, columns, node.get(PROBABILITIES).get(STANDARD_SYMBOLS));
        StandardProbability bonusProbabilities = getBonusProbabilities(node.get(PROBABILITIES).get(BONUS_SYMBOLS).get(SYMBOLS));
        //get win combinations
        WinCombinationsCount wins = getWinConditionsCount(node.get(WIN_COMBINATIONS));
        WinCombinationsLine winsLines = getWinConditionsLines(node.get(WIN_COMBINATIONS));
        Symbol[][] matrix = new Symbol[rows][columns];
        boolean checkBonus = true;
        Symbol bonusSymbol = null;
        //generate symbols
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                boolean bonus = false;
                if (checkBonus) {
                    bonus = ThreadLocalRandom.current().nextBoolean();
                    if (bonus)
                        checkBonus = false;
                }
                matrix[i][j] = generateSymbol(i, j, bonus, probabilities, bonusProbabilities, symbolList);
                if (bonus) {
                    bonusSymbol = matrix[i][j];
                }
            }
        }

        Output output = new Output();
        //copy matrix to output
        output.matrix = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            List<String> s = new ArrayList<>();
            for (int j = 0; j < columns; j++) {
                s.add(matrix[i][j].getValue());
            }
            output.matrix.add(s);
        }

        //check win combinations
        Map<Symbol, Double> lineWins = checkLines(matrix, winsLines, output);

        AtomicReference<Double> win = new AtomicReference<>((double) 0);
        Map<Symbol, Integer> counter = countSymbols(rows, columns, matrix);
        counter.forEach((s, c) -> {
            Double multiplier;
            multiplier = lineWins.getOrDefault(s, 1.0);
            if (Objects.nonNull(wins.wins.get(c))) {
                if (Objects.nonNull(output.applied_winning_combinations.get(s.getValue())))
                    output.applied_winning_combinations.get(s.getValue()).add("same_symbol_" + c + "_times");
                else {
                    List<String> appliedCombinations = new ArrayList<>();
                    appliedCombinations.add("same_symbol_" + c + "_times");
                    output.applied_winning_combinations.put(s.getValue(), appliedCombinations);
                }

                win.updateAndGet(v -> v + bet * wins.wins.get(c) * multiplier*s.getRewardMultiplier());
            }
        });

        if (win.get() > 0 && Objects.nonNull(bonusSymbol)) {
            output.applied_bonus_symbol = bonusSymbol.getValue();
            if (bonusSymbol.getImpact().equals(MULTIPLY_REWARD)) {
                Symbol finalBonusSymbol = bonusSymbol;
                win.updateAndGet(v -> v * finalBonusSymbol.getRewardMultiplier());
            }
            if (bonusSymbol.getImpact().equals(EXTRA_BONUS)) {
                Symbol finalBonusSymbol = bonusSymbol;
                win.updateAndGet(v -> v + finalBonusSymbol.getExtra());
            }
        }

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < rows; j++) {
                System.out.print(matrix[i][j].getValue() + " ");
            }
            System.out.println();
        }
        output.reward = win.get();
        objectMapper.writeValue(new File("output.json"), output);
        System.out.println(win.get());
        System.out.println(output.applied_winning_combinations);
        System.out.println(output.applied_bonus_symbol);
    }

    private static Map<Symbol, Double> checkLines(Symbol[][] matrix, WinCombinationsLine winsLines, Output output) {
        Map<Symbol, Double> symbolMultiplier = new HashMap<>();
        winsLines.wins.forEach(w -> {
            w.getLines().forEach(l -> {
                Integer[] s0 = l.get(0);
                boolean won = true;
                for (int i = 1; i < l.size(); i++) {
                    Integer[] si = l.get(i);
                    if (!matrix[s0[0]][s0[1]].getValue().equals(matrix[si[0]][si[1]].getValue())) {
                        won = false;
                        break;
                    }

                }
                if (won) {
                    if (symbolMultiplier.containsKey(matrix[s0[0]][s0[1]])) {
                        output.applied_winning_combinations.get(matrix[s0[0]][s0[1]].getValue()).add(w.getName());
                        symbolMultiplier.put(matrix[s0[0]][s0[1]], symbolMultiplier.get(matrix[s0[0]][s0[1]]) * w.getMultiplier());
                    } else {
                        List<String> appliedCombinations = new ArrayList<>();
                        appliedCombinations.add(w.getName());
                        output.applied_winning_combinations.put(matrix[s0[0]][s0[1]].getValue(), appliedCombinations);
                        symbolMultiplier.put(matrix[s0[0]][s0[1]], w.getMultiplier());
                    }
                }
            });
        });
        return symbolMultiplier;
    }

    private static WinCombinationsLine getWinConditionsLines(JsonNode winCombinations) {
        WinCombinationsLine winCombinationsLine = new WinCombinationsLine();

        winCombinations.fields().forEachRemaining(p -> {
            if (p.getValue().get("when").asText().equals(LINEAR_SYMBOLS)) {
                WinCombination winCombination = new WinCombination();
                winCombination.setMultiplier(p.getValue().get(REWARD_MULTIPLIER).asDouble());
                winCombination.setName(p.getKey());
                JsonNode areas = p.getValue().get(COVERED_AREAS);

                areas.elements().forEachRemaining(e -> {
                    List<Integer[]> line = new ArrayList<>();
                    e.elements().forEachRemaining(e2 -> {
                        Integer[] slot = new Integer[2];
                        slot[0] = Integer.valueOf(e2.asText().substring(0, 1));
                        slot[1] = Integer.valueOf(e2.asText().substring(2));
                        line.add(slot);
                    });
                    winCombination.getLines().add(line);
                });
                winCombinationsLine.wins.add(winCombination);
            }
        });
        return winCombinationsLine;
    }

    private static Map<Symbol, Integer> countSymbols(int rows, int columns, Symbol[][] matrix) {
        Map<Symbol, Integer> counter = new HashMap<>();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (counter.containsKey(matrix[i][j])) {
                    counter.put(matrix[i][j], counter.get(matrix[i][j]) + 1);
                } else {
                    counter.put(matrix[i][j], 1);
                }
            }
        }
        return counter;
    }

    private static WinCombinationsCount getWinConditionsCount(JsonNode winCombinations) {
        WinCombinationsCount winCombinationsCount = new WinCombinationsCount();

        winCombinations.fields().forEachRemaining(p -> {
            if (p.getValue().get("when").asText().equals(SAME_SYMBOLS)) {
                winCombinationsCount.wins.put(p.getValue().get("count").asInt(), p.getValue().get(REWARD_MULTIPLIER).asDouble());
            }
        });
        return winCombinationsCount;
    }


    private static Symbol generateSymbol(int i, int j, boolean bonus, StandardProbability[][] probabilities, StandardProbability bonusProbabilities, Map<String, Symbol> symbolList) {
        if (bonus) {
            double checkBonus = Math.random() * bonusProbabilities.sum;

            Map.Entry<String, Integer> bonusResult = bonusProbabilities.probability.entrySet().stream().sorted(Map.Entry.comparingByValue()).filter(p -> checkBonus < p.getValue().doubleValue()
            ).findFirst().get();
            return new Symbol(symbolList.get(bonusResult.getKey()));
        }
        double check = Math.random() * probabilities[i][j].sum;

        Map.Entry<String, Integer> result = probabilities[i][j].probability.entrySet().stream().sorted(Map.Entry.comparingByValue()).filter(p -> check < p.getValue().doubleValue()
        ).findFirst().get();
        return new Symbol(symbolList.get(result.getKey()));
    }

    private static StandardProbability getBonusProbabilities(JsonNode probabilities) {
        StandardProbability bonusProbabilities = new StandardProbability();
        AtomicInteger sum = new AtomicInteger(0);
        probabilities.fields().forEachRemaining(p -> {
            bonusProbabilities.probability.put(p.getKey(), sum.addAndGet(p.getValue().asInt()));
        });
        bonusProbabilities.sum = sum.get();
        return bonusProbabilities;
    }

    private static StandardProbability[][] getProbabilities(int rows, int columns, JsonNode probabilities) {
        StandardProbability[][] probabilityMap = new StandardProbability[rows][columns];
        AtomicInteger sum = new AtomicInteger(0);
        probabilities.elements().forEachRemaining(p -> {
            StandardProbability sp = new StandardProbability();
            JsonNode s = p.get(SYMBOLS);
            s.fields().forEachRemaining(symbols2 -> {

                sp.probability.put(symbols2.getKey(), sum.addAndGet(symbols2.getValue().asInt()));
            });
            sp.sum = sum.get();
            sum.getAndSet(0);
            probabilityMap[p.get("row").asInt()][p.get("column").asInt()] = sp;
        });

        return probabilityMap;
    }

    private static Map<String, Symbol> getSymbols(JsonNode symbols) {
        Map<String, Symbol> symbolList = new HashMap<>();
        symbols.fields().forEachRemaining(j -> {
            JsonNode value = j.getValue();
            symbolList.put(j.getKey(), new Symbol(j.getKey(), value.get(REWARD_MULTIPLIER), value.get("type"), value.get("extra"), value.get("impact")));
        });
        return symbolList;
    }
}
