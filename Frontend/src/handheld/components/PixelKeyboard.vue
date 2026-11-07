<script setup lang="ts">
import { ref, watch } from "vue";
import { useI18n } from "vue-i18n";

const props = withDefaults(
  defineProps<{
    /** The text typed so far. */
    modelValue: string;
    /** The longest text the keyboard writes. */
    maxLength?: number;
  }>(),
  { maxLength: 22 },
);

const emit = defineEmits<{ "update:modelValue": [value: string] }>();

const { t } = useI18n();

/** The three rows of letters of the design; the bottom row has the space bar and Borrar. */
const rows = ["QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM"].map((letters) => [...letters]);

/** The key under a finger, drawn lit like a pressed button of the kit. */
const pressed = ref<string | null>(null);

// The text as the keys have left it. Two taps can land before the parent has handed the first one
// back, so each key builds on this and not on the prop.
const text = ref(props.modelValue);
watch(
  () => props.modelValue,
  (value) => {
    text.value = value;
  },
);

function write(next: string): void {
  text.value = next.slice(0, props.maxLength);
  emit("update:modelValue", text.value);
}

function press(letter: string): void {
  write(text.value + letter);
}

function release(): void {
  pressed.value = null;
}

function frame(key: string): string {
  return pressed.value === key ? "btn-on" : "btn";
}

/**
 * Keeps the keys from taking focus: this keyboard is drawn on a window that must not take input
 * from the game, and nothing here is a text field the system keyboard could belong to.
 */
function keepFocus(event: Event): void {
  event.preventDefault();
}
</script>

<template>
  <section class="keyboard" :aria-label="t('handheld.wiki.keyboard.label')">
    <div v-for="(letters, index) in rows" :key="index" class="row">
      <button
        v-for="letter in letters"
        :key="letter"
        type="button"
        class="key letter"
        :class="frame(letter)"
        @mousedown="keepFocus"
        @pointerdown="pressed = letter"
        @pointerup="release"
        @pointerleave="release"
        @pointercancel="release"
        @click="press(letter)"
      >
        {{ letter }}
      </button>
    </div>
    <div class="row bottom">
      <button
        type="button"
        class="key space"
        :class="frame('space')"
        @mousedown="keepFocus"
        @pointerdown="pressed = 'space'"
        @pointerup="release"
        @pointerleave="release"
        @pointercancel="release"
        @click="press(' ')"
      >
        {{ t("handheld.wiki.keyboard.space") }}
      </button>
      <button
        type="button"
        class="key erase"
        :class="frame('erase')"
        @mousedown="keepFocus"
        @pointerdown="pressed = 'erase'"
        @pointerup="release"
        @pointerleave="release"
        @pointercancel="release"
        @click="write(text.slice(0, -1))"
      >
        {{ t("handheld.wiki.keyboard.erase") }}
      </button>
    </div>
  </section>
</template>

<style scoped>
.keyboard {
  height: 248px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 16px;
}

.row {
  flex: 1;
  min-height: 0;
  display: flex;
  justify-content: center;
  gap: 3px;
}

.key {
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  /* A finger on a key must not start a scroll, a zoom or a selection. */
  touch-action: manipulation;
}

.letter {
  width: 57px;
}

.bottom {
  font-size: 12px;
}

.space {
  flex: 1;
}

.erase {
  width: 150px;
}
</style>
