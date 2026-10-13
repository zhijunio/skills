#!/usr/bin/env node

import { execFileSync } from 'node:child_process'
import { readFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const versions = JSON.parse(await readFile(resolve(root, 'versions.json'), 'utf8'))
const args = new Set(process.argv.slice(2))
const needNode = args.has('--frontend')
const needDocker = args.has('--docker')
const needGradle = args.has('--gradle')
let failures = 0

function version(command, commandArgs) {
  try {
    return execFileSync(command, commandArgs, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim()
  } catch {
    return null
  }
}

function numericVersion(value) {
  const match = value?.match(/\d+(?:\.\d+)*/)
  return match ? match[0].split('.').map(Number) : null
}

function displayVersion(value) {
  const match = value?.match(/\d+(?:\.\d+){1,3}/)
  return match?.[0] ?? value.split(/\r?\n/, 1)[0]
}

function compareVersions(left, right) {
  const a = numericVersion(left)
  const b = numericVersion(right)
  if (!a || !b) return null
  for (let i = 0; i < Math.max(a.length, b.length); i += 1) {
    const delta = (a[i] ?? 0) - (b[i] ?? 0)
    if (delta !== 0) return Math.sign(delta)
  }
  return 0
}

function report(label, value, minimum, required, maximum) {
  if (!value) {
    console.log(`${required ? 'FAIL' : 'OPTIONAL'} ${label}: not found`)
    if (required) failures += 1
    return
  }
  const meetsMinimum = compareVersions(value, minimum) >= 0
  const meetsMaximum = !maximum || compareVersions(maximum, value) >= 0
  if (meetsMinimum && meetsMaximum) console.log(`OK   ${label}: ${displayVersion(value)}`)
  else {
    const range = maximum ? `${minimum}-${maximum}` : `minimum ${minimum}`
    console.log(`${required ? 'FAIL' : 'WARN'} ${label}: ${displayVersion(value)} (required ${range})`)
    if (required) failures += 1
  }
}

report('Java', version('java', ['--version']) || version('java', ['-version']), versions.java.minimum, true, versions.java.maximumForSpringBootDefault)
report('Maven', version('mvn', ['--version']), versions.mavenMinimum, false)
report('Gradle', version('gradle', ['--version']), versions.gradleMinimum, needGradle)
report('Node.js', version('node', ['--version']), needNode ? versions.node.minimum : '20', needNode)
report('npm', version('npm', ['--version']), needNode ? versions.node.npmMinimum : '10', needNode)
report('Docker', version('docker', ['--version']), '1', needDocker)

if (failures > 0) process.exitCode = 1
