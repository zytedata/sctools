const API_KEY = 'ffffffffffffffffffffffffffffffff';

beforeEach(() => {
  cy.visit('/')
  // cy.window().then((win) => {
  //   win.localStorage.setItem("day8.re-frame-10x.show-panel", "false")
  // });
  cy.get('body').type('{ctrl}h')
  cy.server()
})

function stubAPIKeyRequest(status) {
  cy.route({
    method: 'GET',
    url: `https://jobq.scrapinghub.com/?apikey=${API_KEY}`,
    status: status,
    response: {},
    delay: 100,
  }).as('verifyAPIKey')
}

function submitAPIKey() {
  cy.get('input[name="api-key"]').type(API_KEY)
  cy.contains('Test').click()
  cy.wait('@verifyAPIKey')
}

describe('set api key', () => {
  it('successfully login', () => {
    stubAPIKeyRequest(400)
    submitAPIKey()

    cy.contains('Go').click()
  })

  it('wrong login', () => {
    stubAPIKeyRequest(401)
    submitAPIKey()

    cy.contains('API Key check failed')
  })

  it('redirects back', () => {
    cy.visit('/#/settings')
    cy.hash().should('eq', '#/init')

    stubAPIKeyRequest(400)
    submitAPIKey()

    cy.hash().should('eq', '#/settings')
  })
})
