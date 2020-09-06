// console.log('describe', describe)
describe('The Home Page', () => {
  console.log('it', it)
  it('successfully loads', () => {
    cy.visit('/index-dev.html')
    cy.get('body').type('{ctrl}h')
    // cy.window().then((win) => {
    //   win.localStorage.setItem("day8.re-frame-10x.show-panel", "false")
    // });
    cy.get('input[name="api-key"]').type('ffffffffffffffffffffffffffffffff')
    cy.contains('Test').click()
    cy.contains('Go').click()
  })
})


// describe('My First Fail Test', () => {
//   it('Does not do much!', () => {
//     expect(true).to.equal(false)
//   })
// })
