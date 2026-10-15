#!/usr/bin/env node

import { readFile, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const versions = JSON.parse(await readFile(resolve(root, 'versions.json'), 'utf8'))
const args = process.argv.slice(2)
if (args.includes('--help') || args.includes('-h')) {
  console.log(`Update version tables in references from versions.json.

Usage:
  node scripts/sync-versions-in-docs.mjs [--check]

Options:
  --check   Report outdated tables without writing files
  --help    Show this help`)
  process.exit(0)
}
const unknown = args.filter((arg) => arg !== '--check')
if (unknown.length > 0) {
  console.error(`Unknown option: ${unknown.join(', ')}`)
  process.exit(1)
}
const checkOnly = args.includes('--check')
const start = '<!-- versions:start -->'
const end = '<!-- versions:end -->'
const frontendRows = (frameworkRows) => [
  ['Node.js', versions.node.default],
  ['npm', versions.node.npm],
  ...frameworkRows,
]
const tables = new Map([
  ['frontend/vue.md', frontendRows([
    ['Vue.js', `${versions.vue.major}.x`],
    ['Vite', `${versions.vue.viteMajor}.x`],
    ['Pinia', `${versions.vue.piniaMajor}.x`],
    ['Vue Router', `${versions.vue.routerMajor}.x`],
  ])],
  ['frontend/react.md', frontendRows([
    ['React', `${versions.react.major}.x`],
    ['Vite', `${versions.vue.viteMajor}.x`],
    ['React Router', `${versions.react.routerMajor}.x`],
  ])],
  ['frontend/angular.md', frontendRows([
    ['Angular', `${versions.angular.major}.x`],
    ['Angular Router', `${versions.angular.major}.x`],
  ])],
  ['frontend/vanilla-js.md', frontendRows([
    ['Vite', `${versions.vue.viteMajor}.x`],
    ['Bootstrap', versions.frontend.bootstrap],
  ])],
  ['deployment/azure.md', [
    ['PostgreSQL', versions.postgresql.defaultMajor],
    ['Java (Temurin)', versions.java.default],
  ]],
  ['deployment/graalvm.md', [
    ['GraalVM', versions.graalvm.defaultMajor],
    ['Java (Temurin)', versions.java.default],
    ['PostgreSQL', versions.postgresql.defaultMajor],
  ]],
])

function render(rows) {
  return [
    '| Tool | Version |',
    '|------|---------|',
    ...rows.map(([name, version]) => `| ${name} | ${version} |`),
  ].join('\n')
}

let changed = []
for (const [filename, rows] of tables) {
  const path = resolve(root, 'references', filename)
  const original = await readFile(path, 'utf8')
  const updated = original.replace(new RegExp(`${start}[\\s\\S]*?${end}`), `${start}\n${render(rows)}\n${end}`)
  if (updated === original) continue
  changed.push(filename)
  if (!checkOnly) await writeFile(path, updated)
}

if (checkOnly && changed.length > 0) {
  console.error(`Version tables need updating: ${changed.join(', ')}`)
  process.exitCode = 1
} else if (changed.length > 0) {
  console.log(`Updated version tables: ${changed.join(', ')}`)
} else {
  console.log('Version tables are up to date.')
}
