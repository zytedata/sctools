process.env.CHROME_BIN = require('puppeteer').executablePath();

module.exports = function (config) {
    config.set({
      browsers: ['ChromeHeadlessNoSandbox'],
      customLaunchers: {
        ChromeCustom: {
          base: 'Chrome',
          flags: [
            '--user-data-dir=test-assets/chrome-profile-unittest',
          ]
        },
        ChromeHeadlessNoSandbox: {
          base: 'ChromeHeadless',
          flags: [
            '--no-sandbox',
            '--user-data-dir=test-assets/chrome-profile-unittest',
          ]
        }
      },
      basePath: 'test-assets/karma/',
      files: [
        'ci.js',
        {pattern: '../../resources/app/static/styles/tailwind**.css'},
        {pattern: '../../resources/app/static/fontawesome/css/fa.purged.css'},
      ],
      frameworks: ['cljs-test'],
      plugins: ['karma-cljs-test', 'karma-chrome-launcher'],
      colors: true,
      logLevel: config.LOG_INFO,
      client: {
        args: ["shadow.test.karma.init"],
        singleRun: true
      }
    })
};
