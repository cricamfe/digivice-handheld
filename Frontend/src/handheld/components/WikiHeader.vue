<script setup lang="ts">
import { useI18n } from "vue-i18n";
import { iconUrl } from "../assets";
import { useScreen } from "../router";
import { openSearch } from "../wiki-common";
import ScreenHeader from "./Header.vue";

defineProps<{
  /** Kit icon of the kind of thing the sheet is about. */
  icon: string;
  title: string;
  /** Small text on the right of the plate: the type, the number, the count. */
  detail?: string;
  /** Adds the search button the lists and the enemy sheet have. */
  search?: boolean;
}>();

const { t } = useI18n();
const { go } = useScreen();

function onSearch(): void {
  openSearch();
  go("wikiBuscar");
}
</script>

<template>
  <ScreenHeader back="trail">
    <span class="head">
      <img :src="iconUrl(icon)" alt="" class="head-icon" />
      <span class="gold head-title">{{ title }}</span>
      <span v-if="detail" class="head-detail">{{ detail }}</span>
    </span>
    <template v-if="search" #action>
      <button
        type="button"
        class="btn head-search"
        :aria-label="t('handheld.wiki.searchButton')"
        @click="onSearch"
      >
        <img :src="iconUrl('search')" alt="" class="head-icon" />
      </button>
    </template>
  </ScreenHeader>
</template>

<style scoped>
.head {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.head-icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.head-title {
  min-width: 0;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
}

.head-detail {
  margin-left: auto;
  flex-shrink: 0;
  padding-left: 6px;
  font-size: 10px;
  white-space: nowrap;
}

.head-search {
  width: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
