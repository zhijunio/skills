#!/usr/bin/env node

import { execFileSync } from 'node:child_process'
import { chmod, mkdtemp, mkdir, readFile, readdir, rename, rm, writeFile } from 'node:fs/promises'
import { existsSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { applyCompose } from './compose.mjs'
import { applyMavenHouseStyle } from './pom.mjs'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const versions = JSON.parse(await readFile(join(root, 'versions.json'), 'utf8'))
const timeoutMs = 60_000

function usage() {
  console.log(`Create a Spring Boot 4 project from Spring Initializr.

Usage:
  node scripts/create-project.mjs --name <name> [options]

Options:
  --name <name>                 Project directory and artifact name (required)
  --group-id <id>               Java group id (default: com.example)
  --package-name <name>         Java package (default: <group-id>.<project-name>)
  --java <version>              Java major version (default: ${versions.java.default})
  --boot-version <version>      Exact Spring Boot version (default: ${versions.springBoot.default})
  --persistence <mybatis-plus|none>  Persistence (default: mybatis-plus, or none with --database none)
  --database <postgresql|none>  Database driver (default: postgresql)
  --output-dir <path>           Parent directory (default: current directory)
  --help                        Show this help

Examples:
  node scripts/create-project.mjs --name inventory-api --output-dir /absolute/path/to/workspace
  node scripts/create-project.mjs --name api --persistence none --database none`)
}

function parseArgs(args) {
  const options = { database: 'postgresql', outputdir: process.cwd() }
  const values = new Set(['--name', '--group-id', '--package-name', '--java', '--boot-version', '--persistence', '--database', '--output-dir'])
  for (let i = 0; i < args.length; i += 1) {
    const key = args[i]
    if (key === '--help' || key === '-h') return { help: true, options }
    if (!values.has(key) || !args[i + 1] || args[i + 1].startsWith('--')) {
      throw new Error(`Unknown option or missing value: ${key}`)
    }
    options[key.slice(2).replaceAll('-', '')] = args[i + 1]
    i += 1
  }
  return { help: false, options }
}

function assertOptions(options) {
  if (!options.name) throw new Error('--name is required')
  if (!/^[a-zA-Z0-9][a-zA-Z0-9._-]*$/.test(options.name) || options.name === '.' || options.name === '..') {
    throw new Error('--name must be a plain project name, not a path')
  }
  if (!['none', 'postgresql'].includes(options.database)) throw new Error('--database must be none or postgresql')
  options.persistence ??= options.database === 'none' ? 'none' : 'mybatis-plus'
  if (!['mybatis-plus', 'none'].includes(options.persistence)) {
    throw new Error('--persistence must be mybatis-plus or none')
  }
  if (options.persistence !== 'none' && options.database === 'none') {
    throw new Error('--persistence mybatis-plus needs --database postgresql or --persistence none')
  }
  const groupId = options.groupid ?? 'com.example'
  if (!/^[A-Za-z_][\w]*(\.[A-Za-z_][\w]*)*$/.test(groupId)) throw new Error('--group-id must be a valid Java group id')
  options.groupid = groupId
  const defaultPackageSegment = options.name.replaceAll(/[^A-Za-z0-9_]/g, '').toLowerCase() || 'app'
  options.packagename ??= `${groupId}.${/^\d/.test(defaultPackageSegment) ? `app${defaultPackageSegment}` : defaultPackageSegment}`
  if (!/^[A-Za-z_][\w]*(\.[A-Za-z_][\w]*)*$/.test(options.packagename)) throw new Error('--package-name must be a valid Java package')
  options.java ??= versions.java.default
  options.bootversion ??= versions.springBoot.default
  if (!/^\d+$/.test(options.java)) throw new Error('--java must be a Java major version, such as 21')
  if (!/^\d+\.\d+\.\d+(?:\.RELEASE)?$/.test(options.bootversion)) {
    throw new Error('--boot-version must be an exact stable version, such as 4.2.0.RELEASE')
  }
  options.initializrBootVersion = /^\d+\.\d+\.\d+$/.test(options.bootversion)
    ? `${options.bootversion}.RELEASE`
    : options.bootversion
  options.mavenBootVersion = options.bootversion.replace(/\.RELEASE$/, '')
  const [bootMajor] = options.mavenBootVersion.split('.')
  if (Number(bootMajor) < 4) {
    throw new Error('This generator only creates Spring Boot 4.x projects')
  }
  const maxJava = versions.java.maximumBySpringBootMajor[bootMajor]
  if (!maxJava) throw new Error(`No compatibility baseline is recorded for Spring Boot ${bootMajor}.x`)
  if (Number(options.java) < Number(versions.java.minimum) || Number(options.java) > Number(maxJava)) {
    throw new Error(`Spring Boot ${bootMajor}.x requires Java ${versions.java.minimum}-${maxJava}`)
  }
  options.outputdir = resolve(options.outputdir)
  options.projectDir = join(options.outputdir, options.name)
  if (existsSync(options.projectDir)) throw new Error(`Target already exists: ${options.projectDir}`)
}

function run(command, args, options = {}) {
  execFileSync(command, args, { stdio: 'inherit', ...options })
}

async function extract(zipPath, destination) {
  run('unzip', ['-q', zipPath, '-d', destination])
}

async function listFiles(directory, result = []) {
  if (!existsSync(directory)) return result
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) await listFiles(path, result)
    else result.push(path)
  }
  return result
}

