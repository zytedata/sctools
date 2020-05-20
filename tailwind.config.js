module.exports = {
  theme: {
    extend: {
      screens: {
        'lt-xl': {'max': '1279px'},
        'lt-lg': {'max': '1023px'},
        'lt-md': {'max': '767px'},
        'lt-sm': {'max': '639px'},
      },
    },
  },
  variants: {
    backgroundColor: ['responsive', 'hover', 'focus', 'active'],
    fontSize: ['responsive', 'hover', 'focus'],
    display: ['responsive', 'hover', 'group-hover'],
  }
}
