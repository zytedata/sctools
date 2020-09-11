const API_KEY = 'ffffffffffffffffffffffffffffffff'

import { clickByText, fillInput, clearInput, stubJobsInfoResponse} from './utils.js'

// import debug from 'debug'
// debug.disable('cypress:*')

let range = (x, start = 0) => {
  return Array.from({length: x}, (_, i) => i + start);
}

function setupTest() {
  cy.window().then(win => {
    win.localStorage.setItem(":sctools/api-key", API_KEY)
    win.localStorage.setItem("day8.re-frame-10x.show-panel", '"false"')
  })

  // cy.on('command:start', cmd => console.warn('cmd:', cmd))

  // Visit the home page empty page so we can release the indexed db
  cy.visit('/')
  cy.clearJobInfoCache()

  // cy.visit('/')
  // cy.get('body').type('{ctrl}h')

  cy.server()
  stubJobsInfoResponse()
}

const JOBS_URL = '#/studio/job/1/1/1/_/3'

describe('load jobs', () => {
  beforeEach(setupTest)

  it('fill the jobs manually', () => {
    cy.visit('/')
    clickByText('go to the jobs studio')
    fillInput('from-job', '1/1/1')
    fillInput('to-job', '1/1/3')
    clickByText('go!')

    cy.hash().should('eq', JOBS_URL)

    cy.wait(['@jobs/1', '@jobs/2', '@jobs/3'])

    cy.get("tr[data-cy=infos-row]").should('has.length', 3)
  })

  it('go to the page directly', () => {
    cy.visit(JOBS_URL)
    cy.wait(['@jobs/1', '@jobs/2', '@jobs/3'])
    cy.get("tr[data-cy=infos-row]").should('has.length', 3)
    for (let k of [1000, 2000, 3000]) {
      cy.get("td[data-cy=job-items]").contains(String(k))
    }
  })

  it('cache job status', done => {
    cy.visit('/#/studio/job/1/1/1/_/3')
    cy.wait(['@jobs/1', '@jobs/2', '@jobs/3'])
    cy.get("tr[data-cy=infos-row]").should('has.length', 3)

    cy.reload()

    cy.wait(['@jobs/1', '@jobs/2', '@jobs/3'], {timeout: 1000})
    cy.get("tr[data-cy=infos-row]").should('has.length', 3)

    cy.on('fail', err => {
      // console.log('errxxx:', err);
      expect(err.message).to.contain('No request ever occurred')
      done()
    })
  })

  it.only('sorts by job id by default', () => {
    cy.visit('#/studio/job/1/1/1/_/10')
    cy.wait(['@jobs/1', '@jobs/2', '@jobs/3', '@jobs/more'])
    cy.get("a[data-cy=job]").then(links => {
      const jobs = Array.from(links).map(link => Cypress.$(link).text())
      assert.deepEqual(jobs, range(10, 1).map(i => `1/1/${i}`))
    })
  })

})

// describe('placeholder', () => {
//   it('placeholder', () => {
//     cy.wait(100)
//   })
// })
