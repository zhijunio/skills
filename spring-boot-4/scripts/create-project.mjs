#!/usr/bin/env node

import { execFileSync } from 'node:child_process'
import { chmod, mkdtemp, mkdir, readFile, rename, rm, writeFile } from 'node:fs/promises'
import { existsSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const versions = JSON.parse(await readFile(join(root, 'versions.json'), 'utf8'))
const timeoutMs = 60_000

function usage() {
  console.log(`Create a Spring Boot 4 Maven skeleton from Spring Initializr.

Usage:
  node scripts/create-project.mjs --name <name> [options]

Options:
  --name <name>             Project directory and artifact name (required)
  --group-id <id>           Java group id (default: com.example)
  --package-name <name>     Java package (default: <group-id>.<project-name>)
  --java <version>          Java major version (default: ${versions.java.default})
  --boot-version <version>  Exact Spring Boot version (default: ${versions.springBoot.maven})
  --output-dir <path>       Parent directory (default: current directory)
  --help                    Show this help

The zip contains web, validation, and actuator. It pins the Maven parent and
does not add house plugins. After generation, follow SKILL.md Create.

Examples:
  node scripts/create-project.mjs --name inventory-api --output-dir /absolute/path/to/workspace`)
}

function parseArgs(args) {
  const options = { outputdir: process.cwd() }
  const values = new Set(['--name', '--group-id', '--package-name', '--java', '--boot-version', '--output-dir'])
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
  const javaKeywords = new Set([
    'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'const', 'continue',
    'default', 'do', 'double', 'else', 'enum', 'extends', 'final', 'finally', 'float', 'for', 'goto', 'if',
    'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'native', 'new', 'package', 'private',
    'protected', 'public', 'return', 'short', 'static', 'strictfp', 'super', 'switch', 'synchronized', 'this',
    'throw', 'throws', 'transient', 'try', 'void', 'volatile', 'while', '_', 'true', 'false', 'null',
  ])
  const isJavaPackage = (value) =>
    /^[A-Za-z_][\w]*(\.[A-Za-z_][\w]*)*$/.test(value) &&
    value.split('.').every((segment) => !javaKeywords.has(segment))

  const groupId = options.groupid ?? 'com.example'
  if (!isJavaPackage(groupId)) throw new Error('--group-id must be a valid Java package prefix')
  options.groupid = groupId
  const defaultPackageSegment = options.name.replaceAll(/[^A-Za-z0-9_]/g, '').toLowerCase() || 'app'
  const safeDefaultSegment = /^\d/.test(defaultPackageSegment) || javaKeywords.has(defaultPackageSegment)
    ? `app${defaultPackageSegment}`
    : defaultPackageSegment
  options.packagename ??= `${groupId}.${safeDefaultSegment}`
  if (!isJavaPackage(options.packagename)) throw new Error('--package-name must be a valid Java package')
  options.java ??= versions.java.default
  options.bootversion ??= versions.springBoot.maven
  if (!/^\d+$/.test(options.java)) throw new Error('--java must be a Java major version, such as 21')
  options.java = String(Number(options.java))
  if (!/^\d+\.\d+\.\d+(?:\.RELEASE)?$/.test(options.bootversion)) {
    throw new Error('--boot-version must be an exact stable version, such as 4.2.0.RELEASE')
  }
  options.initializrBootVersion = /^\d+\.\d+\.\d+$/.test(options.bootversion)
    ? `${options.bootversion}.RELEASE`
    : options.bootversion
  options.mavenBootVersion = options.bootversion.replace(/\.RELEASE$/, '')
  const [bootMajor, bootMinor] = options.mavenBootVersion.split('.')
  if (Number(bootMajor) < 4) {
    throw new Error('This generator only creates Spring Boot 4.x projects')
  }
  if (!versions.springBoot.supportedLines.includes(`${bootMajor}.${bootMinor}.x`)) {
    throw new Error(`Spring Boot ${bootMajor}.${bootMinor}.x is not listed as a supported line in versions.json`)
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

async function pinMavenParent(projectDir, mavenVersion) {
  const pomPath = join(projectDir, 'pom.xml')
  const pom = await readFile(pomPath, 'utf8')
  const next = pom.replace(
    /(<artifactId>spring-boot-starter-parent<\/artifactId>\s*<version>)[^<]+(<\/version>)/,
    `$1${mavenVersion}$2`,
  )
  if (next === pom) throw new Error('generated pom.xml has no spring-boot-starter-parent version to pin')
  await writeFile(pomPath, next)
}

async function main() {
  const { help, options } = parseArgs(process.argv.slice(2))
  if (help) return usage()
  assertOptions(options)

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
    dependencies: 'web,validation,actuator',
  })

  const tempDir = await mkdtemp(join(tmpdir(), 'spring-boot-4-'))
  const zipPath = join(tempDir, 'project.zip')
  const extracted = join(tempDir, options.name)
  const initializrUrl = new URL('/starter.zip', versions.sources.springInitializr)
  initializrUrl.search = query.toString()
  const url = initializrUrl.toString()

  try {
    console.log(`Requesting Spring Boot ${options.initializrBootVersion} (maven, Java ${options.java}) from start.spring.io`)
    const response = await fetch(url, { signal: AbortSignal.timeout(timeoutMs) })
    if (!response.ok) {
      const detail = (await response.text()).slice(0, 1000)
      throw new Error(`Spring Initializr returned HTTP ${response.status}: ${detail}`)
    }
    await writeFile(zipPath, new Uint8Array(await response.arrayBuffer()))
    run('unzip', ['-q', zipPath, '-d', tempDir])
    if (!existsSync(extracted)) throw new Error('Spring Initializr archive did not contain the requested project directory')
    await rm(join(extracted, 'HELP.md'), { force: true })
    await pinMavenParent(extracted, options.mavenBootVersion)
    await mkdir(options.outputdir, { recursive: true })
    await rename(extracted, options.projectDir)
    const wrapper = join(options.projectDir, 'mvnw')
    if (process.platform !== 'win32' && existsSync(wrapper)) await chmod(wrapper, 0o755)
    console.log(`\nProject created: ${options.projectDir}`)
    console.log(`Next: cd ${options.projectDir}`)
    console.log('Then follow SKILL.md Create: house stacks, audit Rules, unit tests last')
  } finally {
    await rm(tempDir, { recursive: true, force: true })
  }
}

main().catch((error) => {
  console.error(`Project generation failed: ${error.message}`)
  process.exitCode = 1
})
