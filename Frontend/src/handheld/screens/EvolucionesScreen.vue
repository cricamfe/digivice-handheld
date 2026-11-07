<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import EvolutionArt from "../components/EvolutionArt.vue";
import EvolutionNode from "../components/EvolutionNode.vue";
import FloatingPanel, { type PanelAnchor } from "../components/FloatingPanel.vue";
import PanelRow from "../components/PanelRow.vue";
import { forkUrl, iconUrl, spriteUrl } from "../assets";
import { DvexpBarSegments } from "@/constants/digievolution.constant";
import {
  buildNodePanel,
  findNode,
  firstNodeId,
  tryBuildEvolutionTree,
  type EvolutionTree,
  type TreeNode,
} from "../evolution-sheet";
import { useScreen } from "../router";
import { rowScrollFor } from "../tree-scroll";
import { useGameState } from "@/composables/use-game-state";
import { PartyPresenter } from "@/presenters/party/party.presenter";
import { ProfilePresenter } from "@/presenters/party/digimon/profile.presenter";

const { t } = useI18n();
const { go, param } = useScreen();
const gameState = useGameState();

const slots = computed(() => PartyPresenter.getFilledSlots(gameState.value.party.slots));

// The Tecnicas screen comes back here with the partner and the digievolution it was showing.
const asked = typeof param.value === "object" && param.value !== null ? param.value : null;
const pickedSlot = ref<number | null>(
  typeof asked?.digimonSlot === "number" ? asked.digimonSlot : null,
);
const pickedNode = ref<number | null>(
  typeof asked?.digievolutionId === "number" ? asked.digievolutionId : null,
);

/** The partner on show: the one asked for, else the first of the party. */
const slot = computed(
  () => slots.value.find((entry) => entry.index === pickedSlot.value) ?? slots.value[0] ?? null,
);

const tree = computed((): EvolutionTree | null => {
  const current = slot.value;
  if (current === null) {
    return null;
  }

  return tryBuildEvolutionTree(current.digimon!, current.digimonId!, t);
});

/** The node on show: the one chosen, else the digievolution the Digimon wears, else the first one. */
const selectedId = computed(() => {
  const current = tree.value;
  if (current === null) {
    return null;
  }

  const candidates = [pickedNode.value, slot.value?.digimon?.activeDigievolutionId ?? null];
  const found = candidates.find((id) => id !== null && findNode(current, id) !== null);
  return found ?? firstNodeId(current);
});

function pickSlot(index: number): void {
  pickedSlot.value = index;
  pickedNode.value = null;
  closePanel();
}

function openTechniques(): void {
  if (slot.value !== null && panel.value !== null) {
    go("tecnicas", { digimonSlot: slot.value.index, digievolutionId: panel.value.id });
  }
}

const container = ref<HTMLElement | null>(null);
const stage = ref<HTMLElement | null>(null);

/**
 * Brings the chosen node into view, so coming back from Tecnicas lands on it. Its row scrolls to a column edge
 * (`rowScrollFor`): the browser's own `scrollIntoView` would put the node at the right edge and leave the column on the
 * left cut in half. The other rows start at their first column.
 */
function revealSelected(): void {
  const tree = container.value;
  if (tree === null) {
    return;
  }

  for (const row of tree.querySelectorAll<HTMLElement>(".family")) {
    row.style.paddingRight = "";
    row.scrollLeft = 0;
  }

  const node = tree.querySelector<HTMLElement>("[aria-pressed='true']");
  const row = node?.closest<HTMLElement>(".family");
  if (!node || !row) {
    return;
  }

  const origin = row.getBoundingClientRect().left + row.clientLeft - row.scrollLeft;
  const spanOf = (element: Element) => {
    const box = element.getBoundingClientRect();
    return { left: box.left - origin, right: box.right - origin };
  };
  const columns = [...row.querySelectorAll("[data-node]")].map((element) => spanOf(element).left);
  const { scrollLeft, endRoom } = rowScrollFor(
    columns,
    spanOf(node),
    row.clientWidth,
    row.scrollWidth,
  );
  if (endRoom > 0) {
    row.style.paddingRight = `${endRoom}px`;
  }
  row.scrollLeft = scrollLeft;
  // The node is whole in its row now, so this only scrolls the tree up or down to it.
  node.scrollIntoView({ block: "nearest", inline: "nearest" });
}

