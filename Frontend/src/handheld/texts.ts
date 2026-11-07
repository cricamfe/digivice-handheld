import { useI18n } from "vue-i18n";
import type { Texts } from "./map-view";

/** The `Texts` the sheet builders take, from the vue-i18n of the component that calls it. */
export function useTexts(): Texts {
  const { t, te } = useI18n();
  return { t: (key, named) => t(key, named ?? {}), te };
}
