#!/usr/bin/env node

import { existsSync } from 'node:fs'
import { readFile, readdir } from 'node:fs/promises'
import { basename, join, relative, resolve } from 'node:path'

function usage() {
  console.log(`Check a Spring Boot project for closed build and configuration contracts.

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

function hasYamlProperty(source, key, value) {
  const expected = value === undefined ? '[^\\r\\n]+' : escapeRegExp(value)
  const leaf = escapeRegExp(key.split('.').at(-1))
  const flat = escapeRegExp(key)
  return new RegExp(`(?:^|\\n)\\s*${leaf}\\s*:\\s*${expected}(?:\\s|$)`, 'm').test(source)
    || new RegExp(`(?:^|\\n)\\s*${flat}\\s*=\\s*${expected}\\s*$`, 'm').test(source)
}

function parseEnvKeys(source) {
  return [...source.matchAll(/^([A-Z][A-Z0-9_]*)=(.*)$/gm)].map((match) => ({ key: match[1], value: match[2] }))
}

function parseProfiles(envEntries) {
  const value = envEntries.find(({ key }) => key === 'SPRING_PROFILES_ACTIVE')?.value ?? ''
  return value.split(',').map((profile) => profile.trim()).filter(Boolean)
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

async function main() {
  if (!existsSync(projectDir)) {
    finding('error', 'project.missing', `Project directory does not exist: ${projectDir}`)
    return
  }

  const buildCandidates = ['pom.xml', 'build.gradle', 'build.gradle.kts'].filter(has)
  if (buildCandidates.length === 0) {
    finding('error', 'build.missing', 'No pom.xml or Gradle build file was found')
    return
  }
  if (buildCandidates.length > 1) finding('warning', 'build.multiple', `Multiple build descriptors found: ${buildCandidates.join(', ')}`)

  const buildPath = buildCandidates[0]
  const buildText = await text(buildPath)
  const isMaven = buildPath === 'pom.xml'
  const hasJpa = /spring-boot-starter-data-jpa|spring-data-jpa/.test(buildText)

  if (isMaven && /<licenses>[\s\S]*<license>[\s\S]*<name>[^<]+<\/name>/.test(buildText) && !has('LICENSE')) {
    finding('warning', 'license.file.missing', 'Build metadata declares a license but the project has no root LICENSE file')
  }

  if (isMaven && !has('mvnw') && !has('mvnw.cmd')) finding('warning', 'wrapper.missing', 'Maven wrapper is not committed')
  if (!isMaven && !has('gradlew') && !has('gradlew.bat')) finding('warning', 'wrapper.missing', 'Gradle wrapper is not committed')

  const sourceFiles = await files(projectDir)
  const configFiles = sourceFiles.filter((path) => /^application(?:-[^/]+)?\.(yaml|yml|properties)$/.test(basename(path)))
  const configText = (await Promise.all(configFiles.map((path) => readFile(path, 'utf8')))).join('\n')
  const envSample = await text('.env.sample')
  const envEntries = parseEnvKeys(envSample)

  if (envSample.includes('generated application imports this file') && !configText.includes('optional:file:.env[.properties]')) {
    finding('warning', 'env.import.missing', '.env.sample says Spring imports .env, but application configuration does not import it')
  }

  if (envEntries.length > 0) {
    const operationalFiles = sourceFiles.filter((path) => {
      const name = basename(path)
      const rel = relative(projectDir, path)
      if (name === '.env' || name === '.env.sample' || rel.startsWith('.git/')) return false
      return rel.startsWith('src/')
        || /(?:^|\/)(?:compose|docker-compose)(?:\.[^/]+)?\.(?:yaml|yml)$/.test(rel)
        || ['Dockerfile', 'mise.toml', 'pom.xml', 'build.gradle', 'build.gradle.kts'].includes(name)
    })
    const operationalText = (await Promise.all(operationalFiles.map((path) => readFile(path, 'utf8').catch(() => '')))).join('\n')
    for (const { key } of envEntries) {
      if (!new RegExp(`\\b${escapeRegExp(key)}\\b`).test(operationalText)) {
        finding('warning', 'env.orphan', `${key} is documented in .env.sample but is not referenced by generated/runtime files`)
      }
    }
  }

  for (const profile of parseProfiles(envEntries)) {
    const profileFile = configFiles.some((path) => basename(path).startsWith(`application-${profile}.`))
    if (!profileFile) finding('warning', 'profile.file.missing', `SPRING_PROFILES_ACTIVE=${profile} has no application-${profile} configuration file`)
  }

  if (hasJpa) {
    if (!hasYamlProperty(configText, 'spring.jpa.open-in-view', 'false')) {
      finding('warning', 'jpa.open-in-view', 'JPA is present but spring.jpa.open-in-view is not explicitly false')
    }
    if (!hasYamlProperty(configText, 'spring.jpa.hibernate.ddl-auto', 'validate')) {
      finding('warning', 'jpa.schema-policy', 'JPA is present but spring.jpa.hibernate.ddl-auto is not explicitly validate')
    }
    if (/ddl-auto\s*[:=]\s*(?:update|create|create-drop)|SPRING_JPA_HIBERNATE_DDL_AUTO[^\r\n]*:-?(?:update|create|create-drop)/.test(configText)) {
      finding('warning', 'jpa.schema-mutating', 'Configuration allows Hibernate to create or update the schema')
    }
  }

  if (/management[.\s\S]{0,160}exposure[.\s\S]{0,80}include\s*[:=]\s*["']?\*["']?/.test(configText)) {
    finding('warning', 'actuator.exposure', 'Actuator exposure includes every endpoint')
  }

  if (isMaven) {
    for (const match of buildText.matchAll(/<module>\s*([^<]+?)\s*<\/module>/g)) {
      const modulePath = match[1].replaceAll('\\', '/')
      if (!has(join(modulePath, 'pom.xml'))) finding('error', 'module.missing', `Maven module is declared but missing: ${modulePath}`)
    }
  } else {
    for (const includeBlock of buildText.matchAll(/include\s*\(([^)]*)\)/g)) {
      for (const match of includeBlock[1].matchAll(/["'](:[^"']+)["']/g)) {
        const modulePath = match[1].slice(1).replaceAll(':', '/')
        if (!has(modulePath)) finding('error', 'module.missing', `Gradle module is included but missing: ${modulePath}`)
      }
    }
  }

  const composeFiles = sourceFiles.filter((path) => /(?:^|\/)(?:compose|docker-compose)(?:\.[^/]+)?\.(?:yaml|yml)$/.test(relative(projectDir, path)))
  const composeText = (await Promise.all(composeFiles.map((path) => readFile(path, 'utf8').catch(() => '')))).join('\n')
  if (hasJpa && /SPRING_JPA_HIBERNATE_DDL_AUTO[^\r\n]*:-?(?:update|create|create-drop)/.test(composeText)) {
    finding('warning', 'compose.jpa.schema-mutating', 'Compose allows Hibernate to create or update the schema')
  }
  const postgresImages = [...composeText.matchAll(/^\s*image:\s*['"]?(postgres:[^'"\s]+)['"]?\s*$/gm)].map((match) => match[1])
  const testSource = (await Promise.all(sourceFiles.filter((path) => relative(projectDir, path).startsWith('src/test/')).map((path) => readFile(path, 'utf8').catch(() => '')))).join('\n')
  const testPostgresImages = [...testSource.matchAll(/postgres:[0-9][^"'\s)]*/g)].map((match) => match[0])
  if (postgresImages.length > 0 && testPostgresImages.length > 0 && !testPostgresImages.every((image) => postgresImages.includes(image))) {
    finding('warning', 'database.image.drift', `Compose PostgreSQL images (${[...new Set(postgresImages)].join(', ')}) differ from Testcontainers (${[...new Set(testPostgresImages)].join(', ')})`)
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
