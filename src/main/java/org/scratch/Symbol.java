package org.scratch;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public class Symbol {
 public String getValue() {
  return value;
 }

 public Integer getRewardMultiplier() {
  return rewardMultiplier;
 }

 public String getType() {
  return type;
 }

 public String getImpact() {
  return impact;
 }

 public Integer getExtra() {
  return extra;
 }

 // @Json("reward_multiplier")
    private String value;
    @JsonProperty("reward_multiplier")
    private Integer rewardMultiplier;
    @JsonProperty("type")
    private String type;
    @JsonProperty("impact")
    private String impact;
 @JsonProperty("extra")
 private Integer extra;

 public Symbol(Integer rewardMultiplier, String type, Integer extra, String impact) {
  this.rewardMultiplier=rewardMultiplier;
  this.type=type;
  this.extra=extra;
  this.impact=impact;
 }

 public Symbol(String value,JsonNode rewardMultiplier, JsonNode type, JsonNode extra, JsonNode impact) {
  this.value=value;
  this.rewardMultiplier= Objects.isNull(rewardMultiplier)?null:rewardMultiplier.asInt();
  this.type=Objects.isNull(type)?null:type.asText();
  this.extra=Objects.isNull(extra)?null:extra.asInt();
  this.impact=Objects.isNull(impact)?null:impact.asText();
 }

 public Symbol(Symbol a) {
  this.value=a.value;
  this.rewardMultiplier= a.rewardMultiplier;
  this.type=a.type;
  this.extra=a.extra;
  this.impact=a.impact;
 }

 @Override
 public boolean equals(Object o) {
  if (this == o) return true;
  if (o == null || getClass() != o.getClass()) return false;
  Symbol symbol = (Symbol) o;
  return Objects.equals(value, symbol.value);
 }

 @Override
 public int hashCode() {
  return Objects.hash(value);
 }
}