/** The node whose panel is open, and the box it floats beside (in the stage's own pixels). */
const panelNodeId = ref<number | null>(null);
const panelAnchor = ref<PanelAnchor | null>(null);
const host = ref({ width: 0, height: 0 });

const panelNode = computed((): TreeNode | null => {
  const current = tree.value;
  return current === null || panelNodeId.value === null
    ? null
    : findNode(current, panelNodeId.value);
});

const panel = computed(() => {
  const current = tree.value;
  const entry = slot.value;
  return current === null || entry === null || panelNode.value === null
    ? null
    : buildNodePanel(current, panelNode.value, entry.digimon!, entry.digimonId!, t);
});

function closePanel(): void {
  panelNodeId.value = null;
  panelAnchor.value = null;
}

/** Floats the panel of a node beside it; false when the stage or the node is not there. */
function showPanel(id: number, element: HTMLElement): boolean {
  if (stage.value === null) {
    return false;
  }

  const box = stage.value.getBoundingClientRect();
  const rect = element.getBoundingClientRect();
  const left = box.left + stage.value.clientLeft;
  const top = box.top + stage.value.clientTop;
  host.value = { width: stage.value.clientWidth, height: stage.value.clientHeight };
  panelAnchor.value = {
    top: rect.top - top,
    bottom: rect.bottom - top,
    left: rect.left - left,
    right: rect.right - left,
  };
  panelNodeId.value = id;
  return true;
}

/** Tapping a node opens its panel; tapping it again closes it. The tap itself goes nowhere else. */
function pickNode(node: TreeNode, element: HTMLElement): void {
  pickedNode.value = node.id;
  if (panelNodeId.value === node.id) {
    closePanel();
    return;
  }

  showPanel(node.id, element);
}

/** Coming back from Tecnicas, the node it was showing opens again once the scroll that reveals it has settled. */
function reopenAsked(): void {
  const id = pickedNode.value;
  if (id === null) {
    return;
  }

  requestAnimationFrame(() =>
    requestAnimationFrame(() => {
      const element = container.value?.querySelector<HTMLElement>(`[data-node='${id}']`);
      if (element) {
        showPanel(id, element);
      }
    }),
  );
}

onMounted(() => {
  revealSelected();
  if (asked !== null) {
    reopenAsked();
  }
});
watch(slot, () => nextTick(revealSelected));
</script>

