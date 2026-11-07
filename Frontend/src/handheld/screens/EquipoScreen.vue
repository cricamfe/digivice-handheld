<script setup lang="ts">
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import FigurePanel from "../components/FigurePanel.vue";
import FloatingPanel, { type PanelAnchor } from "../components/FloatingPanel.vue";
import PanelRow from "../components/PanelRow.vue";
import PartyColumn from "../components/PartyColumn.vue";
import StatusBar from "../components/StatusBar.vue";
import { iconUrl } from "../assets";
import { useScreen } from "../router";
import { buildSheet, type Translate } from "../party-sheet";
import { usePanelHost } from "../use-panel-host";
import { DvexpBarSegments } from "@/constants/digievolution.constant";
import type { EquipmentSlot } from "@/types/equipment-slot.type";
import { useGameState } from "@/composables/use-game-state";
import { PartyPresenter } from "@/presenters/party/party.presenter";

/** The icon an empty slot has: what the pieces that go there raise. */
const slotIcons: Record<EquipmentSlot, string> = {
  head: "defense",
  body: "defense",
  right: "strength",
  left: "strength",
  accessory1: "charisma",
  accessory2: "charisma",
};

const { t } = useI18n();
const { go, param } = useScreen();
const gameState = useGameState();
const translate: Translate = (key, named) => t(key, named ?? {});

const mapId = computed(() => gameState.value.player.mapId);
const slots = computed(() => PartyPresenter.getFilledSlots(gameState.value.party.slots));

// The map screens open the party member that was tapped.
const selectedIndex = ref<number | null>(typeof param.value === "number" ? param.value : null);
/** The chosen slot, or the first one while nothing was chosen or the chosen one left the party. */
const selectedSlot = computed(() => {
  return slots.value.find((slot) => slot.index === selectedIndex.value) ?? slots.value[0] ?? null;
});

const sheet = computed(() => {
  const slot = selectedSlot.value;
  return slot === null ? null : buildSheet(slot, mapId.value, translate);
});

/** The detail a tap opened, floating beside the thing it is about; the thing carries `data-panel-anchor`. */
type PanelKind = "equipment" | "stat" | "portrait";
const panel = ref<{ kind: PanelKind; key: string; anchor: PanelAnchor } | null>(null);
const { host, hostSize, anchorOf } = usePanelHost();

/** Tapping a detail opens its panel; tapping the same one again closes it. */
function togglePanel(kind: PanelKind, key: string, element: unknown): void {
  if (panel.value?.kind === kind && panel.value.key === key) {
    panel.value = null;
    return;
  }

  const anchor = anchorOf(element);
  panel.value = anchor === null ? null : { kind, key, anchor };
}

const panelEquipment = computed(() =>
  panel.value?.kind === "equipment"
    ? (sheet.value?.equipment.find((item) => item.key === panel.value?.key) ?? null)
    : null,
);
const panelStat = computed(() =>
  panel.value?.kind === "stat"
    ? ([...(sheet.value?.attributes ?? []), ...(sheet.value?.elements ?? [])].find(
        (stat) => stat.key === panel.value?.key,
      ) ?? null)
    : null,
);

function pickSlot(index: number, button: HTMLElement): void {
  const slot = selectedSlot.value;
  if (slot !== null && slot.index === index) {
    // The selected portrait explains the Digimon's state instead of selecting it again.
    togglePanel("portrait", String(index), button);
    return;
  }

  selectedIndex.value = index;
  panel.value = null;
}

function openTechniques(digievolutionId: number): void {
  go("tecnicas", { digimonSlot: selectedSlot.value!.index, digievolutionId });
}
</script>

