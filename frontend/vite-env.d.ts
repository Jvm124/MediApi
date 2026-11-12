/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly VITE_API_BASE_URL: string;
    // Añade aquí cualquier otra variable VITE_* que uses
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}