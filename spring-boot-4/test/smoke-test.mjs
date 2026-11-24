#!/usr/bin/env node

import { spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import { mkdtemp, readFile, readdir, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const versions = JSON.parse(await readFile(join(root, 'versions.json'), 'utf8'))
const args = process.argv.slice(2)
if (args.includes('--help') || args.includes('-h')) {
  console.log(`Generate a Boot 4 project, run doctor --strict, and run ./mvnw verify.

Requires network access to start.spring.io.

Usage:
  node test/smoke-test.mjs [--keep]

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
const workDir = await mkdtemp(join(tmpdir(), 'spring-boot-4-smoke-'))
let failures = 0

function javaMajor() {
  const result = spawnSync('java', ['-version'], { encoding: 'utf8' })
  const text = `${result.stdout ?? ''}${result.stderr ?? ''}`
  const match = text.match(/version "(\d+)/)
  return match ? Number(match[1]) : 0
}

function run(command, commandArgs, cwd = root) {
  const result = spawnSync(command, commandArgs, { cwd, encoding: 'utf8', shell: windows, env: process.env })
  return { ok: result.status === 0, output: `${result.stdout ?? ''}${result.stderr ?? ''}` }
}

function generate(name, options, outputDir) {
  return run(process.execPath, [join(root, 'scripts', 'create-project.mjs'), '--name', name, '--output-dir', outputDir, ...options])
}

function report(ok, name, detail = '') {
  console.log(`${ok ? 'PASS' : 'FAIL'} ${name}`)
  if (!ok) {
    failures += 1
    if (detail) console.log(detail.trim().split(/\r?\n/).slice(-30).join('\n'))
  }
}

async function readFileList(directory) {
  const result = []
  if (!(await readdir(directory).catch(() => null))) return result
  for (const item of await readdir(directory, { withFileTypes: true })) {
    const path = join(directory, item.name)
    if (item.isDirectory()) result.push(...await readFileList(path))
    else result.push(path)
  }
  return result
}

async function liveBootIds() {
  const response = await fetch('https://start.spring.io/metadata/client', { signal: AbortSignal.timeout(30_000) })
  if (!response.ok) throw new Error(`Initializr metadata HTTP ${response.status}`)
  const data = await response.json()
  return (data.bootVersion?.values ?? []).map((value) => value.id)
}

try {
  const runtimeJava = javaMajor()
  report(runtimeJava >= Number(versions.java.default), `JDK ${versions.java.default}+ is available for ./mvnw verify`, `java major: ${runtimeJava}`)
  if (runtimeJava < Number(versions.java.default)) {
    console.log(`Set JAVA_HOME to a JDK ${versions.java.default}+ install before running the smoke test.`)
  }

  const skill = await readFile(join(root, 'SKILL.md'), 'utf8')
  report(skill.includes('## When to use') && skill.includes('## When not to use'), 'SKILL.md has when to use / when not to use')
  report(skill.includes('## Stack references') && skill.includes('references/testing.md'), 'SKILL.md discloses stack references')
  report(!skill.includes('](references/'), 'SKILL.md does not link missing reference files')

  const help = run(process.execPath, [join(root, 'scripts', 'create-project.mjs'), '--help'])
  report(help.ok && help.output.includes('--persistence') && help.output.includes('mybatis-plus'), 'create-project --help lists MyBatis-Plus default', help.output)

  for (const { name, options } of [
    { name: 'boot-3.5', options: ['--boot-version', '3.5.9', '--java', '21'] },
    { name: 'java-patch-version', options: ['--java', '21.0.1'] },
    { name: 'mp-without-database', options: ['--database', 'none', '--persistence', 'mybatis-plus'] },
  ]) {
    const result = generate(name, options, workDir)
    report(!result.ok && result.output.includes('Project generation failed'), `rejects ${name}`, result.output || 'generation unexpectedly succeeded')
  }

  const bootIds = await liveBootIds()
  report(bootIds.includes(versions.springBoot.default), `versions.json Boot pin ${versions.springBoot.default} is live on Initializr`)

  const defaultName = 'maven-mybatis'
  const generated = generate(defaultName, [], workDir)
  if (!generated.ok) {
    report(false, `${defaultName}: generate`, generated.output)
  } else {
    const projectDir = join(workDir, defaultName)
    report(!projectDir.startsWith(root + '/') && !projectDir.startsWith(root + '\\'), 'output stays outside the skill tree', projectDir)

    const pom = await readFile(join(projectDir, 'pom.xml'), 'utf8')
    report(pom.includes('mybatis-plus-spring-boot4-starter') && !pom.includes('spring-boot-starter-data-jpa'), `${defaultName}: MyBatis-Plus without JPA`)
    report(pom.includes('spring-boot-starter-flyway') && pom.includes('flyway-database-postgresql'), `${defaultName}: Flyway on Boot 4`)
    report(pom.includes('springdoc-openapi-starter-webmvc-ui') && pom.includes(versions.springdoc.default), `${defaultName}: springdoc OpenAPI`)
    report(!pom.includes('spring-boot-jackson2') && !pom.includes('spring-boot-starter-jdbc'), `${defaultName}: no Jackson 2 or JDBC starter`)
    report(pom.includes(`<java.version>${versions.java.default}</java.version>`), `${defaultName}: Java ${versions.java.default}`)
    report(
      new RegExp(`<artifactId>spring-boot-starter-parent</artifactId>\\s*<version>${versions.springBoot.maven}</version>`).test(pom),
      `${defaultName}: Boot ${versions.springBoot.maven}`,
    )

    const sources = (await readFileList(join(projectDir, 'src', 'main', 'java'))).filter((file) => file.endsWith('Application.java'))
    const application = (await Promise.all(sources.map((file) => readFile(file, 'utf8')))).join('\n')
    report(application.includes('@MapperScan'), `${defaultName}: @MapperScan`)
    const testFiles = (await readFileList(join(projectDir, 'src', 'test', 'java'))).map((file) => file.replaceAll('\\', '/'))
    report(testFiles.some((file) => file.endsWith('ApplicationTests.java')), `${defaultName}: keeps Initializr context-load test`)
    report(
      !testFiles.some((file) => file.endsWith('TestcontainersConfiguration.java') || /\/Test[^/]+Application\.java$/.test(file)),
      `${defaultName}: no extra test classes`,
    )
    report(!pom.includes('testcontainers') && !pom.includes('spring-boot-testcontainers'), `${defaultName}: no Testcontainers dependency`)
    const testYaml = await readFile(join(projectDir, 'src', 'test', 'resources', 'application.yaml'), 'utf8').catch(() => '')
    report(testYaml.includes('DataSourceAutoConfiguration'), `${defaultName}: test config excludes DataSource`)

    const compose = await readFile(join(projectDir, 'compose.yaml'), 'utf8').catch(() => '')
    report(compose.includes(versions.postgresql.image), `${defaultName}: Compose image ${versions.postgresql.image}`)
    report(!/:\s*\/var\/lib\/postgresql\/data\s*$/m.test(compose), `${defaultName}: Compose volume is not /var/lib/postgresql/data`)
    report(/:\s*\/var\/lib\/postgresql\s*$/m.test(compose), `${defaultName}: Compose volume is /var/lib/postgresql`)
    report(compose.includes('healthcheck'), `${defaultName}: Compose healthcheck`)

    const doctor = run(process.execPath, [join(root, 'scripts', 'doctor-project.mjs'), '--strict', projectDir])
    report(doctor.ok && doctor.output.includes('PASS project contract'), `${defaultName}: doctor --strict`, doctor.output)

    const pomPath = join(projectDir, 'pom.xml')
    const originalPom = pom
    await writeFile(pomPath, originalPom.replace(/<artifactId>spring-boot-starter-flyway<\/artifactId>/g, '<artifactId>spring-boot-starter-web</artifactId>'))
    const doctorFlyway = run(process.execPath, [join(root, 'scripts', 'doctor-project.mjs'), '--strict', projectDir])
    report(!doctorFlyway.ok && doctorFlyway.output.includes('flyway.missing'), `${defaultName}: doctor fails without Flyway`, doctorFlyway.output)
    await writeFile(pomPath, originalPom)

    const composePath = join(projectDir, 'compose.yaml')
    const originalCompose = compose
    await writeFile(composePath, originalCompose.replace(/postgres_data:(\s*)\/var\/lib\/postgresql$/m, 'postgres_data:$1/var/lib/postgresql/data'))
    const doctorVolume = run(process.execPath, [join(root, 'scripts', 'doctor-project.mjs'), '--strict', projectDir])
    report(!doctorVolume.ok && doctorVolume.output.includes('compose.volume'), `${defaultName}: doctor fails on /var/lib/postgresql/data`, doctorVolume.output)
    await writeFile(composePath, originalCompose)

    const verify = run(mvnw, ['-q', '-B', 'verify'], projectDir)
    report(verify.ok, `${defaultName}: ./mvnw verify`, verify.output)
  }

  const nodb = generate('maven-nodb', ['--persistence', 'none', '--database', 'none'], workDir)
  if (!nodb.ok) report(false, 'maven-nodb: generate', nodb.output)
  else {
    const nodbDir = join(workDir, 'maven-nodb')
    const nodbPom = await readFile(join(nodbDir, 'pom.xml'), 'utf8')
    report(!nodbPom.includes('mybatis-plus-spring-boot4-starter') && nodbPom.includes('springdoc-openapi-starter-webmvc-ui'), 'maven-nodb: web plus OpenAPI without persistence')
    report(!existsSync(join(nodbDir, 'compose.yaml')), 'maven-nodb: compose.yaml is absent')
    const nodbDoctor = run(process.execPath, [join(root, 'scripts', 'doctor-project.mjs'), '--strict', nodbDir])
    report(nodbDoctor.ok && nodbDoctor.output.includes('PASS project contract'), 'maven-nodb: doctor --strict', nodbDoctor.output)
    const nodbVerify = run(mvnw, ['-q', '-B', 'verify'], nodbDir)
    report(nodbVerify.ok, 'maven-nodb: ./mvnw verify', nodbVerify.output)
  }
} finally {
  if (args.includes('--keep')) console.log(`Projects kept in ${workDir}`)
  else await rm(workDir, { recursive: true, force: true })
}

if (failures > 0) {
  console.log(`${failures} check(s) failed`)
  process.exitCode = 1
}
