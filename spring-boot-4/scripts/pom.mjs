import { readFile, writeFile } from 'node:fs/promises'
import { join } from 'node:path'

/** @typedef {{ default: string }} VersionPin */
/** @typedef {{ mavenPlugin: string, palantirJavaFormat: string }} SpotlessPin */
/**
 * @typedef {{
 *   mybatisPlus: VersionPin,
 *   springdoc: VersionPin & { artifact: string },
 *   spotless: SpotlessPin,
 * }} MavenVersions
 */
/** @typedef {{ persistence: string }} MavenHouseOptions */

const SPRINGDOC_ARTIFACT = 'springdoc-openapi-starter-webmvc-ui'
const MYBATIS_ARTIFACT = 'mybatis-plus-spring-boot4-starter'

function setProperty(pom, name, value) {
  const pattern = new RegExp(`<${name}>[^<]*</${name}>`)
  if (pattern.test(pom)) return pom.replace(pattern, `<${name}>${value}</${name}>`)
  if (!pom.includes('</properties>')) throw new Error('generated pom.xml has no <properties> block')
  return pom.replace('</properties>', `\t\t<${name}>${value}</${name}>\n\t</properties>`)
}

function ensureDependencyManagement(pom) {
  if (pom.includes('<dependencyManagement>')) return pom
  const marker = '\t<dependencies>'
  const index = pom.indexOf(marker)
  if (index === -1) throw new Error('generated pom.xml has no <dependencies> block')
  const block = `\t<dependencyManagement>\n\t\t<dependencies>\n\t\t</dependencies>\n\t</dependencyManagement>\n`
  return `${pom.slice(0, index)}${block}${pom.slice(index)}`
}

function managementInnerClose(pom) {
  const end = pom.indexOf('</dependencyManagement>')
  if (end === -1) throw new Error('generated pom.xml has no <dependencyManagement> block')
  const close = pom.lastIndexOf('</dependencies>', end)
  if (close === -1) throw new Error('generated pom.xml dependencyManagement has no <dependencies> block')
  return close
}

function insertManagedDependency(pom, artifactId, xml) {
  const start = pom.indexOf('<dependencyManagement>')
  const end = pom.indexOf('</dependencyManagement>')
  if (start === -1 || end === -1) throw new Error('generated pom.xml has no <dependencyManagement> block')
  if (pom.slice(start, end).includes(`<artifactId>${artifactId}</artifactId>`)) return pom
  const close = managementInnerClose(pom)
  return `${pom.slice(0, close)}${xml}${pom.slice(close)}`
}

function withoutManagement(pom) {
  return pom.replace(/<dependencyManagement>[\s\S]*?<\/dependencyManagement>/, '')
}

function stripInlineVersion(pom, artifactId) {
  const pattern = new RegExp(`(<artifactId>${artifactId}</artifactId>)\\s*<version>[^<]+</version>`)
  const start = pom.indexOf('<dependencyManagement>')
  const end = pom.indexOf('</dependencyManagement>')
  if (start === -1) return pom.replace(pattern, '$1')
  const prefix = pom.slice(0, start)
  const management = pom.slice(start, end)
  const suffix = pom.slice(end)
  return `${prefix}${management}${suffix.replace(pattern, '$1')}`
}

function ensurePluginManagement(pom) {
  if (pom.includes('<pluginManagement>')) return pom
  const build = pom.indexOf('\t<build>')
  if (build === -1) throw new Error('generated pom.xml has no <build> block')
  const insertAt = build + '\t<build>'.length
  const block = `\n\t\t<pluginManagement>\n\t\t\t<plugins>\n\t\t\t</plugins>\n\t\t</pluginManagement>`
  return `${pom.slice(0, insertAt)}${block}${pom.slice(insertAt)}`
}

function insertManagedPlugin(pom, artifactId, xml) {
  const start = pom.indexOf('<pluginManagement>')
  const end = pom.indexOf('</pluginManagement>')
  if (start === -1 || end === -1) throw new Error('generated pom.xml has no <pluginManagement> block')
  if (pom.slice(start, end).includes(`<artifactId>${artifactId}</artifactId>`)) return pom
  const close = pom.lastIndexOf('</plugins>', end)
  return `${pom.slice(0, close)}${xml}${pom.slice(close)}`
}