<template>
  <div class="view">
    <section v-if="sheet && selectedSlot" ref="host" class="equipo">
      <PartyColumn
        :slots="slots"
        :selected="selectedSlot.index"
        :map-id="mapId"
        show-names
        show-levels
        anchored
        @pick="pickSlot"
      />

      <div class="detail">
        <div class="win profile">
          <div class="profile-head">
            <span class="drop profile-name">{{ sheet.name }}</span>
            <span class="profile-meta">
              <span class="gold">{{ t("digimon.lv") }} {{ sheet.level }}</span>
              <span class="profile-tp">
                <img :src="iconUrl('training')" :alt="t('digimon.tp')" class="icon" />{{ sheet.tp }}
              </span>
            </span>
          </div>
          <div v-for="bar in sheet.bars" :key="bar.kind" class="bar meter">
            <div class="meter-fill" :class="bar.kind" :style="{ width: bar.percent }"></div>
            <span class="outline meter-label">{{ bar.label }}</span>
            <span class="outline meter-text">{{ bar.text }}</span>
          </div>
        </div>

        <div class="columns">
          <div class="left">
            <span class="gold block-title">{{ t("digimon.digievolutions") }}</span>
            <template v-for="evolution in sheet.digievolutions" :key="evolution.key">
              <button
                v-if="evolution.digievolutionId !== null"
                type="button"
                class="evolution"
                :class="[
                  evolution.isActive ? 'thin-on active' : 'thin',
                  { starts: evolution.startsBattle },
                ]"
                @click="openTechniques(evolution.digievolutionId)"
              >
                <img
                  v-if="evolution.startsBattle"
                  :src="iconUrl('battle')"
                  :alt="t('handheld.party.startsBattle')"
                  class="icon starts-battle"
                />
                <span class="evolution-text">
                  <span class="evolution-line" :class="evolution.isActive ? 'gold' : 'drop'">
                    <span>{{ evolution.name }}</span>
                    <span>{{ evolution.level }}</span>
                  </span>
                  <span class="segments">
                    <span
                      v-for="segment in DvexpBarSegments"
                      :key="segment"
                      class="segment"
                      :class="{ filled: segment <= evolution.filled }"
                    ></span>
                  </span>
                </span>
              </button>
              <div v-else class="thin evolution evolution-empty">
                <span class="dim evolution-line">{{ t("digimon.states.empty") }}</span>
              </div>
            </template>

            <span class="gold block-title">{{ t("handheld.party.equipment") }}</span>
            <div class="win equipment">
              <button
                v-for="item in sheet.equipment"
                :key="item.key"
                type="button"
                class="equipment-item"
                :class="{ equipped: item.isEquipped }"
                data-panel-anchor
                @click="togglePanel('equipment', item.key, $event.currentTarget)"
              >
                {{ item.label }}
              </button>
            </div>
          </div>

          <div class="win stats">
            <div class="stats-column">
              <button
                v-for="stat in sheet.attributes"
                :key="stat.key"
                type="button"
                class="stat"
                :aria-label="stat.label"
                data-panel-anchor
                @click="togglePanel('stat', stat.key, $event.currentTarget)"
              >
                <img :src="iconUrl(stat.key)" alt="" class="icon" />
                <span class="stat-text">
                  <span class="drop">{{ stat.value }}</span>
                  <span class="gold stat-bonus">{{ stat.bonus > 0 ? `+${stat.bonus}` : "" }}</span>
                </span>
              </button>
            </div>
            <div class="stats-column">
              <button
                v-for="stat in sheet.elements"
                :key="stat.key"
                type="button"
                class="stat"
                :aria-label="stat.label"
                data-panel-anchor
                @click="togglePanel('stat', stat.key, $event.currentTarget)"
              >
                <img :src="iconUrl(stat.key)" alt="" class="icon" />
                <span class="stat-text">
                  <span class="drop">{{ stat.value }}</span>
                  <span class="gold stat-bonus">{{ stat.bonus > 0 ? `+${stat.bonus}` : "" }}</span>
                </span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- A piece of equipment: what it is, what it changes, who can wear it. An empty slot only says so. -->
      <FloatingPanel
        v-if="panel !== null && panelEquipment !== null"
        :anchor="panel.anchor"
        :host-width="hostSize.width"
        :host-height="hostSize.height"
        :width="panelEquipment.detail ? 300 : 240"
        :label="panelEquipment.label"
        :title="panelEquipment.detail?.name ?? panelEquipment.label"
        :subtitle="panelEquipment.detail?.typeLabel ?? t('handheld.equipment.empty')"
        :icon="panelEquipment.detail?.icon ?? slotIcons[panelEquipment.key]"
        @close="panel = null"
      >
        <template v-if="panelEquipment.detail">
          <div v-if="panelEquipment.detail.attributes.length > 0" class="panel-rows">
            <PanelRow
              v-for="attribute in panelEquipment.detail.attributes"
              :key="attribute.key"
              :icon="attribute.key"
              :label="attribute.label"
            >
              <span :class="attribute.isPenalty ? 'down' : 'gold'">{{ attribute.value }}</span>
            </PanelRow>
          </div>
          <p v-else class="panel-text panel-note">{{ t("handheld.equipment.noAttributes") }}</p>
          <PanelRow icon="party" :label="t('handheld.equipment.equippedBy')">
            <span v-if="panelEquipment.detail.forAll" class="ice">{{
              t("handheld.equipment.everyone")
            }}</span>
            <span v-else-if="panelEquipment.detail.equipable.length === 0" class="dim">{{
              t("handheld.equipment.nobody")
            }}</span>
          </PanelRow>
          <p
            v-if="!panelEquipment.detail.forAll && panelEquipment.detail.equipable.length > 0"
            class="panel-text ice"
          >
            {{ panelEquipment.detail.equipable.join(", ") }}
          </p>
        </template>
        <template v-if="panelEquipment.detail?.hasWiki" #footer>
          <button
            type="button"
            class="btn panel-button"
            @click="go('wikiEquipo', panelEquipment.detail.id)"
          >
            {{ t("handheld.equipment.wiki") }}
            <img :src="iconUrl('arrow')" alt="" />
          </button>
        </template>
      </FloatingPanel>

      <FigurePanel
        v-if="panel !== null && panelStat !== null"
        :detail="panelStat.detail"
        :anchor="panel.anchor"
        :host-width="hostSize.width"
        :host-height="hostSize.height"
        @close="panel = null"
      />

      <!-- The partner's state: the plain one, or each alteration it suffers. -->
      <FloatingPanel
        v-if="panel?.kind === 'portrait'"
        :anchor="panel.anchor"
        :host-width="hostSize.width"
        :host-height="hostSize.height"
        :width="260"
        beside
        :label="sheet.name"
        :title="sheet.name"
        :subtitle="t('handheld.partner.state')"
        icon="party"
        @close="panel = null"
      >
        <div class="panel-rows">
          <PanelRow
            v-for="row in sheet.statusRows"
            :key="row.icon"
            :icon="row.icon"
            :label="row.text"
            :tone="row.tone"
          />
        </div>
      </FloatingPanel>
    </section>
    <section v-else class="equipo empty">
      <div class="win empty-note">{{ t("handheld.party.none") }}</div>
    </section>

    <StatusBar />
  </div>
