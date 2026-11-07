<script setup lang="ts">
import { iconUrl } from "../assets";

defineProps<{
  /** Kit icon of what the row says. Without one the row has no icon column. */
  icon?: string;
  label?: string;
  /** Colour of the label: the grade of an attack, a met requirement. */
  tone?: string;
  /** The label may take more than one line (a sentence rather than a word). */
  wrap?: boolean;
}>();
</script>

<template>
  <div class="panel-row" :class="{ plain: icon === undefined, wrap }">
    <img v-if="icon" :src="iconUrl(icon)" alt="" class="panel-icon" />
    <span class="panel-label" :class="tone"
      ><slot name="label">{{ label }}</slot></span
    >
    <span class="panel-value"><slot /></span>
  </div>
</template>