<template>
  <div class="view">
    <template v-if="slot">
      <div class="tabs" :style="{ gridTemplateColumns: `repeat(${slots.length}, minmax(0, 1fr))` }">
        <button
          v-for="entry in slots"
          :key="entry.index"
          type="button"
          class="tab"
          :class="entry.index === slot.index ? 'slot-on' : 'slot'"
          :aria-current="entry.index === slot.index ? 'true' : undefined"
          @click="pickSlot(entry.index)"
        >
          <img
            v-if="spriteUrl(entry.digimonId!)"
            :src="spriteUrl(entry.digimonId!)!"
            alt=""
            class="sprite"
          />
          <img v-else :src="iconUrl('unknown')" alt="" class="sprite-missing" />
          {{ ProfilePresenter.getName(entry.digimonId!) }}
        </button>
      </div>

      <div ref="stage" class="stage">
        <section
          ref="container"
          class="win scroll tree"
          :aria-label="t('handheld.evolutions.families')"
          @scroll.capture="closePanel"
        >
          <template v-if="tree && tree.families.length > 0">
            <template v-for="(family, index) in tree.families" :key="family.key">
              <div class="family scroll">
                <template v-for="(node, position) in family.line" :key="node.id">
                  <EvolutionNode
                    :node="node"
                    :selected="node.id === selectedId"
                    @pick="pickNode(node, $event)"
                  />
                  <img
                    v-if="position < family.line.length - 1"
                    :src="iconUrl('arrow')"
                    alt=""
                    class="arrow"
                  />
                </template>

                <template v-if="family.branches.length > 0">
                  <img :src="forkUrl()" alt="" class="fork" />
                  <div class="branches">
                    <div
                      v-for="(branch, branchIndex) in family.branches"
                      :key="branchIndex"
                      class="branch"
                    >
                      <template v-for="(node, position) in branch" :key="node.id">
                        <EvolutionNode
                          :node="node"
                          :selected="node.id === selectedId"
                          @pick="pickNode(node, $event)"
                        />
                        <img
                          v-if="position < branch.length - 1"
                          :src="iconUrl('arrow')"
                          alt=""
                          class="arrow"
                        />
                      </template>
                    </div>
                  </div>
                </template>
              </div>
              <div v-if="index < tree.families.length - 1" class="rule"></div>
            </template>
          </template>
          <div v-else class="none">
            <img :src="iconUrl('unknown')" alt="" class="none-icon" />
            <span class="drop">{{ t("digievolution.noEvolutionData") }}</span>
          </div>
        </section>

        <!-- A digievolution: its picture, whether the partner has it, what it asks, where it leads and what it gives. -->
        <FloatingPanel
          v-if="panel !== null && panelAnchor !== null"
          :anchor="panelAnchor"
          :host-width="host.width"
          :host-height="host.height"
          :width="244"
          beside
          :label="panel.name"
          @close="closePanel"
        >
          <template #head>
            <span class="thin pop-art-frame">
              <EvolutionArt
                v-if="panel.imageUrl"
                :key="panel.imageUrl"
                :src="panel.imageUrl"
                :evolution-id="panel.id"
                class="pop-art"
                :class="{ locked: !panel.isObtained }"
              />
              <img v-else :src="iconUrl('unknown')" alt="" class="pop-art-missing" />
              <span class="outline gold pop-name">{{ panel.name }}</span>
            </span>
          </template>

          <div class="panel-rows">
            <PanelRow
              :icon="panel.isObtained ? 'unlocked' : 'locked'"
              :label="
                t(
                  panel.isObtained
                    ? 'handheld.evolutions.panel.obtained'
                    : 'handheld.evolutions.panel.notObtained',
                )
              "
              :tone="panel.isObtained ? 'ice' : ''"
            >
              <span v-if="panel.isObtained" class="gold">{{
                t("handheld.evolutions.panel.level", { level: panel.level })
              }}</span>
            </PanelRow>
            <PanelRow v-if="panel.startsBattle" icon="battle" tone="gold">
              <template #label>{{ t("handheld.party.startsBattle") }}</template>
            </PanelRow>
          </div>
          <span v-if="panel.filled !== null" class="segments">
            <span
              v-for="segment in DvexpBarSegments"
              :key="segment"
              class="segment"
              :class="{ filled: segment <= panel.filled }"
            ></span>
          </span>

          <span class="gold panel-section">{{ t("handheld.evolutions.panel.requirements") }}</span>
          <div v-if="panel.requirements.length > 0" class="panel-rows">
            <div
              v-for="requirement in panel.requirements"
              :key="requirement.text"
              class="panel-row requirement"
            >
              <img
                :src="iconUrl(requirement.isMet ? 'check' : 'cross')"
                :alt="
                  t(
                    requirement.isMet
                      ? 'handheld.evolutions.panel.met'
                      : 'handheld.evolutions.panel.notMet',
                  )
                "
                class="panel-icon"
              />
              <span class="requirement-text">
                <span :class="requirement.isMet ? 'met' : 'unmet'">{{ requirement.text }}</span>
                <span class="dim"
                  >{{ t("handheld.evolutions.panel.current") }} {{ requirement.current }} /
                  {{ requirement.required }}</span
                >
              </span>
            </div>
          </div>
          <p v-else class="panel-text panel-note">
            {{ t("handheld.evolutions.panel.noRequirements") }}
          </p>
          <p
            v-if="!panel.isObtained && panel.requirements.every((entry) => entry.isMet)"
            class="panel-text ice"
          >
            {{ t("handheld.evolutions.panel.ready") }}
          </p>

          <span class="gold panel-section">{{ t("handheld.evolutions.panel.follows") }}</span>
          <p v-if="panel.next.length > 0" class="panel-text ice">{{ panel.next.join(", ") }}</p>
          <p v-else class="panel-text panel-note">{{ t("handheld.techniques.last") }}</p>

          <span class="gold panel-section">{{ t("handheld.evolutions.panel.gives") }}</span>
          <template v-if="panel.attributes.length > 0 || panel.elements.length > 0">
            <div v-if="panel.attributes.length > 0" class="pop-bonuses">
              <span v-for="bonus in panel.attributes" :key="bonus.key" class="pop-bonus">
                <img :src="iconUrl(bonus.key)" :alt="bonus.label" class="panel-icon" />
                <span>{{ bonus.value > 0 ? "+" : "" }}{{ bonus.value }}</span>
              </span>
            </div>
            <div v-if="panel.elements.length > 0" class="pop-bonuses">
              <span v-for="bonus in panel.elements" :key="bonus.key" class="pop-bonus">
                <img :src="iconUrl(bonus.key)" :alt="bonus.label" class="panel-icon" />
                <span>{{ bonus.value > 0 ? "+" : "" }}{{ bonus.value }}</span>
              </span>
            </div>
          </template>
          <p v-else class="panel-text panel-note">{{ t("handheld.evolutions.panel.noBonus") }}</p>

          <template #footer>
            <button type="button" class="btn panel-button" @click="openTechniques">
              {{ t("digievolution.techniques") }}
              <img :src="iconUrl('arrow')" alt="" />
            </button>
          </template>
        </FloatingPanel>
      </div>
    </template>
    <div v-else class="win none-party">{{ t("handheld.evolutions.noDigimon") }}</div>
  </div>
