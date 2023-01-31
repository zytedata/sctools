import { defineConfig } from 'cypress'

export default defineConfig({
  watchForFileChanges: false,
  projectId: '4hv8zp',
  e2e: {
    // We've imported your old cypress plugins here.
    // You may want to clean this up later by importing these.
    setupNodeEvents(on, config) {
      return require('./cypress/plugins/index.ts')(on, config)
    },
    baseUrl: 'http://127.0.0.1:3345',
    specPattern: 'cypress/e2e/**/*_spec.ts',
    excludeSpecPattern: [
      '**/table_spec.ts',
    ],
  },
})
