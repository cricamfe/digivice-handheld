import { createI18n } from "vue-i18n";
import enUS from "./locales/en-US/handheld-locale";

// Launcher and overlay share the app messages. Only the overlay reads the disc.
export default createI18n({
  legacy: false,
  locale: "en-US",
  fallbackLocale: "en-US",
  fallbackWarn: false,
  missingWarn: false,
  messages: { "en-US": enUS },
});
