package org.scratch;

import java.util.ArrayList;
import java.util.List;

public class WinCombination {
    public List<List<Integer[]>> getLines() {
        return lines;
    }

    public void setLines(List<List<Integer[]>> lines) {
        this.lines = lines;
    }

    public Double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(Double multiplier) {
        this.multiplier = multiplier;
    }

    List<List<Integer[]>> lines = new ArrayList<>();
    Double multiplier;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    String name;
}