function insertMavenDependency(pom, xml) {
  if (pom.includes(xml)) return pom
  const closing = pom.lastIndexOf('</dependencies>')
  if (closing === -1) throw new Error('generated pom.xml has no <dependencies> block')
  return `${pom.slice(0, closing)}${xml}\n\t${pom.slice(closing)}`
}

async function addMyBatisPlus(projectDir) {
  const pomPath = join(projectDir, 'pom.xml')
  let pom = await readFile(pomPath, 'utf8')
  if (pom.includes('mybatis-plus-spring-boot4-starter')) return
  const dependency = `\n\t\t<dependency>\n\t\t\t<groupId>com.baomidou</groupId>\n\t\t\t<artifactId>mybatis-plus-spring-boot4-starter</artifactId>\n\t\t</dependency>`
  await writeFile(pomPath, insertMavenDependency(pom, dependency))
}

async function addSpringdoc(projectDir) {
  const pomPath = join(projectDir, 'pom.xml')
  let pom = await readFile(pomPath, 'utf8')
  if (pom.includes(versions.springdoc.artifact)) return
  const dependency = `\n\t\t<dependency>\n\t\t\t<groupId>org.springdoc</groupId>\n\t\t\t<artifactId>${versions.springdoc.artifact}</artifactId>\n\t\t</dependency>`
  await writeFile(pomPath, insertMavenDependency(pom, dependency))
}

function assertJackson3(pom) {
  if (pom.includes('spring-boot-jackson2') || pom.includes('spring-boot-starter-jackson2')) {
    throw new Error('Jackson 2 starters are not allowed; Boot 4 projects use Jackson 3')
  }
  if (pom.includes('gson') || pom.includes('yasson') || pom.includes('jakarta.json.bind')) {
    throw new Error('Gson and JSON-B are not allowed; Boot 4 projects use Jackson 3')
  }
}

function stripDependency(pom, artifactId) {
  return pom.replace(new RegExp(`\\s*<dependency>\\s*<groupId>[^<]+</groupId>\\s*<artifactId>${artifactId}</artifactId>[\\s\\S]*?</dependency>`, 'g'), '')
}

async function pinMavenBootVersion(projectDir, mavenVersion) {
  const pomPath = join(projectDir, 'pom.xml')
  let pom = await readFile(pomPath, 'utf8')
  pom = pom.replace(
    /(<artifactId>spring-boot-starter-parent<\/artifactId>\s*<version>)[^<]+(<\/version>)/,
    `$1${mavenVersion}$2`,
  )
  await writeFile(pomPath, pom)
}

async function removeJdbcStarter(projectDir) {
  const pomPath = join(projectDir, 'pom.xml')
  const pom = await readFile(pomPath, 'utf8')
  const next = stripDependency(pom, 'spring-boot-starter-jdbc')
  if (next !== pom) await writeFile(pomPath, next)
}

async function ensureFlyway(projectDir, database) {
  if (database !== 'postgresql') return
  const pomPath = join(projectDir, 'pom.xml')
  let pom = await readFile(pomPath, 'utf8')
  if (!pom.includes('spring-boot-starter-flyway')) {
    pom = insertMavenDependency(pom, `\n\t\t<dependency>\n\t\t\t<groupId>org.springframework.boot</groupId>\n\t\t\t<artifactId>spring-boot-starter-flyway</artifactId>\n\t\t</dependency>`)
  }
  if (!pom.includes('flyway-database-postgresql')) {
    pom = insertMavenDependency(pom, `\n\t\t<dependency>\n\t\t\t<groupId>org.flywaydb</groupId>\n\t\t\t<artifactId>flyway-database-postgresql</artifactId>\n\t\t</dependency>`)
  }
  await writeFile(pomPath, pom)
  await mkdir(join(projectDir, 'src', 'main', 'resources', 'db', 'migration'), { recursive: true })
}

async function addMapperScan(projectDir, packageName) {
  const javaRoot = join(projectDir, 'src', 'main', 'java')
  const files = (await listFiles(javaRoot)).filter((file) => file.endsWith('Application.java'))
  if (files.length === 0) throw new Error('Spring Initializr did not generate an Application class')
  const path = files[0]
  let source = await readFile(path, 'utf8')
  if (!source.includes('@MapperScan')) {
    if (!source.includes('import org.mybatis.spring.annotation.MapperScan;')) {
      source = source.replace('import org.springframework.boot.autoconfigure.SpringBootApplication;\n', 'import org.mybatis.spring.annotation.MapperScan;\nimport org.springframework.boot.autoconfigure.SpringBootApplication;\n')
    }
    source = source.replace('@SpringBootApplication', `@SpringBootApplication\n@MapperScan("${packageName}")`)
    await writeFile(path, source)
  }
}