</template>

<style scoped>
.equipo {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 6px;
}

.empty-note {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.detail {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.profile {
  height: 132px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.profile-head {
  height: 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-name {
  font-size: 16px;
}

.profile-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.profile-tp {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meter {
  position: relative;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.meter-fill {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
}

.meter-label {
  position: absolute;
  left: 4px;
}

.meter-text {
  position: relative;
}

.columns {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.left {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.block-title {
  flex-shrink: 0;
  font-size: 10px;
}

.evolution {
  min-height: 36px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: center;
  gap: 5px;
  font-size: 12px;
}

.evolution-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* What the lit row means out of a battle: the form it starts in. */
.evolution-text {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
}

/* The form the battle starts in: its icon beside the text. The longest name still fits, a little tighter. */
.starts {
  flex-direction: row;
  align-items: center;
  gap: 4px;
}

.starts .evolution-text {
  flex: 1;
  min-width: 0;
}

.starts .evolution-line {
  letter-spacing: -1px;
  white-space: nowrap;
}

.starts-battle {
  flex-shrink: 0;
}

.evolution-empty {
  align-items: stretch;
}

.segments {
  display: flex;
  gap: 2px;
  height: 5px;
}

.segment {
  flex: 1;
  min-width: 0;
  background: #0b2a78;
}

.segment.filled {
  background: #5fb4ff;
}

.active .segment {
  background: #5a2a0c;
}

.active .segment.filled {
  background: #ffd52e;
}

.equipment {
  flex: 1;
  min-height: 0;
  display: grid;
  /* One name per row: the longest (Positron Cannon, 15 letters) takes 150 of the 210 px inside the frame, where two
     columns of 107 px held no more than 10. */
  grid-template-columns: minmax(0, 1fr);
  grid-template-rows: repeat(6, minmax(0, 1fr));
  gap: 2px;
}

.equipment-item {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-align: left;
  font-size: 10px;
  color: #8fb4e8;
  border-bottom: 2px solid #123a9e;
}

.equipment-item.equipped {
  color: #7cc0ff;
}

.stats {
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 2px;
}

.stats-column {
  min-width: 0;
  display: grid;
  grid-template-rows: repeat(7, minmax(0, 1fr));
}

.stat {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.stat-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
}

.stat-bonus {
  font-size: 10px;
}
</style>
