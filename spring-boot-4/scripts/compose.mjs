import { readFile, rm, writeFile } from 'node:fs/promises'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const COMPOSE_FILE = 'compose.yaml'
const TEMPLATE_PATH = join(root, 'assets', 'compose.yaml')
const IMAGE_TOKEN = '__POSTGRESQL_IMAGE__'
const PG18_VOLUME = '/var/lib/postgresql'
const PG18_VOLUME_FORBIDDEN = '/var/lib/postgresql/data'

/** @typedef {'postgresql' | 'none'} Database */
/** @typedef {string} PostgresImage */
/** @typedef {{ kind: 'absent' } | { kind: 'owned', image: PostgresImage }} ComposePlan */
/** @typedef {{ hasPostgres: boolean, image: PostgresImage }} ComposeInspectRequest */
/** @typedef {{ code: string, message: string }} ComposeIssue */

function planCompose(database, image) {
  if (database === 'postgresql') return { kind: 'owned', image }
  if (database === 'none') return { kind: 'absent' }
  throw new Error(`unsupported database: ${database}`)
}

function renderCompose(template, image) {
  const count = template.split(IMAGE_TOKEN).length - 1
  if (count !== 1) throw new Error(`compose template must contain ${IMAGE_TOKEN} exactly once`)
  return template.replace(IMAGE_TOKEN, image)
}

export function inspectCompose(composeText, { hasPostgres, image }) {
  if (!hasPostgres) return []
  if (composeText == null) {
    return [{ code: 'compose.missing', message: 'compose.yaml is missing' }]
  }
  const issues = []
  if (!composeText.includes(image)) {
    issues.push({ code: 'compose.image', message: `compose.yaml image is not ${image}` })
  }
  const forbiddenVolume = new RegExp(`:\\s*${PG18_VOLUME_FORBIDDEN.replaceAll('/', '\\/')}\\s*$`, 'm')
  const requiredVolume = new RegExp(`:\\s*${PG18_VOLUME.replaceAll('/', '\\/')}\\s*$`, 'm')
  if (forbiddenVolume.test(composeText)) {
    issues.push({
      code: 'compose.volume',
      message: `compose.yaml mounts ${PG18_VOLUME_FORBIDDEN}; PostgreSQL 18 needs ${PG18_VOLUME}`,
    })
  } else if (!requiredVolume.test(composeText)) {
    issues.push({
      code: 'compose.volume',
      message: `compose.yaml does not mount ${PG18_VOLUME}`,
    })
  }
  return issues
}

export async function applyCompose(projectDir, database) {
  const versions = JSON.parse(await readFile(join(root, 'versions.json'), 'utf8'))
  const plan = planCompose(database, versions.postgresql.image)
  const composePath = join(projectDir, COMPOSE_FILE)
  if (plan.kind === 'absent') {
    await rm(composePath, { force: true })
    return
  }
  const template = await readFile(TEMPLATE_PATH, 'utf8')
  const text = renderCompose(template, plan.image)
  const issues = inspectCompose(text, { hasPostgres: true, image: plan.image })
  if (issues.length > 0) {
    throw new Error(issues.map(({ code, message }) => `${code}: ${message}`).join('; '))
  }
  await writeFile(composePath, text)
}
