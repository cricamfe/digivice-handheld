<script setup lang="ts">
import { useTexts } from "../texts";
import { placeWord } from "../place-words";
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import MapImage from "../components/MapImage.vue";
import StatusBar from "../components/StatusBar.vue";
import { iconUrl } from "../assets";
import { buildSeabedDock, buildSeabedHere } from "../seabed-sheet";
import { useScreen } from "../router";
import { useCover } from "../use-cover";
import { useGameState } from "@/composables/use-game-state";

const { t } = useI18n();
const { go, param } = useScreen();
const gameState = useGameState();

const texts = useTexts();

const mapId = computed(() => gameState.value.player.mapId);

/** A dock picked in the Seabed overview shows that dock; without one, it shows where the player is. */
const dockId = computed(() => (typeof param.value === "string" ? param.value : null));
const dock = computed(() => (dockId.value === null ? null : buildSeabedDock(dockId.value, texts)));
const here = computed(() => buildSeabedHere(gameState.value, texts));

const title = computed(() => dock.value?.title ?? here.value.title);
const imageUrl = computed(() => (dock.value ? dock.value.imageUrl : here.value.imageUrl));

const stage = ref<HTMLElement | null>(null);
const { onLoad, place } = useCover(stage, () => imageUrl.value);

const pinStyle = computed(() => {
  if (!dock.value || dock.value.x === null || dock.value.y === null) {
    return null;
  }

  const spot = place(dock.value.x, dock.value.y);
  return { left: `calc(${spot.left} - 22px)`, top: `calc(${spot.top} - 40px)` };
});

/** The exits are laid along the top edge on the side of the map where they open. */
const emergeAlignment = computed(() => {
  const exits = here.value.emerge;
  if (exits.length !== 1) {
    return "space-between";
  }

  const sides = { topLeft: "flex-start", top: "center", topRight: "flex-end" } as const;
  return sides[exits[0]!.on];
});
</script>

<template>
  <div class="view">
    <section class="ruta">
      <div ref="stage" class="win stage">
        <MapImage :src="imageUrl" :alt="title" :detail="dockId ?? mapId" @load="onLoad" />

        <div class="header">
          <button
            v-if="dock"
            type="button"
            class="btn back"
            :aria-label="t('handheld.back')"
            @click="go('seabed')"
          >
            <img :src="iconUrl('back')" alt="" class="icon" />
          </button>
          <div class="plate heading">
            <img :src="iconUrl('seabed')" alt="" class="icon" />
            <span class="gold heading-text">{{ title }}</span>
          </div>
          <!-- Where the player is has no way to the route map in the design, so the anchor opens it. -->
          <button
            v-if="!dock"
            type="button"
            class="btn back routes"
            :aria-label="t('handheld.map.seabed', { place: placeWord(texts, 'seabed') })"
            @click="go('seabed')"
          >
            <img :src="iconUrl('seabed')" alt="" class="icon" />
          </button>
        </div>

        <template v-if="dock">
          <img
            v-if="pinStyle"
            :src="iconUrl('place')"
            :alt="t('seabed.dock')"
            class="pin"
            :style="pinStyle"
          />
        </template>

        <template v-else>
          <div
            v-if="here.emerge.length > 0"
            class="emerge"
            :style="{ justifyContent: emergeAlignment }"
          >
            <div v-for="exit in here.emerge" :key="`${exit.on}-${exit.name}`" class="good exit">
              <img :src="iconUrl('weak')" alt="" class="icon" />
              <span class="exit-text">
                <span class="up">{{ t("handheld.seabed.emerge") }}</span>
                <span class="drop">{{ exit.name }}</span>
              </span>
            </div>
          </div>

          <div v-if="here.left || here.right" class="sides">
            <div v-if="here.left" class="slot side">
              <img :src="iconUrl('back')" alt="" class="icon" />
              <span class="side-text">
                <span
                  v-for="(line, index) in here.left.lines"
                  :key="index"
                  :class="index === 0 ? 'drop' : 'ice'"
                  >{{ line }}</span
                >
              </span>
            </div>
            <span v-else></span>
            <div v-if="here.right" class="slot side right">
              <span class="side-text right-text">
                <span
                  v-for="(line, index) in here.right.lines"
                  :key="index"
                  :class="index === 0 ? 'drop' : 'ice'"
                  >{{ line }}</span
                >
              </span>
              <img :src="iconUrl('arrow')" alt="" class="icon" />
            </div>
          </div>

          <div v-if="here.encounters.length > 0" class="win-soft encounters">
            <img :src="iconUrl('walking')" :alt="t('handheld.seabed.walking')" class="icon" />
            <button
              v-for="enemy in here.encounters"
              :key="enemy.id"
              type="button"
              class="enemy"
              @click="go('wikiEnemigo', enemy.id)"
            >
              {{ enemy.name }}{{ enemy.level === null ? "" : ` ${t("digimon.lv")}${enemy.level}` }}
            </button>
          </div>
        </template>
      </div>
    </section>

    <StatusBar />
  </div>
</template>

<style scoped>
.ruta {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 6px;
}

.stage {
  position: relative;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.header {
  position: absolute;
  left: 4px;
  top: 4px;
  right: 4px;
  display: flex;
  gap: 4px;
}

.back {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.routes {
  margin-left: auto;
}

.heading {
  height: 44px;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
}

.heading-text {
  white-space: nowrap;
  overflow: hidden;
  text-transform: uppercase;
}

.pin {
  position: absolute;
  width: 44px;
  height: 44px;
}

.up {
  color: #b4f7a8;
}

.emerge {
  position: absolute;
  left: 4px;
  right: 4px;
  top: 60px;
  display: flex;
  gap: 4px;
}

.exit {
  width: 184px;
  max-width: 100%;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
}

.exit-text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sides {
  position: absolute;
  left: 4px;
  right: 4px;
  top: 42%;
  display: flex;
  justify-content: space-between;
  gap: 4px;
}

.side {
  min-width: 200px;
  max-width: 232px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
}

.side-text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.right {
  justify-content: flex-end;
  text-align: right;
}

.right-text {
  align-items: flex-end;
}

.encounters {
  position: absolute;
  left: 4px;
  right: 4px;
  bottom: 4px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 10px;
  font-size: 10px;
}

.enemy {
  position: relative;
  color: #ff7a5a;
}

/* The line is 8 px high in the design; this widens the area that answers to a finger without moving it. */
.enemy::after {
  content: "";
  position: absolute;
  inset: -12px -4px;
}
</style>
