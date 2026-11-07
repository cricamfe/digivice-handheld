<script setup lang="ts">
import { useI18n } from "vue-i18n";
import { iconUrl } from "../assets";
import { useScreen, type ScreenName } from "../router";

defineProps<{
  title?: string;
  /** Small text on the right of the plate. */
  detail?: string;
  /**
   * Screen the back button returns to. `"trail"` returns to wherever the player came from (the Wiki,
   * where a sheet is reached from a list, the search or another screen). Without it there is no back button.
   */
  back?: ScreenName | "trail";
}>();

// The rule asks for two words; the file keeps the name the design and the task give it.
defineOptions({ name: "ScreenHeader" });

const { t } = useI18n();
const { go, back: goBack } = useScreen();

function onBack(back: ScreenName | "trail"): void {
  if (back === "trail") {
    goBack();
  } else {
    go(back);
  }
}
</script>

<template>
  <header class="header">
    <button
      v-if="back"
      type="button"
      class="btn header-back"
      :aria-label="t('handheld.back')"
      @click="onBack(back)"
    >
      <img :src="iconUrl('back')" alt="" class="header-icon" />
    </button>
    <div class="plate header-plate">
      <slot>
        <span class="gold header-title">{{ title }}</span>
        <span v-if="detail" class="header-detail">{{ detail }}</span>
      </slot>
    </div>
    <slot name="action"></slot>
  </header>
</template>

<style scoped>
.header {
  height: 44px;
  flex-shrink: 0;
  display: flex;
  gap: 4px;
}

.header-back {
  width: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-icon {
  width: 22px;
  height: 22px;
}

.header-plate {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-title {
  font-size: 12px;
}

.header-detail {
  font-size: 10px;
}
</style>
