#!/usr/bin/env node

import { existsSync } from 'node:fs'
import { readFile, readdir } from 'node:fs/promises'
import { dirname, join, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { inspectCompose } from './compose.mjs'
import { dependenciesBlock } from './pom.mjs'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const versions = JSON.parse(await readFile(join(root, 'versions.json'), 'utf8'))

function usage() {
  console.log(`Check a Spring Boot 4 project for house-style contracts.

Usage:
  node scripts/doctor-project.mjs [project-dir] [--strict]

Options:
  --strict   Treat warnings as failures
  --help     Show this help`)
}

const args = process.argv.slice(2)
if (args.includes('--help') || args.includes('-h')) {
  usage()
  process.exit(0)
}

const strict = args.includes('--strict')
const paths = args.filter((arg) => arg !== '--strict')
if (paths.length > 1) {
  console.error('Only one project directory may be supplied')
  process.exit(1)
}

const projectDir = resolve(paths[0] ?? process.cwd())
const findings = []

function finding(level, code, message) {
  findings.push({ level, code, message })
}

function has(path) {
  return existsSync(join(projectDir, path))
}

async function text(path) {
  try {
    return await readFile(join(projectDir, path), 'utf8')
  } catch {
    return ''
  }
}

async function files(directory, result = []) {
  if (!existsSync(directory)) return result
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) {
      if (!['.git', 'target', 'build', 'node_modules'].includes(entry.name)) await files(path, result)
    } else {
      result.push(path)
    }
  }
  return result
}

function declaredArtifact(pom, artifactId) {
  return new RegExp(`<artifactId>\\s*${artifactId}\\s*</artifactId>`).test(pom)
}

