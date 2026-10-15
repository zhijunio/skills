#!/usr/bin/env node

import { spawnSync } from 'node:child_process'
import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const args = process.argv.slice(2)
if (args.includes('--help') || args.includes('-h')) {
  console.log(`Generate projects with create-project.mjs and run their builds.

Requires network access to start.spring.io and a running Docker daemon.

Usage:
  node scripts/smoke-test.mjs [--keep]

Options:
  --keep   Keep the generated projects for inspection
  --help   Show this help`)
  process.exit(0)
}
const unknown = args.filter((arg) => arg !== '--keep')
if (unknown.length > 0) {
  console.error(`Unknown option: ${unknown.join(', ')}`)
  process.exit(1)
}

const windows = process.platform === 'win32'
const mvnw = windows ? 'mvnw.cmd' : './mvnw'
const gradlew = windows ? 'gradlew.bat' : './gradlew'

const builds = [
  { name: 'maven-jpa', options: [], commands: [[mvnw, '-q', '-B', 'verify']] },
  { name: 'gradle-jpa', options: ['--build', 'gradle'], commands: [[gradlew, '-q', 'check']] },
  { name: 'maven-mybatis', options: ['--persistence', 'mybatis-plus'], commands: [[mvnw, '-q', '-B', 'verify']] },
  { name: 'maven-nodb', options: ['--database', 'none'], commands: [[mvnw, '-q', '-B', 'verify']] },
  {
    name: 'maven-docker',
    options: ['--docker'],
    commands: [[mvnw, '-q', '-B', 'verify'], ['docker', 'compose', '-f', 'compose.full.yaml', 'config', '-q']],
  },
]

const rejections = [
  { name: 'jpa-without-database', options: ['--database', 'none', '--persistence', 'jpa'] },
  { name: 'boot-3.5', options: ['--boot-version', '3.5.9', '--java', '21'] },
  { name: 'java-patch-version', options: ['--java', '21.0.1'] },
  { name: 'docker-with-gradle', options: ['--docker', '--build', 'gradle'] },
]

function run(command, commandArgs, cwd) {
  const result = spawnSync(command, commandArgs, { cwd, encoding: 'utf8', shell: windows })
  return { ok: result.status === 0, output: `${result.stdout ?? ''}${result.stderr ?? ''}` }
}

function generate(name, options, outputDir) {
  return run(process.execPath, [join(root, 'scripts', 'create-project.mjs'), '--name', name, '--output-dir', outputDir, ...options], root)
}

const workDir = await mkdtemp(join(tmpdir(), 'spring-boot-developer-smoke-'))
let failures = 0

function report(ok, name, detail) {
  console.log(`${ok ? 'PASS' : 'FAIL'} ${name}`)
  if (!ok) {
    failures += 1
    console.log(detail.trim().split(/\r?\n/).slice(-20).join('\n'))
  }
}

try {
  for (const { name, options } of rejections) {
    const result = generate(name, options, workDir)
    report(!result.ok && result.output.includes('Project generation failed'), `rejects ${name}`, result.output || 'generation unexpectedly succeeded')
  }

  for (const { name, options, commands } of builds) {
    const generated = generate(name, options, workDir)
    if (!generated.ok) {
      report(false, `${name}: generate`, generated.output)
      continue
    }
    for (const [command, ...commandArgs] of commands) {
      const result = run(command, commandArgs, join(workDir, name))
      report(result.ok, `${name}: ${[command, ...commandArgs].join(' ')}`, result.output)
      if (!result.ok) break
    }
  }
} finally {
  if (args.includes('--keep')) console.log(`Projects kept in ${workDir}`)
  else await rm(workDir, { recursive: true, force: true })
}

if (failures > 0) {
  console.log(`${failures} check(s) failed`)
  process.exitCode = 1
}
