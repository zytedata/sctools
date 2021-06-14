// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add("login", (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add("drag", { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add("dismiss", { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite("visit", (originalFn, url, options) => { ... })


declare namespace Cypress {
  interface Chainable {
    clearJobInfoCache(): void
  }
}

Cypress.Commands.add('clearJobInfoCache', () => {
  // visit an empty page so we can release the indexed db;
  return new Cypress.Promise(async (resolve, reject) => {
    // @ts-ignore
    let dbs = await indexedDB.databases();
    let found = false;

    for (let i = 0; i < dbs.length; i++) {
      let db = dbs[i];
      if (db.name == 'jobs') {
        found = true;
        const req = indexedDB.deleteDatabase('jobs');

        req.onsuccess = function() {
          // console.log('req.onsuccess');
          resolve();
        };
        req.onerror = function(e) {
          // console.log('req.onerror');
          console.warn(e);
          reject(new Error('clearJobInfoCache failed'));
        };
      }
    }

    if (!found) {
      resolve();
    }
  });
});