async function keepOnlyContextLoadTest(projectDir) {
  const testsRoot = join(projectDir, 'src', 'test', 'java')
  const files = await listFiles(testsRoot)
  for (const file of files) {
    const base = file.split(/[/\\]/).pop()
    if (base === 'TestcontainersConfiguration.java' || /^Test.+Application\.java$/.test(base)) {
      await rm(file)
      continue
    }
    if (!base.endsWith('ApplicationTests.java')) continue
    let source = await readFile(file, 'utf8')
    source = source.replace(/\nimport org\.springframework\.context\.annotation\.Import;\n/, '\n')
    source = source.replace(/@Import\(TestcontainersConfiguration\.class\)\n/, '')
    await writeFile(file, source)
  }
}

async function stripTestcontainers(projectDir) {
  const pomPath = join(projectDir, 'pom.xml')
  let pom = await readFile(pomPath, 'utf8')
  for (const artifact of ['spring-boot-testcontainers', 'testcontainers-junit-jupiter', 'testcontainers-postgresql', 'testcontainers']) {
    pom = stripDependency(pom, artifact)
  }
  await writeFile(pomPath, pom)
}

async function writeTestApplicationYaml(projectDir, { database, persistence }) {
  if (database !== 'postgresql') return
  const excludes = [
    'org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration',
    'org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration',
    'org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration',
  ]
  if (persistence === 'mybatis-plus') {
    excludes.push('com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration')
  }
  const yaml = `spring:\n  autoconfigure:\n    exclude:\n${excludes.map((name) => `      - ${name}`).join('\n')}\n`
  const resources = join(projectDir, 'src', 'test', 'resources')
  await mkdir(resources, { recursive: true })
  await writeFile(join(resources, 'application.yaml'), yaml)
}

async function main() {
  const { help, options } = parseArgs(process.argv.slice(2))
  if (help) return usage()
  assertOptions(options)

  const dependencies = ['web', 'validation', 'actuator']
  if (options.database === 'postgresql') dependencies.push('postgresql', 'flyway', 'docker-compose')
  const query = new URLSearchParams({
    type: 'maven-project',
    language: 'java',
    bootVersion: options.initializrBootVersion,
    baseDir: options.name,
    groupId: options.groupid,
    artifactId: options.name,
    name: options.name,
    packageName: options.packagename,
    packaging: 'jar',
    javaVersion: options.java,
    configurationFileFormat: 'yaml',
    dependencies: [...new Set(dependencies)].join(','),
  })

  const tempDir = await mkdtemp(join(tmpdir(), 'spring-boot-4-'))
  const zipPath = join(tempDir, 'project.zip')
  const extracted = join(tempDir, options.name)
  const url = `https://start.spring.io/starter.zip?${query}`

  try {
    console.log(`Requesting Spring Boot ${options.initializrBootVersion} (maven, Java ${options.java}) from start.spring.io`)
    const response = await fetch(url, { signal: AbortSignal.timeout(timeoutMs) })
    if (!response.ok) {
      const detail = (await response.text()).slice(0, 1000)
      throw new Error(`Spring Initializr returned HTTP ${response.status}: ${detail}`)
    }
    await writeFile(zipPath, new Uint8Array(await response.arrayBuffer()))
    await extract(zipPath, tempDir)
    await rm(join(extracted, 'HELP.md'), { force: true })
    if (!existsSync(extracted)) throw new Error('Spring Initializr archive did not contain the requested project directory')
    await mkdir(options.outputdir, { recursive: true })
    await rename(extracted, options.projectDir)

    await pinMavenBootVersion(options.projectDir, options.mavenBootVersion)
    const pom = await readFile(join(options.projectDir, 'pom.xml'), 'utf8')
    assertJackson3(pom)
    await removeJdbcStarter(options.projectDir)
    await addSpringdoc(options.projectDir)
    if (options.persistence === 'mybatis-plus') {
      await addMyBatisPlus(options.projectDir)
      await addMapperScan(options.projectDir, options.packagename)
    }
    await ensureFlyway(options.projectDir, options.database)
    await stripTestcontainers(options.projectDir)
    await keepOnlyContextLoadTest(options.projectDir)
    await writeTestApplicationYaml(options.projectDir, options)
    await applyCompose(options.projectDir, options.database)
    await applyMavenHouseStyle(options.projectDir, options, versions)
    const wrapper = join(options.projectDir, 'mvnw')
    if (process.platform !== 'win32' && existsSync(wrapper)) await chmod(wrapper, 0o755)
    run(wrapper, ['-q', '-B', 'spotless:apply'], { cwd: options.projectDir })
    console.log(`\nProject created: ${options.projectDir}`)
    console.log(`Next: cd ${options.projectDir}`)
    console.log('Then: ./mvnw spring-boot:run')
  } finally {
    await rm(tempDir, { recursive: true, force: true })
  }
}

main().catch((error) => {
  console.error(`Project generation failed: ${error.message}`)
  process.exitCode = 1
})
