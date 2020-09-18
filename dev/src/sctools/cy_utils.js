const utils = {}
export default utils;

// For REPL interactive dev with cypress
export function _wrapForceRun(fn, name) {
  name = name || /_(.*)/.exec(fn.name)[1];
  utils[name] = fn;
  const newFn = function (... args) {
    let ret = fn.apply(null, args);
    if (window._isCyApp) {
      vrun();
    }
    return ret;
  }
  //   newFn.name = name;
  return newFn;
}

utils._wrapForceRun = _wrapForceRun;

function _clickByText() {
  let sel, text;
  if (arguments.length > 1) {
    [sel, text] = arguments;
  } else {
    [text] = arguments;
  }
  let chain = cy;
  if (sel) {
    chain = chain.get(sel);
  }
  return chain.contains(text, {matchCase: false}).click();
}
export const clickByText = _wrapForceRun(_clickByText);

function _simpleCmd(cmd, ...bindArgs) {
  return _wrapForceRun(function(...args) {
    bindArgs.push(...args);
    return cy[cmd].apply(null, bindArgs);
  }, cmd);
}

export const wait = _simpleCmd('wait');
export const get = _simpleCmd('get');
export const reload = _simpleCmd('reload');
export const back = _simpleCmd('go', 'back');
export const forward = _simpleCmd('go', 'forward');
export const visit = _simpleCmd('visit');

function _inputSel(name) {
  return `input[name="${name}"]`;
}

function _clearInput(name) {
  return cy.get(_inputSel(name)).clear();
}
export const clearInput = _wrapForceRun(_clearInput);

function _fillInput(name, text) {
  return cy.get(_inputSel(name)).clear().type(text);
}
export const fillInput = _wrapForceRun(_fillInput);

function _stubJobsInfoResponse() {
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
        delay: 100,
      }).as(k);

      if (id == 3) {
        cy.route({
          method: 'GET',
          url: /https:\/\/storage.scrapinghub.com\/jobs\/1\/1\/([4-9]|[0-9]{2,}.*)/,
          status: 200,
          response: info,
          delay: 100,
        }).as('jobs/more');
      }
    })
  }
}

export const stubJobsInfoResponse = _wrapForceRun(_stubJobsInfoResponse);

// Pass the `cy` object into the iframe window where the application
// runs, so it could call `cy.visit` etc. from there.
function _attachFns(root, child) {
  child._isCyApp = true;
  child.cy = root.cy;
  child.vrun = root.vrun;
  child.Cypress = root.Cypress;
}

function _populateCy() {
  const root = window.parent;

  root.document.querySelectorAll('iframe').forEach(iframe => {
    if (/Your App/.exec(iframe.id)) {
      iframe.onload = () => {
        _attachFns(root, iframe.contentWindow);
      };
    }
  });
}

// Prevent _populateCy to run inside application iframe
if (!window._isCyApp) {
  _populateCy();
} else {
  // Expose to the top level window.
  window.parent.cy_utils = utils;
}

// function _wrapFns(fns) {
//   return Object.fromEntries(
//     Object.entries(fns).map(([k, f]) => [k, _wrapForceRun(f)])
//   );
// }
//
// const utils = _wrapFns({
//   clickByText,
//   fillInput,
// });
//
