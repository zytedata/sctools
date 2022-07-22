import { clickByText  } from './utils'
import { setupStudioTest, range, JOBS_URL } from './test_utils'

const visitJobsURL = () => {
  cy.visit(JOBS_URL)
  cy.wait(['@jobs/1', '@jobs/2', '@jobs/3'])
}

beforeEach(() => {
  setupStudioTest()
  visitJobsURL()
})

let assertJobs = (jobs: string[]) => {
    cy.get("a[data-cy=job]")
      .should(links => {
        const _jobs = Array.from(links).map(link => Cypress.$(link).text())
        assert.deepEqual(_jobs, jobs)
      })
}

let assertIndicatorExist = (parent: string, exist: boolean) => {
  return cy.get(parent)
           .find('[data-cy=sort-indicator]')
           .should(exist ? 'exist' : 'not.exist')
}

describe('sort jobs', () => {
  it('sorts by job id by default', () => {
    cy.visit('#/studio/job/1/1/1/_/20')
    cy.reload()
    cy.wait('@jobs/more')
    assertJobs(range(20, 1).map(i => `1/1/${i}`))
  })

  it('sorts by one column', () => {
    cy.get('th[data-cy=col-items]').trigger('mouseover')
    cy.get('[data-cy=sort-col]').click()
    assertIndicatorExist('th[data-cy=col-items]', true)

    // data rows change
    assertJobs(['1/1/2', '1/1/1', '1/1/3'])

    // reverse
    cy.get('th[data-cy=col-items]').trigger('mouseover', {force: true})
    cy.get('[data-cy=sorted-col]').click()
    assertJobs(['1/1/3', '1/1/1', '1/1/2'])

    // clear sorting
    cy.get('[data-cy=clear-sorting]').click()
    assertJobs(['1/1/1', '1/1/2', '1/1/3'])
    assertIndicatorExist('th[data-cy=col-items]', false)
  })

})

let assertColumn = (col: string, exist: boolean) => {
  cy.get(`th[data-cy^=col-`)
    .contains(col)
    .should(exist ? 'exist' : 'not.exist')
}

let withDialogOpen = (f: Function) => {
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

let addStat = (stat: string) => {
  assertColumn(stat, false)
  withDialogOpen(() => {
    clickByText('stats to display')
    cy.get('input[id=stats]').type(stat).type('{downarrow}')
    cy.get('input[id=stats]').type('{enter}')
  })
  assertColumn(stat, true)
}

let removeStat = (stat: string) => {
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
    cy.get('th[data-cy=col-stat]').trigger('mouseover')
    cy.get('[data-cy=sort-col]').click()
    assertIndicatorExist('th[data-cy=col-stat]', true)

    // data rows change
    assertJobs(['1/1/3', '1/1/1', '1/1/2'])

    // reverse
    cy.get('th[data-cy=col-stat]').trigger('mouseover', {force: true})
    cy.get('[data-cy=sorted-col]').click()
    assertJobs(['1/1/2', '1/1/1', '1/1/3'])

    // clear sorting
    cy.get('[data-cy=clear-sorting]').click()
    assertJobs(['1/1/1', '1/1/2', '1/1/3'])
    assertIndicatorExist('th[data-cy=col-stat]', false)
  })
})

let checkChart = () => {
  cy.get('.vega-embed svg').should('exist')
  cy.get('svg').contains('Items').should('exist')
  cy.get('svg path[transform]')
    .should('exist')
}

describe('chart by column', () => {
  it('chart by one column', () => {
    cy.get('th[data-cy=col-items]').trigger('mouseover')
    cy.get('[data-cy=visualize-col]').click()
    cy.hash().should('contains', '#/studio/chart/')
    checkChart()
  })


  it('visit chart url directly', () => {
    cy.visit('/')
    cy.reload()
    cy.visit('#/studio/chart/1/1/1/_/3?q=%2522%255B%255C%2522%255E%2520%255C%2522%252C%255C%2522~%253Aid%255C%2522%252C%255C%2522~%253Aitems%255C%2522%252C%255C%2522~%253Astat%253F%255C%2522%252Cnull%255D%2522')
    checkChart()
  })
})

describe('chart by stat', () => {
  it('chart by one column', () => {
    addStat('200')
    cy.get('th[data-cy=col-stat]').trigger('mouseover')
    cy.get('[data-cy=visualize-col]').click()
    cy.hash().should('contains', '#/studio/chart/')
    cy.get('.vega-embed svg').should('exist')
  })
})


describe('drag & drop to reorder columns', () => {
  const assertOrdering = (ordering: string[], msg=null) => {
    cy.get('th[data-cy*=col-]')
      .then((cols) => {
        const names = cols.map((_, el) => el.getAttribute('data-cy'))
        const match = Array.from(names.filter((_, x) => ordering.indexOf(x) >= 0))
        assert.deepEqual(match, ordering, msg)
      })
  }
  it('reorder a non-stat column', () => {
    assertOrdering(['col-state', 'col-items']);
    cy.get('th[data-cy=col-state]').trigger('mouseover');
    cy.get('[data-cy=sctools-col-popover]').drag('[data-cy=col-pages]', {
      // This is required to emulate the series of drag and drop related events.
      force: true,
    });
    // Wait for the dnd events to be processed
    cy.wait(1000);
    assertOrdering(
      ['col-items', 'col-state'],
      'The "state" col shall be moved to be after "items" col by drag and drop'
    );

    // Clear the ordering in settings
    cy.visit('/#/settings');
    cy.get('[data-cy=ordering-clear-btn]').click();
    cy.get('[data-cy=ordering-clear-success]');

    cy.visit(JOBS_URL);
    assertOrdering(['col-state', 'col-items'], 'The ordering shall be reset');
  });

  it('reorder a stat column', () => {
    addStat('200')
    assertOrdering(['col-items', 'col-stat']);
    cy.get('th[data-cy=col-stat]').trigger('mouseover');
    cy.get('[data-cy=sctools-col-popover]').drag('[data-cy=col-state]', {
      // This is required to emulate the series of drag and drop related events.
      force: true,
    });
    // Wait for the dnd events to be processed
    cy.wait(1000);
    assertOrdering(
      ['col-stat', 'col-items'],
      'The "state" col shall be moved to be after "items" col by drag and drop'
    );
  });
})