async function main() {
  if (!existsSync(projectDir)) {
    finding('error', 'project.missing', `Project directory does not exist: ${projectDir}`)
    return
  }

  if (has('build.gradle') || has('build.gradle.kts')) {
    finding('error', 'build.gradle', 'Gradle is not in the v1 house style; use Maven')
  }
  if (!has('pom.xml')) {
    finding('error', 'build.missing', 'No pom.xml was found')
    return
  }

  const pom = await text('pom.xml')
  if (!has('mvnw') && !has('mvnw.cmd')) finding('error', 'wrapper.missing', 'Maven wrapper is not committed')

  const parent = pom.match(/<parent>[\s\S]*?<artifactId>\s*spring-boot-starter-parent\s*<\/artifactId>[\s\S]*?<version>\s*([^<]+)\s*<\/version>/)
  const bootVersion = parent?.[1]?.trim() ?? ''
  if (!bootVersion) finding('error', 'boot.parent', 'pom.xml has no spring-boot-starter-parent version')
  else if (!/^4\./.test(bootVersion)) finding('error', 'boot.major', `Spring Boot ${bootVersion} is not 4.x`)

  const javaVersion = pom.match(/<java\.version>\s*([^<]+)\s*<\/java\.version>/)?.[1]?.trim()
  if (javaVersion && Number(javaVersion) < 17) finding('error', 'java.minimum', `Java ${javaVersion} is below 17`)

  if (declaredArtifact(pom, 'spring-boot-starter-data-jpa')) finding('error', 'jpa.forbidden', 'spring-boot-starter-data-jpa is not allowed')
  if (declaredArtifact(pom, 'spring-boot-starter-jdbc')) finding('error', 'jdbc.forbidden', 'spring-boot-starter-jdbc is not allowed; use MyBatis-Plus')
  if (declaredArtifact(pom, 'spring-boot-jackson2') || declaredArtifact(pom, 'spring-boot-starter-jackson2')) {
    finding('error', 'jackson2.forbidden', 'Jackson 2 starters are not allowed')
  }
  if (declaredArtifact(pom, 'gson') || declaredArtifact(pom, 'yasson')) finding('error', 'json.alt.forbidden', 'Gson and JSON-B are not allowed')
  if (!declaredArtifact(pom, 'spring-boot-starter-web') && !declaredArtifact(pom, 'spring-boot-starter-webmvc') && !declaredArtifact(pom, 'spring-boot-starter-jackson')) {
    finding('error', 'jackson3.missing', 'Jackson 3 is missing; expected spring-boot-starter-webmvc or spring-boot-starter-jackson')
  }
  if (!declaredArtifact(pom, 'springdoc-openapi-starter-webmvc-ui')) {
    finding('error', 'springdoc.missing', 'springdoc-openapi-starter-webmvc-ui is missing')
  }
  if (!pom.includes('<dependencyManagement>')) {
    finding('error', 'maven.dependencyManagement', 'pom.xml has no <dependencyManagement> block')
  }
  if (!pom.includes('<springdoc.version>')) {
    finding('error', 'springdoc.version.property', 'springdoc.version is missing from <properties>')
  }
  if (declaredArtifact(pom, 'mybatis-plus-spring-boot4-starter') && !pom.includes('<mybatis-plus.version>')) {
    finding('error', 'mybatis.version.property', 'mybatis-plus.version is missing from <properties>')
  }
  const deps = dependenciesBlock(pom)
  if (/<artifactId>\s*springdoc-openapi-starter-webmvc-ui\s*<\/artifactId>\s*<version>/.test(deps)) {
    finding('error', 'maven.inline.version', 'springdoc version belongs in dependencyManagement, not <dependencies>')
  }
  if (/<artifactId>\s*mybatis-plus-spring-boot4-starter\s*<\/artifactId>\s*<version>/.test(deps)) {
    finding('error', 'maven.inline.version', 'mybatis-plus version belongs in dependencyManagement, not <dependencies>')
  }
  if (!declaredArtifact(pom, 'maven-surefire-plugin') || !pom.includes('<failIfNoTests>true</failIfNoTests>')) {
    finding('error', 'surefire.missing', 'maven-surefire-plugin with failIfNoTests is missing')
  }
  if (!declaredArtifact(pom, 'spotless-maven-plugin')) {
    finding('error', 'spotless.missing', 'spotless-maven-plugin is missing')
  }

  const hasMp = declaredArtifact(pom, 'mybatis-plus-spring-boot4-starter')
  const hasPostgres = declaredArtifact(pom, 'postgresql')
  if (hasMp) {
    const sources = await files(join(projectDir, 'src', 'main', 'java'))
    const application = (await Promise.all(sources.filter((file) => file.endsWith('Application.java')).map((file) => readFile(file, 'utf8')))).join('\n')
    if (!application.includes('@MapperScan')) finding('error', 'mapperscan.missing', '@MapperScan is missing on the Application class')
  }
  if (hasMp || hasPostgres) {
    if (!declaredArtifact(pom, 'spring-boot-starter-flyway')) finding('error', 'flyway.missing', 'spring-boot-starter-flyway is missing')
  }
  if (hasPostgres && !declaredArtifact(pom, 'flyway-database-postgresql')) {
    finding('error', 'flyway.postgres.missing', 'flyway-database-postgresql is missing')
  }
  if (hasPostgres) {
    const composeText = has('compose.yaml') ? await text('compose.yaml') : null
    for (const { code, message } of inspectCompose(composeText, { hasPostgres, image: versions.postgresql.image })) {
      finding('error', code, message)
    }
  }

  const mainJava = await files(join(projectDir, 'src', 'main', 'java'))
  for (const file of mainJava.filter((path) => path.endsWith('.java'))) {
    const source = await readFile(file, 'utf8')
    const rel = relative(projectDir, file)
    if (/@Bean[\s\S]{0,240}ObjectMapper\b/.test(source) || /\bObjectMapper\b[\s\S]{0,120}@Bean/.test(source)) {
      finding('error', 'jackson.objectmapper.bean', `${rel} declares an ObjectMapper bean; inject JsonMapper instead`)
    }
    if (source.includes('com.fasterxml.jackson.databind.ObjectMapper')) {
      finding('error', 'jackson2.databind', `${rel} imports Jackson 2 ObjectMapper; use tools.jackson JsonMapper`)
    }
  }

  if (findings.length === 0) console.log(`PASS project contract: ${projectDir}`)
  for (const { level, code, message } of findings) console.log(`${level.toUpperCase()} ${code}: ${message}`)
}

await main()

const errors = findings.filter(({ level }) => level === 'error').length
const warnings = findings.filter(({ level }) => level === 'warning').length
if (errors > 0 || (strict && warnings > 0)) {
  console.log(`${errors + (strict ? warnings : 0)} contract check(s) failed`)
  process.exitCode = 1
}