function insertPlugin(pom, artifactId, xml) {
  const pluginsOnly = pom.replace(/<pluginManagement>[\s\S]*?<\/pluginManagement>/, '')
  if (pluginsOnly.includes(`<artifactId>${artifactId}</artifactId>`)) return pom
  const close = pom.lastIndexOf('</plugins>')
  if (close === -1) throw new Error('generated pom.xml has no <plugins> block')
  return `${pom.slice(0, close)}${xml}${pom.slice(close)}`
}

function managedDependencyXml(groupId, artifactId, property) {
  return `\t\t\t<dependency>\n\t\t\t\t<groupId>${groupId}</groupId>\n\t\t\t\t<artifactId>${artifactId}</artifactId>\n\t\t\t\t<version>\${${property}}</version>\n\t\t\t</dependency>\n\t\t`
}

/**
 * Put third-party versions in properties and dependencyManagement.
 * Add Spotless and Surefire. Idempotent.
 *
 * @param {string} projectDir
 * @param {MavenHouseOptions} options
 * @param {MavenVersions} versions
 */
export async function applyMavenHouseStyle(projectDir, options, versions) {
  const pomPath = join(projectDir, 'pom.xml')
  let pom = await readFile(pomPath, 'utf8')

  pom = setProperty(pom, 'springdoc.version', versions.springdoc.default)
  pom = setProperty(pom, 'spotless.version', versions.spotless.mavenPlugin)
  pom = setProperty(pom, 'palantir-java-format.version', versions.spotless.palantirJavaFormat)
  if (options.persistence === 'mybatis-plus') {
    pom = setProperty(pom, 'mybatis-plus.version', versions.mybatisPlus.default)
  }

  pom = ensureDependencyManagement(pom)
  pom = insertManagedDependency(
    pom,
    SPRINGDOC_ARTIFACT,
    managedDependencyXml('org.springdoc', SPRINGDOC_ARTIFACT, 'springdoc.version'),
  )
  pom = stripInlineVersion(pom, SPRINGDOC_ARTIFACT)
  if (options.persistence === 'mybatis-plus') {
    pom = insertManagedDependency(
      pom,
      MYBATIS_ARTIFACT,
      managedDependencyXml('com.baomidou', MYBATIS_ARTIFACT, 'mybatis-plus.version'),
    )
    pom = stripInlineVersion(pom, MYBATIS_ARTIFACT)
  }

  pom = ensurePluginManagement(pom)
  pom = insertManagedPlugin(
    pom,
    'spotless-maven-plugin',
    `\t\t\t\t<plugin>\n\t\t\t\t\t<groupId>com.diffplug.spotless</groupId>\n\t\t\t\t\t<artifactId>spotless-maven-plugin</artifactId>\n\t\t\t\t\t<version>\${spotless.version}</version>\n\t\t\t\t</plugin>\n\t\t\t`,
  )
  pom = insertPlugin(
    pom,
    'maven-surefire-plugin',
    `\t\t\t<plugin>\n\t\t\t\t<groupId>org.apache.maven.plugins</groupId>\n\t\t\t\t<artifactId>maven-surefire-plugin</artifactId>\n\t\t\t\t<configuration>\n\t\t\t\t\t<failIfNoTests>true</failIfNoTests>\n\t\t\t\t</configuration>\n\t\t\t</plugin>\n\t\t`,
  )
  pom = insertPlugin(
    pom,
    'spotless-maven-plugin',
    `\t\t\t<plugin>\n\t\t\t\t<groupId>com.diffplug.spotless</groupId>\n\t\t\t\t<artifactId>spotless-maven-plugin</artifactId>\n\t\t\t\t<configuration>\n\t\t\t\t\t<java>\n\t\t\t\t\t\t<importOrder/>\n\t\t\t\t\t\t<removeUnusedImports/>\n\t\t\t\t\t\t<formatAnnotations/>\n\t\t\t\t\t\t<palantirJavaFormat>\n\t\t\t\t\t\t\t<version>\${palantir-java-format.version}</version>\n\t\t\t\t\t\t</palantirJavaFormat>\n\t\t\t\t\t</java>\n\t\t\t\t</configuration>\n\t\t\t\t<executions>\n\t\t\t\t\t<execution>\n\t\t\t\t\t\t<goals>\n\t\t\t\t\t\t\t<goal>check</goal>\n\t\t\t\t\t\t</goals>\n\t\t\t\t\t</execution>\n\t\t\t\t</executions>\n\t\t\t</plugin>\n\t\t`,
  )

  await writeFile(pomPath, pom)
}

export function dependenciesBlock(pom) {
  return withoutManagement(pom)
}
