/// <reference types="vite/client" />

declare module '*.vue' {
    import type { DefineComponent } from 'vue'
    const component: DefineComponent<{}, {}, any>
    export default component
}

/** The app's version (`versionName` of the Android build), set by the handheld Vite config. */
declare const __APP_VERSION__: string
