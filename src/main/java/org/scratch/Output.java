package org.scratch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Output {

    public List<List<String>> getMatrix() {
        return matrix;
    }

    public void setMatrix(List<List<String>> matrix) {
        this.matrix = matrix;
    }

    public Double getReward() {
        return reward;
    }

    public void setReward(Double reward) {
        this.reward = reward;
    }

    public Map<String, List<String>> getApplied_winning_combinations() {
        return applied_winning_combinations;
    }

    public void setApplied_winning_combinations(Map<String, List<String>> applied_winning_combinations) {
        this.applied_winning_combinations = applied_winning_combinations;
    }

    public String getApplied_bonus_symbol() {
        return applied_bonus_symbol;
    }

    public void setApplied_bonus_symbol(String applied_bonus_symbol) {
        this.applied_bonus_symbol = applied_bonus_symbol;
    }

    List<List<String>> matrix;
    Double reward;
    Map<String,List<String>> applied_winning_combinations=new HashMap<>();
    String applied_bonus_symbol;
}
