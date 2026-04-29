export default {
  locales: ['en', 'es', 'de'],
  output: 'src/i18n/$LOCALE.json',
  input: ['src/**/*.{ts,tsx}'],
  defaultNamespace: 'translation',
  keySeparator: '.',
  namespaceSeparator: ':',
  sort: true,
}
