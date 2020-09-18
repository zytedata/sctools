import { clickByText, fillInput, clearInput, stubJobsInfoResponse } from './utils.js'
import { setupStudioTest, range, JOBS_URL } from './test_utils.js'

beforeEach(() => {
  setupStudioTest()
  cy.visit(JOBS_URL)
  cy.wait(['@jobs/1', '@jobs/2', '@jobs/3'])
})

let assertJobs = jobs => {
    cy.get("a[data-cy=job]")
      .should(links => {
        const _jobs = Array.from(links).map(link => Cypress.$(link).text())
        assert.deepEqual(_jobs, jobs)
      })
}

describe('sort jobs', () => {
  it('sorts by job id by default', () => {
    cy.visit('#/studio/job/1/1/1/_/20')
    cy.reload()
    cy.wait('@jobs/more')
    assertJobs(range(20, 1).map(i => `1/1/${i}`))
  })

  it('sorts by one column', () => {
    cy.get('th[data-cy=col-items]').find('[data-cy=sort-col]').click({force: true})
    // indicator change
    cy.get('th[data-cy=col-items]').find('[data-cy=sorted-col]').should('exist')

    // data rows change
    assertJobs(['1/1/2', '1/1/1', '1/1/3'])

    // reverse
    cy.get('th[data-cy=col-items]').find('[data-cy=sorted-col]').click()
    assertJobs(['1/1/3', '1/1/1', '1/1/2'])

    // clear sorting
    cy.get('th[data-cy=col-items]')
      .find('[data-cy=clear-sorting]')
      .click({force: true})
    assertJobs(['1/1/1', '1/1/2', '1/1/3'])
  })

})

let assertColumn = (col, exist) => {
  cy.get(`th[data-cy^=col-`)
    .contains(col)
    .should(exist ? 'exist' : 'not.exist')
}

let withDialogOpen = f => {
  cy.get('[data-cy=show-studio-preference]').click()

  f()

  cy.get('[data-cy=close-preference-dialog]').click()

  cy.get('[data-cy=studio-preference-dialog]').should('not.exist')
}

describe('show/hide columns', () => {
  it('show hide columns', () => {
    assertColumn('Logs', false)
    withDialogOpen(() => {
      cy.get('[data-cy=studio-preference-dialog]').contains('Logs').click()
    })
    assertColumn('Logs', true)

    assertColumn('Items', true)
    withDialogOpen(() => {
      cy.get('[data-cy=studio-preference-dialog]').contains('Items').click()
    })
    assertColumn('Items', false)
  })
})

let addStat = stat => {
  assertColumn(stat, false)
  withDialogOpen(() => {
    clickByText('stats to display')
    cy.get('input[id=stats]').type(stat).type('{downarrow}')
    cy.get('input[id=stats]').type('{enter}')
  })
  assertColumn(stat, true)
}

let removeStat = stat => {
  assertColumn(stat, true)
  withDialogOpen(() => {
    clickByText('stats to display')
    cy.get('[data-cy=hide-stat]')
      .contains(stat)
      .closest('[data-cy=hide-stat]')
      .find('[data-cy=hide-stat-button]')
      .click()
  })
  assertColumn(stat, false)
}


describe('show/hide stats', () => {
  it('show/hide one stat ', () => {

    addStat('200')
    addStat('502')

    removeStat('200')
    removeStat('502')
  })
})

describe('sort by stats', () => {
  it('sort by one stat column', () => {
    addStat('200')
    cy.get('th[data-cy=col-stat]').find('[data-cy=sort-col]').click({force: true})
    // indicator change
    cy.get('th[data-cy=col-stat]').find('[data-cy=sorted-col]').should('exist')

    // data rows change
    assertJobs(['1/1/3', '1/1/1', '1/1/2'])

    // reverse
    cy.get('th[data-cy=col-stat]').find('[data-cy=sorted-col]').click()
    assertJobs(['1/1/2', '1/1/1', '1/1/3'])

    // clear sorting
    cy.get('th[data-cy=col-stat]')
      .find('[data-cy=clear-sorting]')
      .click({force: true})
    assertJobs(['1/1/1', '1/1/2', '1/1/3'])
  })
})
