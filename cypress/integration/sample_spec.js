describe('My First Test', () => {
  it('Does not do much!', () => {
    expect(true).to.equal(true)
    // cy.pause()
  })
})

// describe('My First Fail Test', () => {
//   it('Does not do much!', () => {
//     expect(true).to.equal(false)
//   })
// })

// console.log(describe);
// console.log(cy);
// describe('My First UI Test', () => {
//   it('Visits the Kitchen Sink', () => {
//     cy.visit('https://example.cypress.io')
//     //     cfindy.pause()
//     cy.contains('type').click()
//     cy.url().should('include', '/commands/actions')
//     cy.get('.action-email')
//       .type('fake@email.com')
//       .should('have.value', 'fake@email.com')
//   })
// })
