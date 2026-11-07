<script setup lang="ts">
import { ref, shallowRef } from "vue";
import { useI18n } from "vue-i18n";
import { iconUrl } from "../assets";
import { AppVersion, LicenceDocs, SourceUrl, type LicenceDoc } from "../about";
import { createLogger } from "@/events/logger";

const emit = defineEmits<{ back: [] }>();

const { t } = useI18n();
const logger = createLogger("AboutPanel", "#e67e22");

/** The licence being read, or `null` for the page with the version and the list of texts. */
const open = shallowRef<LicenceDoc | null>(null);
const body = ref("");
const failed = ref(false);
const scroller = ref<HTMLElement | null>(null);

async function read(doc: LicenceDoc): Promise<void> {
  open.value = doc;
  body.value = "";
  failed.value = false;
  try {
    const text = await doc.load();
    if (open.value === doc) body.value = text;
  } catch (err) {
    logger.error(`Licence ${doc.id} failed to load.`, err);
    if (open.value === doc) failed.value = true;
  }
  scroller.value?.scrollTo({ top: 0 });
}

function back(): void {
  if (open.value === null) {
    emit("back");
  } else {
    open.value = null;
    scroller.value?.scrollTo({ top: 0 });
  }
}
</script>

<template>
  <div class="win about" role="dialog" aria-modal="true" :aria-label="t('handheld.about.title')">
    <div class="thin-plate panel-head">
      <img :src="iconUrl(open ? 'journal' : 'signature')" alt="" class="panel-icon" />
      <span class="panel-head-text">
        <span class="gold panel-title">{{ open ? open.name : `Digivice ${AppVersion}` }}</span>
        <span class="panel-sub">{{
          open ? t("handheld.about.title") : "© 2026 Cristian Campos"
        }}</span>
      </span>
    </div>
    <div ref="scroller" class="scroll panel-body">
      <template v-if="open">
        <p v-if="failed" class="panel-text panel-note">{{ t("handheld.about.failed") }}</p>
        <pre v-else class="licence">{{ body }}</pre>
      </template>
      <template v-else>
        <p class="panel-text">{{ t("handheld.about.free") }}</p>
        <p class="panel-text">{{ t("handheld.about.based") }}</p>
        <div class="panel-section">
          <img :src="iconUrl('search')" alt="" class="panel-icon" />
          <span class="gold">{{ t("handheld.about.source") }}</span>
        </div>
        <p class="panel-text url">{{ SourceUrl }}</p>
        <div class="panel-section">
          <img :src="iconUrl('journal')" alt="" class="panel-icon" />
          <span class="gold">{{ t("handheld.about.texts") }}</span>
        </div>
        <div class="docs">
          <button
            v-for="doc in LicenceDocs"
            :key="doc.id"
            type="button"
            class="btn panel-action"
            @click="read(doc)"
          >
            <img :src="iconUrl('arrow')" alt="" />
            <span>{{ doc.name }}</span>
          </button>
        </div>
      </template>
    </div>
    <div class="panel-foot">
      <button type="button" class="btn-on panel-button" @click="back">
        <img :src="iconUrl('back')" alt="" />
        <span>{{ t("handheld.about.back") }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.about {
  width: 600px;
  height: 520px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 10px;
  line-height: 1.5;
}

.url {
  color: #7fe8ff;
  overflow-wrap: anywhere;
}

.docs {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 2px 6px 6px;
}

/* The licences keep the layout their authors gave them (78 columns), which the pixel font cannot fit on this screen:
   they are the only text in the app set in the system's monospace. */
.licence {
  margin: 0;
  padding: 2px 6px 6px;
  font-family: monospace;
  font-size: 10px;
  line-height: 1.4;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  color: #ffffff;
}
</style>
