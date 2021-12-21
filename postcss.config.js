module.exports = {
    plugins: [
      require('postcss-import'),
      require('tailwindcss/nesting'),
      require('tailwindcss'),
      require('postcss-nested'),
      require('postcss-preset-env')({
        stage: 1,
        // Fix a bug
        // https://github.com/nuxt-community/tailwindcss-module/issues/79#issuecomment-609693459
        features: {
            'focus-within-pseudo-class': false,
        },
      }),
      process.env.NODE_ENV === 'prod' && require('@fullhuman/postcss-purgecss')({
        content: [
          './resources/app/**/*.html',
          './src/**/*.cljs'
        ],
        defaultExtractor: function(content) {
          const v1 = content.match(/[A-Za-z0-9-_:/]+/g) || [];
          const v2 = content.match(/[A-Za-z0-9-_/]+/g) || [];
          const v3 = content.match(/[A-Za-z0-9-_:/.]+/g) || [];
          const v4 = content.match(/[A-Za-z0-9-_/.]+/g) || [];
          return v1.concat(v2).concat(v3).concat(v4);
        }
      })
    ]
}
