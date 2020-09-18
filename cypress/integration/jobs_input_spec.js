import { clickByText, fillInput, clearInput, stubJobsInfoResponse} from './utils.js'
import { setupStudioTest, range, JOBS_URL } from './test_utils.js'

beforeEach(setupStudioTest)

describe('load jobs', () => {

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

  // This case must be the last one in the whole file, otherise any
  // test case comes after it would fail for no good reason.
  it('cache job status', done => {
    cy.visit('/#/studio/job/1/1/1/_/3')
    cy.wait(['@jobs/1', '@jobs/2', '@jobs/3'])
    cy.get("tr[data-cy=infos-row]").should('has.length', 3)

    cy.reload()

    cy.wait(['@jobs/1', '@jobs/2', '@jobs/3'], {timeout: 1000})
    cy.get("tr[data-cy=infos-row]").should('has.length', 3)

    cy.once('fail', err => {
      // console.log('errxxx:', err);
      expect(err.message).to.contain('No request ever occurred')
      done()
    })
  })

})
