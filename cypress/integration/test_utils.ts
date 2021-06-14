const API_KEY = 'ffffffffffffffffffffffffffffffff'

export const JOBS_URL = '#/studio/job/1/1/1/_/3'

export function stubJobsInfoResponse() {
  // The catch-all route must come first -- seems cypress is using
  // "last-match wins" for mutiple routes.
  cy.route({
    method: 'GET',
    url: 'https://storage.scrapinghub.com/**',
    status: 500,
    response: {},
  });

  const ids = [1, 2, 3];
  for (let id of ids) {
    const k = `jobs/${id}`;
    // Use cy.readFile instead of cy.fixture because the latter caches
    // the file content.
    // https://github.com/cypress-io/cypress/issues/4716#issuecomment-518528101
    cy.readFile(`cypress/fixtures/${k}.json`).then(info => {
      let url = 'https://storage.scrapinghub.com/jobs/1/1/' +
                `${id}?**`;
      console.log('url =>', url);
      cy.route({
        method: 'GET',
        url: url,
        status: 200,
        response: info,
        delay: 0,
      }).as(k);

      if (id == 3) {
        cy.route({
          method: 'GET',
          url: /https:\/\/storage.scrapinghub.com\/jobs\/1\/1\/([4-9]|[0-9]{2,}.*)/,
          status: 200,
          response: info,
          delay: 0,
        }).as('jobs/more');
      }
    })
  }
}

export let setupStudioTest = () => {
  cy.window().then(win => {
    win.localStorage.setItem(":sctools/api-key", API_KEY)
    win.localStorage.setItem("day8.re-frame-10x.show-panel", '"false"')
  })

  // Visit the home page empty page so we can release the indexed db
  cy.visit('/')
  cy.clearJobInfoCache()

  cy.server()
  stubJobsInfoResponse()
}

export let range = (x: number, start = 0) => {
  return Array.from({length: x}, (_, i) => i + start);
}
