<script setup lang="ts">
import { useTexts } from "../texts";
import { computed, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import PanelRow from "../components/PanelRow.vue";
import PartyColumn from "../components/PartyColumn.vue";
import FigurePanel from "../components/FigurePanel.vue";
import FloatingPanel, { type PanelAnchor } from "../components/FloatingPanel.vue";
import StatusBar from "../components/StatusBar.vue";
import { iconUrl } from "../assets";
import { decimal, times } from "../format";
import { buildBattleSheet, type BattleStat } from "../battle-sheet";
import type { AttackGrade, BattleAttack } from "../battle-attacks";
import { useScreen } from "../router";
import { usePanelHost } from "../use-panel-host";
import { useGameState } from "@/composables/use-game-state";
import { useGameStore } from "@/stores/use-game-store";
import { PartyPresenter } from "@/presenters/party/party.presenter";

const { t, locale } = useI18n();
const texts = useTexts();
const { go } = useScreen();
const gameState = useGameState();
const store = useGameStore();

const mapId = computed(() => gameState.value.player.mapId);
const slots = computed(() => PartyPresenter.getFilledSlots(gameState.value.party.slots));

/**
 * The partner whose attacks the list shows: the one the player tapped in this battle, else the one the game has out
 * (which it follows when the game changes fighter). The choice goes when the battle ends.
 */
const chosenSlot = ref<number | null>(null);
const chosenForm = ref<number>();
const formsOpen = ref(false);
const outSlot = computed(() => gameState.value.digimonBattle.activeFighter + 1);
const pickedSlot = computed(() =>
  slots.value.some((slot) => slot.index === chosenSlot.value) ? chosenSlot.value! : outSlot.value,
);
const sheet = computed(() =>
  buildBattleSheet(
    gameState.value,
    texts,
    mapId.value,
    store.partnerTechniques,
    pickedSlot.value,
    chosenForm.value,
  ),
);

watch(pickedSlot, () => {
  chosenForm.value = undefined;
  popup.value = null;
});

watch(
  () => sheet.value.hasEnemy,
  (hasEnemy) => {
    if (!hasEnemy) {
      chosenSlot.value = null;
      chosenForm.value = undefined;
      formsOpen.value = false;
      popup.value = null;
    }
  },
);

function pick(index: number): void {
  chosenSlot.value = index;
  chosenForm.value = undefined;
  popup.value = null;
}

/** The detail of one attack, element, stat or condition, floating next to its row. */
type PopupKind = "attack" | "element" | "figure";
const popup = ref<{ kind: PopupKind; key: string; anchor: PanelAnchor } | null>(null);
const { host: stage, hostSize: host, anchorOf } = usePanelHost();

/** Tapping a row opens its detail; tapping the same row again closes it. */
function openPopup(event: MouseEvent, kind: PopupKind, key: string): void {
  if (popup.value?.kind === kind && popup.value.key === key) {
    popup.value = null;
    return;
  }

  const anchor = anchorOf(event.currentTarget);
  popup.value = anchor === null ? null : { kind, key, anchor };
}

const popupAttack = computed(() =>
  popup.value?.kind === "attack"
    ? (sheet.value.attacks.attacks.find((attack) => attack.key === popup.value?.key) ?? null)
    : null,
);
const popupElement = computed(() =>
  popup.value?.kind === "element"
    ? (sheet.value.affinities.find((affinity) => affinity.element === popup.value?.key) ?? null)
    : null,
);

/** The stat or the condition whose panel is open. */
const popupFigure = computed(() => {
  if (popup.value?.kind !== "figure") {
    return null;
  }

  const figures = [...sheet.value.attributes, ...sheet.value.elements, ...sheet.value.conditions];
  return figures.find((figure) => figure.key === popup.value?.key)?.detail ?? null;
});

const hasDetails = computed(() => sheet.value.attributes.length > 0);
const isOpen = ref(false);

function toggle(): void {
  isOpen.value = !isOpen.value;
  popup.value = null;
}

function toggleForms(): void {
  formsOpen.value = !formsOpen.value;
  popup.value = null;
}

/** Kit icon of a grade: the green arrow for what hits harder, the red one for what hits softer, the bars for the same. */
function gradeIcon(grade: AttackGrade): string {
  if (grade === "normal") {
    return "same";
  }

  return grade === "veryEffective" || grade === "effective" ? "weak" : "resist";
}

/** The line under an attack's name: its element and the kind of technique it is. */
function attackSubtitle(attack: BattleAttack): string {
  const element = attack.element ? t(`stat.${attack.element}`) : t("handheld.battle.pop.noElement");
  return `${t(`techniqueTypes.${attack.type}`)} · ${element}`;
}

function deltaClass(stat: BattleStat): string {
  if (stat.delta > 0) {
    return "up";
  }

  return stat.delta < 0 ? "down" : "";
}

function openEnemy(): void {
  if (sheet.value.enemyId !== null) {
    go("wikiEnemigo", sheet.value.enemyId);
  }
}
</script>

<template>
  <div class="view">
    <section class="batalla">
      <PartyColumn
        :slots="slots"
        :selected="pickedSlot"
        :map-id="mapId"
        show-mp
        keep-hp
        :label-keys="['handheld.battle.pick', 'handheld.battle.picked']"
        @pick="pick"
      />

      <div ref="stage" class="win stage" :class="{ 'has-enemy': sheet.hasEnemy }">
        <img v-if="sheet.stageImageUrl" :src="sheet.stageImageUrl" alt="" class="backdrop" />

        <div class="win-soft header">
          <img
            :src="iconUrl(sheet.hasEnemy ? sheet.speciesIcon : 'unknown')"
            :alt="
              sheet.speciesLabel ? t('handheld.battle.species', { name: sheet.speciesLabel }) : ''
            "
            class="species"
          />
          <div class="header-body">
            <div class="header-line">
              <span class="drop name" :class="{ boss: sheet.isBoss }">{{
                sheet.hasEnemy
                  ? sheet.name || t("handheld.battle.unknownEnemy")
                  : t("handheld.battle.noEnemy")
              }}</span>
              <div
                class="field"
                :class="sheet.field.kind === 'neutral' ? 'win-soft' : `frame-${sheet.field.kind}`"
              >
                <span class="field-name" :class="sheet.field.kind === 'neutral' ? 'dim' : 'gold'">
                  <img :src="iconUrl(sheet.field.kind)" alt="" class="icon" />
                  <span>{{ sheet.field.name }}</span>
                </span>
                <span
                  v-for="effect in sheet.field.effects"
                  :key="effect.element"
                  class="field-effect"
                  :aria-label="`${t(`stat.${effect.element}`)} ${effect.text}`"
                >
                  <img :src="iconUrl(effect.element)" alt="" class="icon" />
                  <span class="figure-text" :class="effect.up ? 'up' : 'down'">{{
                    effect.text
                  }}</span>
                </span>
              </div>
            </div>
            <div v-if="sheet.hasEnemy" class="header-health">
              <span v-if="sheet.level !== null" class="gold enemy-level"
                >{{ t("digimon.lv") }} {{ sheet.level }}</span
              >
              <div class="bar meter">
                <span class="hp meter-fill" :style="{ width: sheet.hp.percent }"></span>
                <span class="outline meter-label">{{ t("handheld.bar.hp") }}</span>
                <span class="outline meter-text">{{ sheet.hp.text }}</span>
              </div>
              <button
                v-if="hasDetails"
                type="button"
                class="btn details-button"
                :aria-expanded="isOpen"
                @click="toggle"
              >
                {{ isOpen ? t("handheld.battle.close") : t("handheld.battle.details") }}
              </button>
            </div>
          </div>
        </div>

        <div class="band">
          <div
            v-if="formsOpen"
            class="win-soft scroll form-browser"
            role="group"
            :aria-label="t('handheld.battle.forms')"
          >
            <div class="form-options">
              <button
                v-for="form in sheet.attacks.forms"
                :key="form.id"
                type="button"
                class="form-option"
                :class="form.id === sheet.attacks.formId ? 'thin-on' : 'thin'"
                :aria-pressed="form.id === sheet.attacks.formId"
                @click="chosenForm = form.live ? undefined : form.id"
              >
                <span>{{ form.name }}</span>
                <small v-if="form.live" class="gold">{{ t("handheld.battle.liveForm") }}</small>
              </button>
            </div>
            <div
              class="form-stats"
              role="group"
              :aria-label="t('handheld.battle.formStats', { name: sheet.attacks.fighterName })"
            >
              <span v-for="stat in sheet.attacks.stats" :key="stat.key" class="form-stat">
                <img
                  :src="iconUrl(stat.key)"
                  :alt="t(`stat.${stat.key}`)"
                  :title="t(`stat.${stat.key}`)"
                />
                {{ stat.value }}
              </span>
            </div>
          </div>
          <button
            v-else-if="sheet.enemyAnimationUrl || sheet.enemyImageUrl"
            type="button"
            class="enemy"
            :disabled="sheet.enemyId === null"
            :aria-label="sheet.name"
            @click="openEnemy"
          >
            <span
              v-if="sheet.enemyAnimationUrl"
              :key="sheet.enemyAnimationUrl"
              class="enemy-animation"
            >
              <span class="enemy-frames">
                <img :src="sheet.enemyAnimationUrl" alt="" />
              </span>
            </span>
            <img v-else :src="sheet.enemyImageUrl ?? undefined" alt="" class="enemy-image" />
          </button>
          <span v-else class="enemy"></span>

          <div v-if="sheet.hasEnemy" class="win-soft affinities">
            <span class="gold block-title">{{ t("handheld.battle.affinities") }}</span>
            <div v-if="sheet.affinities.length > 0" class="scroll affinity-list">
              <button
                v-for="(affinity, position) in sheet.affinities"
                :key="affinity.element"
                type="button"
                class="affinity"
                :class="{ 'thin-on': position === 0 && affinity.multiplier > 1 }"
                data-panel-anchor
                @click="openPopup($event, 'element', affinity.element)"
              >
                <img
                  :src="iconUrl(affinity.element)"
                  :alt="t(`stat.${affinity.element}`)"
                  class="icon"
                />
                <img
                  :src="iconUrl(affinity.multiplier > 1 ? 'weak' : 'resist')"
                  alt=""
                  class="icon"
                />
                <span class="grade-text" :class="`grade-${affinity.grade}`">{{
                  t(`handheld.battle.grade.${affinity.grade}`)
                }}</span>
                <span class="figure-text" :class="`grade-${affinity.grade}`">{{
                  times(affinity.multiplier, locale)
                }}</span>
              </button>
            </div>
            <span v-else class="grade-text grade-normal">{{
              t("handheld.battle.noAffinity")
            }}</span>
          </div>
        </div>

        <div v-if="sheet.hasEnemy" class="win attacks">
          <div class="gold block-title attacks-title">
            <span>{{ t("handheld.battle.attacksOf") }}</span>
            <span class="attacks-fighter">{{ sheet.attacks.fighterName }}</span>
            <button
              v-if="sheet.attacks.forms.length"
              type="button"
              class="forms-button"
              :class="formsOpen ? 'btn-on' : 'btn'"
              :aria-expanded="formsOpen"
              @click="toggleForms"
            >
              {{ t("handheld.battle.forms") }}
            </button>
          </div>
          <div
            v-if="sheet.attacks.attacks.length > 0"
            :key="`${pickedSlot}-${sheet.attacks.formId}`"
            class="scroll attack-list"
            @scroll="popup?.kind === 'attack' && (popup = null)"
          >
            <button
              v-for="attack in sheet.attacks.attacks"
              :key="attack.key"
              type="button"
              class="attack"
              :class="[attack.isBest ? 'thin-on' : 'thin', { unusable: !attack.usable }]"
              data-panel-anchor
              @click="openPopup($event, 'attack', attack.key)"
            >
              <img
                :src="iconUrl(attack.element ?? 'cross')"
                :alt="attack.element ? t(`stat.${attack.element}`) : ''"
                class="icon"
              />
              <span class="drop attack-name" :class="{ long: attack.name.length > 13 }">{{
                attack.name
              }}</span>
              <span class="ice figure-text">{{ t("handheld.bar.mp") }} {{ attack.mp }}</span>
              <span class="grade" :class="`grade-${attack.grade}`">{{
                t(`handheld.battle.grade.${attack.grade}`)
              }}</span>
            </button>
          </div>
          <p v-else class="dim attacks-empty">
            {{ t("handheld.battle.noAttacks") }}
          </p>
        </div>

        <div v-if="isOpen && hasDetails" class="win details">
          <div class="details-column">
            <button
              v-for="stat in sheet.attributes"
              :key="stat.key"
              type="button"
              class="figure"
              :aria-label="stat.label"
              data-panel-anchor
              @click="openPopup($event, 'figure', stat.key)"
            >
              <img :src="iconUrl(stat.key)" alt="" class="icon" />
              <span class="drop" :class="deltaClass(stat)">{{ stat.value }}</span>
            </button>
          </div>
          <div class="details-column">
            <button
              v-for="stat in sheet.elements"
              :key="stat.key"
              type="button"
              class="figure"
              :aria-label="stat.label"
              data-panel-anchor
              @click="openPopup($event, 'figure', stat.key)"
            >
              <img :src="iconUrl(stat.key)" alt="" class="icon" />
              <span class="drop" :class="deltaClass(stat)">{{ stat.value }}</span>
            </button>
          </div>
          <div class="details-column">
            <button
              v-for="condition in sheet.conditions"
              :key="condition.key"
              type="button"
              class="figure"
              :aria-label="condition.label"
              data-panel-anchor
              @click="openPopup($event, 'figure', condition.key)"
            >
              <img :src="iconUrl(condition.key)" alt="" class="icon" />
              <span class="drop" :class="condition.can ? 'up' : 'down'">{{ condition.text }}</span>
            </button>
          </div>
        </div>

        <!-- An attack: its element's frame on the header, then what it costs, what it hits for and what it does here. -->
        <FloatingPanel
          v-if="popup !== null && popupAttack !== null"
          class="attack-detail"
          :anchor="popup.anchor"
          :host-width="host.width"
          :host-height="host.height"
          :label="popupAttack.name"
          :title="popupAttack.name"
          :subtitle="attackSubtitle(popupAttack)"
          :icon="popupAttack.element ?? 'cross'"
          :frame="popupAttack.element ? `frame-${popupAttack.element}` : undefined"
          @close="popup = null"
        >
          <div class="panel-rows">
            <PanelRow icon="mp" :label="t('handheld.bar.mp')">
              <span :class="popupAttack.usable ? 'ice' : 'down'">{{ popupAttack.mp }}</span>
            </PanelRow>
            <PanelRow icon="power" :label="t('handheld.battle.pop.power')">{{
              popupAttack.power
            }}</PanelRow>
            <PanelRow
              :icon="gradeIcon(popupAttack.grade)"
              :label="t(`handheld.battle.grade.${popupAttack.grade}`)"
              :class="`grade-row grade-${popupAttack.grade}`"
            >
              {{ times(popupAttack.multiplier, locale) }}
            </PanelRow>
            <PanelRow icon="hp" :label="t('handheld.battle.pop.damage')">
              ~{{ popupAttack.damage }}
              <span class="dim">{{ t("handheld.battle.pop.estimate") }}</span>
            </PanelRow>
          </div>
          <p class="panel-text panel-note">{{ t("handheld.battle.pop.damageNote") }}</p>
          <PanelRow
            v-if="!popupAttack.usable"
            icon="alert"
            :label="t('handheld.battle.pop.noMp')"
            wrap
          />
          <p v-if="popupAttack.description" class="panel-text">{{ popupAttack.description }}</p>
        </FloatingPanel>

        <!-- An element of the enemy: how it takes it, and what that does to the damage. -->
        <FloatingPanel
          v-if="popup !== null && popupElement !== null"
          :anchor="popup.anchor"
          :host-width="host.width"
          :host-height="host.height"
          :label="t(`stat.${popupElement.element}`)"
          :title="t(`stat.${popupElement.element}`)"
          :subtitle="t('handheld.battle.pop.against', { name: sheet.name })"
          :icon="popupElement.element"
          :frame="`frame-${popupElement.element}`"
          @close="popup = null"
        >
          <div class="panel-rows">
            <PanelRow
              :icon="gradeIcon(popupElement.grade)"
              :label="t(`handheld.battle.grade.${popupElement.grade}`)"
              :class="`grade-row grade-${popupElement.grade}`"
            >
              {{ times(popupElement.multiplier, locale) }}
            </PanelRow>
          </div>
          <p class="panel-text">
            {{
              t(
                popupElement.multiplier > 1
                  ? "handheld.battle.pop.weakPoint"
                  : "handheld.battle.pop.resisted",
              )
            }}
          </p>
          <p class="panel-text panel-note">
            {{
              t("handheld.battle.pop.elementNote", {
                multiplier: decimal(popupElement.multiplier, locale),
              })
            }}
          </p>
        </FloatingPanel>

        <FigurePanel
          v-if="popup !== null && popupFigure !== null"
          :detail="popupFigure"
          :anchor="popup.anchor"
          :host-width="host.width"
          :host-height="host.height"
          @close="popup = null"
        />
      </div>
    </section>

    <StatusBar />
  </div>
</template>

<style scoped>
/* One palette for what an element does to the enemy, for the weaknesses and the attacks alike: green hits it harder,
   red softer, the blue between. The word on the chip says it too. */
.batalla {
  --t-strong: #a8ffb8;
  --t-more: #6fe08c;
  --t-same: #a4c4f4;
  --t-less: #ffa592;
  --t-very: #ff7e6e;
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 6px;
}

/* Every panel of the column shares the stage's left and right edges and the same gap. */
.stage {
  position: relative;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  padding: 4px;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  grid-template-rows: auto minmax(144px, 1fr);
  gap: 4px;
  /* Where the game has no picture of the stage: flat, not another picture. */
  background-color: #02061c;
}

/* The attack list scrolls in its own row, independently of the enemy's size and position. */
.stage.has-enemy {
  grid-template-rows: auto 144px minmax(0, 1fr);
}

.backdrop {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.up {
  color: #b4f7a8;
}

.down {
  color: #ffb09a;
}

.header {
  grid-row: 1;
  position: relative;
  min-height: 58px;
  padding: 4px 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.species {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
}

.header-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-line {
  display: flex;
  align-items: center;
  gap: 6px;
}

.name {
  font-size: 12px;
  min-width: 0;
}

.header-line .drop {
  flex: 1;
  color: #ff7a5a;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.header-line .drop.boss {
  color: #ffd52e;
}

.header-health {
  display: flex;
  align-items: center;
  gap: 6px;
}

.enemy-level {
  white-space: nowrap;
}

.meter {
  flex: 1;
  min-width: 0;
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

.band {
  grid-row: 2;
  position: relative;
  display: flex;
  align-items: stretch;
  gap: 4px;
}

.enemy {
  flex: 1;
  min-width: 0;
  display: grid;
  place-items: center;
  container-type: size;
}

.enemy-animation {
  display: block;
  width: min(100cqw, 100cqh);
  aspect-ratio: 1;
  overflow: hidden;
}

/* The pack stores 8 columns by 6 rows. Move the atlas without repainting individual frames. */
.enemy-frames {
  display: block;
  width: 800%;
  animation: enemy-columns calc(2s / 6) steps(8, end) infinite;
}

.enemy-frames img {
  display: block;
  width: 100%;
  max-width: none;
  animation: enemy-rows 2s steps(6, end) infinite;
}

@keyframes enemy-columns {
  to {
    transform: translateX(-100%);
  }
}

@keyframes enemy-rows {
  to {
    transform: translateY(-100%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .enemy-frames,
  .enemy-frames img {
    animation: none;
  }
}

.enemy-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: left center;
}

.affinities {
  flex: 0 0 42%;
  align-self: flex-start;
  min-width: 0;
  padding: 4px;
  max-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-height: 0;
}

.block-title {
  flex-shrink: 0;
  font-size: 10px;
  line-height: 1.2;
}

.affinity-list {
  flex: 0 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
}

/* Rows without a fill: icon of the element, the arrow of its direction, the word and the multiplier in the colour of
   their grade. Only the hardest hit has a frame (the kit's gold one, as the best attack has). */
.affinity {
  flex-shrink: 0;
  min-height: 22px;
  display: grid;
  grid-template-columns: 16px 16px minmax(0, 1fr) auto;
  align-items: center;
  column-gap: 3px;
  border-width: 0 4px;
  border-style: solid;
  border-color: transparent;
  padding: 0 2px;
  text-align: left;
}

.affinity.thin-on {
  min-height: 26px;
  border-width: 4px;
  padding: 0 2px;
}

.affinity .icon {
  width: 16px;
  height: 16px;
}

.attack .icon {
  margin: -2px 0;
}

.figure-text {
  font-size: 10px;
  white-space: nowrap;
}

.grade-text {
  font-size: 10px;
  overflow-wrap: anywhere;
}

/* The scale's colours, for the word of a grade and for its chip: green gives more, red less, blue the same. */
.grade-veryEffective {
  --grade-fg: var(--t-strong);
  --grade-bg: #0e5a2e;
}

.grade-effective {
  --grade-fg: var(--t-more);
  --grade-bg: #0a3a22;
}

.grade-normal {
  --grade-fg: var(--t-same);
  --grade-bg: #0c1a48;
}

.grade-weak {
  --grade-fg: var(--t-less);
  --grade-bg: #3a1118;
}

.grade-veryWeak {
  --grade-fg: var(--t-very);
  --grade-bg: #4a0c1c;
}

.grade-text,
.affinity .figure-text {
  color: var(--grade-fg);
}

/* The chip of a grade in an attack row: one size, a dark fill of its colour and its word in the bright one. */
.grade {
  min-width: 0;
  min-height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  line-height: 1;
  text-align: center;
  overflow-wrap: anywhere;
  color: var(--grade-fg);
  background: var(--grade-bg);
  box-shadow: inset 0 0 0 2px #040716;
}

.attacks {
  grid-row: 3;
  position: relative;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 4px;
  padding-bottom: 4px;
}

.attacks-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.attacks-title > span:first-child {
  white-space: nowrap;
}

.attacks-fighter {
  min-width: 0;
  overflow-wrap: anywhere;
}

.forms-button {
  flex-shrink: 0;
  margin-inline-start: auto;
  padding: 1px 8px;
}

.form-browser {
  flex: 1;
  min-width: 0;
  min-height: 0;
  padding: 2px;
  overflow-y: auto;
}

.form-options,
.form-stats {
  display: grid;
  gap: 2px;
}

.form-options {
  grid-template-columns: minmax(0, 1fr) fit-content(60%);
}

.form-option {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  min-height: 22px;
  padding: 1px 3px;
  text-align: start;
  overflow-wrap: anywhere;
}

.form-option > * {
  max-width: 100%;
}

.form-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 4px;
}

.form-stat {
  display: flex;
  gap: 3px;
  align-items: center;
  min-height: 14px;
}

.form-stat img {
  width: 14px;
  height: 14px;
}

.attack-detail :deep(.panel-title) {
  white-space: normal;
  overflow-wrap: anywhere;
}

.attacks-enemy {
  flex-shrink: 0;
}

/* The word and the figure of a grade in a panel take the colour of the scale. */
.grade-row :deep(.panel-label),
.grade-row :deep(.panel-value) {
  color: var(--grade-fg);
}

.attack-list {
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
  overflow-y: auto;
}

/* Fixed columns: element, name, MP, grade. The damage is in the details a tap shows. */
.attack {
  flex-shrink: 0;
  min-height: 26px;
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) 64px 152px;
  align-items: center;
  column-gap: 4px;
  padding: 0 4px;
  text-align: left;
}

/* The game greys the techniques without the MP; a darker row says the same without a see-through box. */
.attack.unusable {
  filter: brightness(0.6);
}

.attack-name {
  font-size: 12px;
  overflow-wrap: anywhere;
}

.attack-name.long {
  font-size: 10px;
}

.attacks-empty {
  font-size: 10px;
  line-height: 1.4;
}

.field,
.details-button,
.affinities,
.form-browser,
.forms-button {
  border-width: 4px;
  border-image-width: 4px;
}

.field {
  min-width: 0;
  max-width: 50%;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, auto));
  gap: 2px 4px;
  padding: 1px 4px;
}

.field .icon {
  width: 14px;
  height: 14px;
}

.field-name {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  line-height: 1.2;
  overflow-wrap: anywhere;
  text-transform: uppercase;
}

.field-effect {
  display: flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
}

.details-button {
  padding: 0 6px;
}

.details {
  position: absolute;
  grid-row: 2 / 4;
  grid-column: 1;
  inset: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.details-column {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-start;
}

.figure {
  position: relative;
  display: flex;
  align-items: center;
  gap: 5px;
}

/* The rows are 22 px high in the design; this widens the area that answers to a finger without moving them. */
.figure::after {
  content: "";
  position: absolute;
  inset: -5px -4px;
}
</style>