</template>

<style scoped>
.tabs {
  height: 48px;
  flex-shrink: 0;
  display: grid;
  gap: 3px;
  font-size: 10px;
}

.tab {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.sprite {
  width: 32px;
  height: 32px;
}

.sprite-missing {
  width: 22px;
  height: 22px;
}

/* The panel floats over this box, which holds the tree and nothing else, so it stays inside the screen. */
.stage {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.tree {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.family {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  overflow-x: auto;
  padding-bottom: 4px;
}

/* A dashed rule under every family but the last, in whole pixels (6 px dashes, 4 px gaps), as the design's rule. */
.rule {
  height: 2px;
  flex-shrink: 0;
  margin: 4px 0 8px;
  background: repeating-linear-gradient(to right, #144aa8 0 6px, transparent 6px 10px);
}

.arrow {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.fork {
  width: 24px;
  height: 190px;
  /* Stretch the SVG's original 190 px artwork to the two 132 px nodes and their 14 px gap. */
  transform: scaleY(calc(278 / 190));
  flex-shrink: 0;
}

.branches {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.branch {
  display: flex;
  align-items: center;
}

.none {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 12px;
}

.none-icon {
  width: 44px;
  height: 44px;
}

.none-party {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

/* The panel is 244 px wide, the most that fits beside any column of the tree. Its picture is the header band. */
.pop-art-frame {
  position: relative;
  flex-shrink: 0;
  height: 112px;
  padding: 24px 4px 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pop-art {
  width: 100%;
  height: 100%;
  min-height: 0;
}

.pop-art.locked {
  filter: grayscale(0.4) brightness(0.45);
}

.pop-art-missing {
  width: 44px;
  height: 44px;
}

.pop-name {
  position: absolute;
  left: 6px;
  top: 5px;
  right: 6px;
  font-size: 12px;
}

/* A requirement, and under it what the partner has against what it asks. */
.requirement {
  align-items: flex-start;
}

.requirement-text {
  min-width: 0;
  grid-column: 2 / 4;
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 12px;
  padding-top: 5px;
}

.met {
  color: #77e878;
}

.unmet {
  color: #ffb09a;
}

.pop-bonuses {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 2px 6px;
  padding: 2px 6px;
}

.pop-bonus {
  display: flex;
  align-items: center;
  gap: 4px;
}

.segments {
  flex-shrink: 0;
  display: flex;
  gap: 2px;
  height: 5px;
  margin: 2px 6px;
}

.segment {
  flex: 1;
  min-width: 0;
  background: #0b2a78;
}

.segment.filled {
  background: #5fb4ff;
}
</style>
