#!/usr/bin/env node

import { execFileSync } from 'node:child_process'
import { mkdtemp, mkdir, readFile, rename, rm, writeFile } from 'node:fs/promises'
import { existsSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { basename, dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const versions = JSON.parse(await readFile(join(root, 'versions.json'), 'utf8'))
const timeoutMs = 60_000

function usage() {
  console.log(`Create a Spring Boot project from Spring Initializr.

Usage:
  node scripts/create-project.mjs --name <name> [options]

Options:
  --name <name>                 Project directory and artifact name (required)
  --group-id <id>               Java group id (default: com.example)
  --package-name <name>         Java package (default: <group-id>.<project-name>)
  --build <maven|gradle>         Build tool (default: maven)
  --java <version>              Java version (default: ${versions.java.default})
  --boot-version <version>      Exact Spring Boot version (default: ${versions.springBoot.default})
  --persistence <jpa|mybatis-plus|none> Persistence layer (default: jpa)
  --database <postgresql|none>   Database driver and local Compose setup (default: postgresql)
  --docker                      Add Dockerfile and full-stack Compose overlay
  --output-dir <path>           Parent directory (default: current directory)
  --help                        Show this help

Examples:
  node scripts/create-project.mjs --name inventory --docker
  node scripts/create-project.mjs --name api --persistence mybatis-plus --database postgresql
  node scripts/create-project.mjs --name api --build gradle --java 21 --output-dir ./apps`)
}

function parseArgs(args) {
  const options = { build: 'maven', persistence: 'jpa', database: 'postgresql', outputDir: process.cwd() }
  const values = new Set(['--name', '--group-id', '--package-name', '--build', '--java', '--boot-version', '--persistence', '--database', '--output-dir'])
  for (let i = 0; i < args.length; i += 1) {
    const key = args[i]
    if (key === '--help' || key === '-h') return { help: true, options }
    if (key === '--docker') {
      options.docker = true
      continue
    }
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
  if (!['maven', 'gradle'].includes(options.build)) throw new Error('--build must be maven or gradle')
  if (!['jpa', 'mybatis-plus', 'none'].includes(options.persistence)) throw new Error('--persistence must be jpa, mybatis-plus, or none')
  if (!['none', 'postgresql'].includes(options.database)) throw new Error('--database must be none or postgresql')
  if (options.docker && options.database !== 'postgresql') {
    throw new Error('--docker currently adds the PostgreSQL-backed full-stack Compose overlay; also pass --database postgresql')
  }
  if (options.docker && options.build !== 'maven') {
    throw new Error('--docker currently uses the Maven Dockerfile; pass --build maven or omit --docker')
  }
  const groupId = options.groupid ?? 'com.example'
  if (!/^[A-Za-z_][\w]*(\.[A-Za-z_][\w]*)*$/.test(groupId)) throw new Error('--group-id must be a valid Java group id')
  options.groupid = groupId
  options.artifactId = options.name
  const defaultPackageSegment = options.name.replaceAll(/[^A-Za-z0-9_]/g, '').toLowerCase() || 'app'
  options.packagename ??= `${groupId}.${/^\d/.test(defaultPackageSegment) ? `app${defaultPackageSegment}` : defaultPackageSegment}`
  if (!/^[A-Za-z_][\w]*(\.[A-Za-z_][\w]*)*$/.test(options.packagename)) throw new Error('--package-name must be a valid Java package')
  options.java ??= versions.java.default
  options.bootversion ??= versions.springBoot.default
  if (!/^\d+(\.\d+){0,2}$/.test(options.java)) throw new Error('--java must be a numeric Java version')
  if (!/^\d+\.\d+\.\d+$/.test(options.bootversion)) throw new Error('--boot-version must be an exact stable version, such as 4.1.1')
  const [bootMajor] = options.bootversion.split('.')
  const maxJava = versions.java.maximumBySpringBootMajor[bootMajor]
  if (!maxJava) throw new Error(`No compatibility baseline is recorded for Spring Boot ${bootMajor}.x`)
  if (Number(options.java) < Number(versions.java.minimum) || Number(options.java) > Number(maxJava)) {
    throw new Error(`Spring Boot ${bootMajor}.x compatibility baseline requires Java ${versions.java.minimum}-${maxJava}`)
  }
  options.outputdir = resolve(options.outputdir)
  options.projectDir = join(options.outputdir, options.name)
  if (existsSync(options.projectDir)) throw new Error(`Target already exists: ${options.projectDir}`)
}

function run(command, args) {
  execFileSync(command, args, { stdio: 'inherit' })
}

async function extract(zipPath, destination) {
  if (process.platform === 'win32') {
    const escapedZip = zipPath.replaceAll("'", "''")
    const escapedDest = destination.replaceAll("'", "''")
    run('powershell', ['-NoProfile', '-NonInteractive', '-Command', `Expand-Archive -LiteralPath '${escapedZip}' -DestinationPath '${escapedDest}'`])
  } else {
    run('unzip', ['-q', zipPath, '-d', destination])
  }
}

async function addDockerAssets(projectDir, name, javaVersion, persistence) {
  const assets = join(root, 'assets')
  const dockerfile = (await readFile(join(assets, 'Dockerfile'), 'utf8'))
    .replace('eclipse-temurin:25-jdk-noble', `eclipse-temurin:${javaVersion}-jdk-noble`)
  await writeFile(join(projectDir, 'Dockerfile'), dockerfile)
  let compose = (await readFile(join(assets, 'docker-compose.yml'), 'utf8')).replace('  spring-app:', `  ${name}:`)
  if (persistence !== 'jpa') compose = compose.replace(/^\s+SPRING_JPA_HIBERNATE_DDL_AUTO:.*\r?\n/m, '')
  await writeFile(join(projectDir, 'compose.full.yaml'), compose)
  const ignorePath = join(projectDir, '.dockerignore')
  const current = existsSync(ignorePath) ? await readFile(ignorePath, 'utf8') : ''
  const additions = ['.git', '.env', '.env.*', '!*.env.sample', 'target', '**/target', 'frontend/node_modules', '**/node_modules', 'frontend/dist']
  const merged = [...new Set([...current.split(/\r?\n/).filter(Boolean), ...additions])]
  await writeFile(ignorePath, `${merged.join('\n')}\n`)
}

async function addMyBatisPlusDependency(projectDir, build, bootVersion) {
  const artifact = Number(bootVersion.split('.')[0]) >= 4
    ? 'mybatis-plus-spring-boot4-starter'
    : 'mybatis-plus-spring-boot3-starter'
  if (build === 'maven') {
    const pomPath = join(projectDir, 'pom.xml')
    let pom = await readFile(pomPath, 'utf8')
    if (pom.includes('com.baomidou')) return
    const dependency = `\n\t\t<dependency>\n\t\t\t<groupId>com.baomidou</groupId>\n\t\t\t<artifactId>${artifact}</artifactId>\n\t\t\t<version>${versions.mybatisPlus.default}</version>\n\t\t</dependency>`
    const closing = pom.lastIndexOf('</dependencies>')
    if (closing === -1) throw new Error('Cannot configure MyBatis-Plus: generated pom.xml has no <dependencies> block')
    pom = `${pom.slice(0, closing)}${dependency}\n\t${pom.slice(closing)}`
    await writeFile(pomPath, pom)
    return
  }

  const buildPath = ['build.gradle.kts', 'build.gradle'].map((file) => join(projectDir, file)).find((path) => existsSync(path))
  if (!buildPath) throw new Error('Cannot configure MyBatis-Plus: generated Gradle build file was not found')
  let buildFile = await readFile(buildPath, 'utf8')
  if (buildFile.includes('com.baomidou')) return
  const declaration = buildPath.endsWith('.kts')
    ? `    implementation("com.baomidou:${artifact}:${versions.mybatisPlus.default}")\n`
    : `    implementation 'com.baomidou:${artifact}:${versions.mybatisPlus.default}'\n`
  if (!/dependencies\s*\{/.test(buildFile)) throw new Error('Cannot configure MyBatis-Plus: generated Gradle file has no dependencies block')
  buildFile = buildFile.replace(/(dependencies\s*\{\s*\r?\n)/, `$1${declaration}`)
  await writeFile(buildPath, buildFile)
}

async function addDatabaseAssets(projectDir) {
  const composePath = join(projectDir, 'compose.yaml')
  if (!existsSync(composePath)) {
    await writeFile(composePath, await readFile(join(root, 'assets', 'compose.yaml')))
  }
}

async function addProjectSetupAssets(projectDir, packageName, bootVersion) {
  const assets = join(root, 'assets')
  const gitignorePath = join(projectDir, '.gitignore')
  const existingGitignore = existsSync(gitignorePath) ? await readFile(gitignorePath, 'utf8') : ''
  const generatedGitignore = await readFile(join(assets, 'gitignore'), 'utf8')
  const mergedGitignore = [...new Set([...existingGitignore.split(/\r?\n/).filter(Boolean), '# === spring-boot-developer additions ===', ...generatedGitignore.split(/\r?\n/).filter(Boolean)])]
  await writeFile(gitignorePath, `${mergedGitignore.join('\n')}\n`)

  for (const [sourceName, targetName] of [['editorconfig', '.editorconfig'], ['gitattributes', '.gitattributes'], ['env.sample', '.env.sample']]) {
    const target = join(projectDir, targetName)
    if (!existsSync(target)) await writeFile(target, await readFile(join(assets, sourceName)))
  }

  const dockerignorePath = join(projectDir, '.dockerignore')
  const currentDockerignore = existsSync(dockerignorePath) ? await readFile(dockerignorePath, 'utf8') : ''
  const generatedDockerignore = await readFile(join(assets, 'dockerignore'), 'utf8')
  const mergedDockerignore = [...new Set([...currentDockerignore.split(/\r?\n/).filter(Boolean), ...generatedDockerignore.split(/\r?\n/).filter(Boolean)])]
  await writeFile(dockerignorePath, `${mergedDockerignore.join('\n')}\n`)

  await mkdir(join(projectDir, '.github'), { recursive: true })
  await writeFile(join(projectDir, '.github', 'lsp.json'), await readFile(join(assets, 'lsp.json')))

  if (bootVersion.startsWith('4.')) {
    const packagePath = packageName.split('.').join('/')
    const configDirectory = join(projectDir, 'src', 'main', 'java', packagePath, 'config')
    await mkdir(configDirectory, { recursive: true })
    const listener = (await readFile(join(assets, 'StartupInfoListener.java.tmpl'), 'utf8')).replaceAll('__PACKAGE__', packageName)
    await writeFile(join(configDirectory, 'StartupInfoListener.java'), listener)
  }
}

async function addDeveloperToolingAssets(projectDir, javaVersion, build, database, docker) {
  await writeFile(join(projectDir, '.sdkmanrc'), `java=${javaVersion}-tem\n`)

  const wrapperName = build === 'maven' ? 'mvnw' : 'gradlew'
  const wrapper = `{{if eq OS "windows"}}${wrapperName === 'mvnw' ? 'mvnw.cmd' : 'gradlew.bat'}{{else}}./${wrapperName}{{end}}`
  const tasks = [
    "version: '3'",
    '',
    'vars:',
    `  WRAPPER: '${wrapper}'`,
    '',
    'tasks:',
    '  default:',
    '    desc: List available tasks',
    '    cmds:',
    '      - task --list',
    '',
    '  test:',
    '    desc: Run the test suite',
    '    cmds:',
    `      - '{{.WRAPPER}} test'`,
    '',
    '  verify:',
    '    desc: Run verification and package checks',
    '    cmds:',
    `      - '{{.WRAPPER}} ${build === 'maven' ? 'verify' : 'check'}'`,
    '',
    '  format:',
    '    desc: Apply Java formatting',
    '    cmds:',
    `      - '{{.WRAPPER}} ${build === 'maven' ? 'spotless:apply' : 'spotlessApply'}'`,
    '',
    '  run:',
    '    desc: Run the application',
    '    cmds:',
    `      - '{{.WRAPPER}} ${build === 'maven' ? 'spring-boot:run' : 'bootRun'}'`,
  ]

  if (database === 'postgresql') {
    tasks.push(
      '',
      '  compose:up:',
      '    desc: Start local PostgreSQL',
      '    cmds:',
      '      - docker compose up -d postgres',
      '',
      '  compose:stop:',
      '    desc: Stop local services and keep their data',
      '    cmds:',
      '      - docker compose stop',
    )
  }

  if (docker) {
    tasks.push(
      '',
      '  docker:up:',
      '    desc: Build and start the Docker Compose application',
      '    cmds:',
      '      - docker compose -f compose.full.yaml up -d --build',
      '',
      '  docker:down:',
      '    desc: Stop the Docker Compose application and keep its data',
      '    cmds:',
      '      - docker compose -f compose.full.yaml down',
    )
  }

  await writeFile(join(projectDir, 'Taskfile.yml'), `${tasks.join('\n')}\n`)
}

async function renameContainerTest(projectDir) {
  const testsRoot = join(projectDir, 'src', 'test', 'java')
  if (!existsSync(testsRoot)) return
  const entries = await readFileList(testsRoot)
  for (const file of entries.filter((entry) => entry.endsWith('ApplicationTests.java'))) {
    const source = await readFile(file, 'utf8')
    if (!source.includes('TestcontainersConfiguration')) continue
    const oldName = basename(file, '.java')
    const newName = `${oldName.slice(0, -'Tests'.length)}IT`
    await writeFile(join(dirname(file), `${newName}.java`), source.replaceAll(oldName, newName))
    await rm(file)
  }
}

async function normalizeTestcontainersConfiguration(projectDir, bootVersion) {
  const testsRoot = join(projectDir, 'src', 'test', 'java')
  if (!existsSync(testsRoot)) return
  const entries = await readFileList(testsRoot)
  for (const file of entries.filter((entry) => basename(entry) === 'TestcontainersConfiguration.java')) {
    let source = await readFile(file, 'utf8')
    if (bootVersion.startsWith('4.')) {
      // Testcontainers 2 made PostgreSQLContainer non-generic. Spring Initializr
      // templates can still emit the old diamond/generic form, which fails compile.
      source = source.replace(/PostgreSQLContainer\s*<\s*[^>]*\s*>/g, 'PostgreSQLContainer')
    }
    source = source.replace(/^(\s*)(?<!public )(class\s+TestcontainersConfiguration\b)/m, '$1public $2')
    source = source.replace(/^(\s*)(?!public\s)(PostgreSQLContainer(?:<[^>]+>)?\s+postgresContainer\s*\()/m, '$1public $2')
    await writeFile(file, source)
  }
}

async function ensureMavenFailsafe(projectDir) {
  const pomPath = join(projectDir, 'pom.xml')
  if (!existsSync(pomPath)) return
  let pom = await readFile(pomPath, 'utf8')
  if (pom.includes('maven-failsafe-plugin')) return
  const closingPlugins = pom.indexOf('</plugins>')
  if (closingPlugins === -1) throw new Error('Cannot configure Failsafe: generated pom.xml has no <plugins> block')
  const plugin = `\t\t\t<plugin>\n\t\t\t\t<groupId>org.apache.maven.plugins</groupId>\n\t\t\t\t<artifactId>maven-failsafe-plugin</artifactId>\n\t\t\t\t<executions>\n\t\t\t\t\t<execution>\n\t\t\t\t\t\t<goals>\n\t\t\t\t\t\t\t<goal>integration-test</goal>\n\t\t\t\t\t\t\t<goal>verify</goal>\n\t\t\t\t\t\t</goals>\n\t\t\t\t\t</execution>\n\t\t\t\t</executions>\n\t\t\t</plugin>\n\t\t`
  pom = `${pom.slice(0, closingPlugins)}${plugin}${pom.slice(closingPlugins)}`
  await writeFile(pomPath, pom)
}

async function ensureMavenSpotless(projectDir) {
  const pomPath = join(projectDir, 'pom.xml')
  if (!existsSync(pomPath)) return
  let pom = await readFile(pomPath, 'utf8')
  if (pom.includes('spotless-maven-plugin')) return
  const closingPlugins = pom.indexOf('</plugins>')
  if (closingPlugins === -1) throw new Error('Cannot configure Spotless: generated pom.xml has no <plugins> block')
  const plugin = `\t\t\t<plugin>\n\t\t\t\t<groupId>com.diffplug.spotless</groupId>\n\t\t\t\t<artifactId>spotless-maven-plugin</artifactId>\n\t\t\t\t<version>${versions.spotless.mavenPlugin}</version>\n\t\t\t\t<configuration>\n\t\t\t\t\t<java>\n\t\t\t\t\t\t<importOrder/>\n\t\t\t\t\t\t<removeUnusedImports/>\n\t\t\t\t\t\t<formatAnnotations/>\n\t\t\t\t\t\t<palantirJavaFormat>\n\t\t\t\t\t\t\t<version>${versions.spotless.palantirJavaFormat}</version>\n\t\t\t\t\t\t</palantirJavaFormat>\n\t\t\t\t\t</java>\n\t\t\t\t</configuration>\n\t\t\t\t<executions>\n\t\t\t\t\t<execution>\n\t\t\t\t\t\t<goals><goal>check</goal></goals>\n\t\t\t\t\t</execution>\n\t\t\t\t</executions>\n\t\t\t</plugin>\n\t\t`
  pom = `${pom.slice(0, closingPlugins)}${plugin}${pom.slice(closingPlugins)}`
  await writeFile(pomPath, pom)
}

async function ensureGradleSpotless(projectDir) {
  const candidates = ['build.gradle.kts', 'build.gradle']
  const buildPath = candidates.map((name) => join(projectDir, name)).find((path) => existsSync(path))
  if (!buildPath) throw new Error('Cannot configure Spotless: generated Gradle build file was not found')
  let buildFile = await readFile(buildPath, 'utf8')
  if (buildFile.includes('com.diffplug.spotless')) return

  const pluginsStart = buildFile.indexOf('plugins {')
  const pluginsEnd = pluginsStart === -1 ? -1 : buildFile.indexOf('\n}', pluginsStart)
  if (pluginsEnd === -1) throw new Error('Cannot configure Spotless: generated Gradle file has no plugins block')

  const pluginDeclaration = buildPath.endsWith('.kts')
    ? `    id("com.diffplug.spotless") version "${versions.spotless.gradlePlugin}"\n`
    : `    id 'com.diffplug.spotless' version '${versions.spotless.gradlePlugin}'\n`
  buildFile = `${buildFile.slice(0, pluginsEnd)}\n${pluginDeclaration}${buildFile.slice(pluginsEnd)}`

  const configuration = buildPath.endsWith('.kts')
    ? `\n\nspotless {\n    java {\n        importOrder()\n        removeUnusedImports()\n        formatAnnotations()\n        palantirJavaFormat("${versions.spotless.palantirJavaFormat}")\n    }\n}\n`
    : `\n\nspotless {\n    java {\n        importOrder()\n        removeUnusedImports()\n        formatAnnotations()\n        palantirJavaFormat('${versions.spotless.palantirJavaFormat}')\n    }\n}\n`
  buildFile = `${buildFile.trimEnd()}${configuration}`
  await writeFile(buildPath, buildFile)
}

async function ensureGradlePluginRepositories(projectDir) {
  const candidates = ['settings.gradle.kts', 'settings.gradle']
  const settingsPath = candidates.map((name) => join(projectDir, name)).find((path) => existsSync(path))
  if (!settingsPath) throw new Error('Cannot configure Gradle: generated project has no settings file')
  let settings = await readFile(settingsPath, 'utf8')
  if (!settings.includes('pluginManagement')) {
    const block = `pluginManagement {\n    repositories {\n        mavenCentral()\n        gradlePluginPortal()\n    }\n}\n\n`
    settings = `${block}${settings}`
    await writeFile(settingsPath, settings)
  }
}

async function readFileList(directory) {
  const { readdir } = await import('node:fs/promises')
  const result = []
  for (const item of await readdir(directory, { withFileTypes: true })) {
    const path = join(directory, item.name)
    if (item.isDirectory()) result.push(...await readFileList(path))
    else result.push(path)
  }
  return result
}

async function main() {
  const { help, options } = parseArgs(process.argv.slice(2))
  if (help) return usage()
  assertOptions(options)

  const dependencies = ['web', 'validation', 'actuator']
  if (options.persistence === 'jpa') dependencies.push('data-jpa')
  if (options.persistence === 'mybatis-plus') dependencies.push('jdbc')
  if (options.database === 'postgresql') dependencies.push('postgresql', 'docker-compose', 'testcontainers')
  const projectType = options.build === 'maven' ? 'maven-project' : 'gradle-project-kotlin'
  const query = new URLSearchParams({
    type: projectType,
    language: 'java',
    bootVersion: options.bootversion,
    baseDir: options.name,
    groupId: options.groupid,
    artifactId: options.artifactId,
    name: options.name,
    packageName: options.packagename,
    packaging: 'jar',
    javaVersion: options.java,
    dependencies: [...new Set(dependencies)].join(','),
  })

  const tempDir = await mkdtemp(join(tmpdir(), 'spring-boot-developer-'))
  const zipPath = join(tempDir, 'project.zip')
  const extracted = join(tempDir, options.name)
  const url = `https://start.spring.io/starter.zip?${query}`

  try {
    console.log(`Requesting Spring Boot ${options.bootversion} (${options.build}, Java ${options.java}) from start.spring.io…`)
    const response = await fetch(url, { signal: AbortSignal.timeout(timeoutMs) })
    if (!response.ok) {
      const detail = (await response.text()).slice(0, 1000)
      throw new Error(`Spring Initializr returned HTTP ${response.status}: ${detail}`)
    }
    await writeFile(zipPath, new Uint8Array(await response.arrayBuffer()))
    await extract(zipPath, tempDir)
    if (!existsSync(extracted)) throw new Error('Spring Initializr archive did not contain the requested project directory')
    await mkdir(options.outputdir, { recursive: true })
    await rename(extracted, options.projectDir)

    if (options.persistence === 'mybatis-plus') {
      await addMyBatisPlusDependency(options.projectDir, options.build, options.bootversion)
    }

    await addProjectSetupAssets(options.projectDir, options.packagename, options.bootversion)
    await addDeveloperToolingAssets(options.projectDir, options.java, options.build, options.database, options.docker)

    if (options.database === 'postgresql') await addDatabaseAssets(options.projectDir)

    if (options.build === 'maven') await ensureMavenSpotless(options.projectDir)
    else await ensureGradleSpotless(options.projectDir)

    if (options.database === 'postgresql' && options.build === 'maven') {
      await normalizeTestcontainersConfiguration(options.projectDir, options.bootversion)
      await ensureMavenFailsafe(options.projectDir)
      await renameContainerTest(options.projectDir)
    }
    if (options.build === 'gradle') await ensureGradlePluginRepositories(options.projectDir)
    if (options.docker) await addDockerAssets(options.projectDir, options.name, options.java, options.persistence)
    console.log(`\nProject created: ${options.projectDir}`)
    console.log(`Next: cd ${options.projectDir}`)
    console.log(options.build === 'maven' ? 'Then: ./mvnw spring-boot:run' : 'Then: ./gradlew bootRun')
  } finally {
    await rm(tempDir, { recursive: true, force: true })
  }
}

main().catch((error) => {
  console.error(`Project generation failed: ${error.message}`)
  process.exitCode = 1
})
