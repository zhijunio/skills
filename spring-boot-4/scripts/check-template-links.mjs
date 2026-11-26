#!/usr/bin/env node

import { readdir, readFile, stat } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const failures = [];
const versions = JSON.parse(await readFile(path.join(root, 'versions.json'), 'utf8'));

async function markdownFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = await Promise.all(
    entries.map(async (entry) => {
      const entryPath = path.join(directory, entry.name);
      if (entry.isDirectory()) return markdownFiles(entryPath);
      return entry.isFile() && entry.name.endsWith('.md') ? [entryPath] : [];
    }),
  );
  return files.flat();
}

function markdownDestinations(content) {
  return [...content.matchAll(/\[[^\]]*\]\(([^)]+)\)/g)].map(([, link]) => {
    const trimmedLink = link.trim();
    const destination = trimmedLink.startsWith('<')
      ? trimmedLink.slice(1, trimmedLink.indexOf('>'))
      : trimmedLink.split(/\s+/, 1)[0];
    return decodeURIComponent(destination);
  });
}

const assetsDirectory = path.join(root, 'assets');
const readmePath = path.join(root, 'README.md');
const readmeContent = await readFile(readmePath, 'utf8');
const assetEntries = await readdir(assetsDirectory, { withFileTypes: true });
const templates = assetEntries
  .filter((entry) => entry.isDirectory() && entry.name.startsWith('template-'))
  .map((entry) => entry.name);

const documents = [
  path.join(root, 'README.md'),
  path.join(root, 'SKILL.md'),
  ...(await markdownFiles(path.join(root, 'references'))),
  ...templates.map((template) => path.join(assetsDirectory, template, 'README.md')),
];
const contents = new Map(
  await Promise.all(
    documents.map(async (file) => {
      try {
        return [file, await readFile(file, 'utf8')];
      } catch {
        return [file, ''];
      }
    }),
  ),
);

for (const [file, content] of contents) {
  for (const destination of markdownDestinations(content)) {
    if (/^(?:[a-z][a-z\d+.-]*:|#)/i.test(destination)) continue;

    const fileDestination = destination.split('#', 1)[0];
    if (!fileDestination) continue;

    const target = path.resolve(path.dirname(file), fileDestination);
    try {
      await stat(target);
    } catch {
      failures.push(`${path.relative(root, file)}: missing link target ${destination}`);
    }
  }
}

for (const template of templates) {
  const readme = `assets/${template}/README.md`;
  const templateReadme = path.join(root, readme);
  try {
    await stat(templateReadme);
  } catch {
    failures.push(`assets/${template}: missing README.md`);
    continue;
  }

  const pomPath = path.join(root, 'assets', template, 'pom.xml');
  try {
    const pom = await readFile(pomPath, 'utf8');
    const bootParent = pom.match(
      /<parent>[\s\S]*?<artifactId>spring-boot-starter-parent<\/artifactId>[\s\S]*?<version>([^<]+)<\/version>/,
    )?.[1];
    const javaVersion = pom.match(/<java\.version>([^<]+)<\/java\.version>/)?.[1];
    if (bootParent !== versions.springBoot.maven) {
      failures.push(
        `assets/${template}/pom.xml: Spring Boot version ${bootParent ?? 'missing'} does not match versions.json (${versions.springBoot.maven})`,
      );
    }
    if (javaVersion !== versions.java.default) {
      failures.push(
        `assets/${template}/pom.xml: Java version ${javaVersion ?? 'missing'} does not match versions.json (${versions.java.default})`,
      );
    }
  } catch {
    // A runnable template may use another build tool and need not have a pom.xml.
  }

  const composePath = path.join(root, 'assets', template, 'compose.yaml');
  try {
    const compose = await readFile(composePath, 'utf8');
    const managedImages = [
      versions.postgresql?.image,
      versions.redis?.image,
      versions.mailpit?.image,
    ].filter(Boolean);
    for (const [, image] of compose.matchAll(/^\s+image:\s*([^\s#]+)/gm)) {
      if (
        managedImages.some((managedImage) => image.startsWith(`${managedImage.split(':')[0]}:`)) &&
        !managedImages.includes(image)
      ) {
        failures.push(
          `assets/${template}/compose.yaml: image ${image} does not match a pin in versions.json`,
        );
      }
    }
  } catch {
    // Compose is optional for templates that do not use local services.
  }

  for (const indexFile of ['README.md', 'SKILL.md']) {
    const indexDestinations = markdownDestinations(
      contents.get(path.join(root, indexFile)) ?? '',
    );
    if (!indexDestinations.includes(readme)) {
      failures.push(`${indexFile}: missing template entry ${readme}`);
    }
  }

  const templateRow = readmeContent
    .split('\n')
    .find((line) => line.startsWith('|') && line.includes(`(${readme})`));
  const matchingReferences = templateRow
    ? markdownDestinations(templateRow).filter((destination) =>
        destination.startsWith('references/'),
      )
    : [];
  if (matchingReferences.length === 0) {
    failures.push(`README.md: no matching references listed for assets/${template}`);
    continue;
  }

  for (const reference of matchingReferences) {
    const referencePath = path.resolve(root, reference.split('#', 1)[0]);
    const referenceContent = contents.get(referencePath);
    if (!referenceContent) {
      failures.push(`README.md: matching reference does not exist: ${reference}`);
      continue;
    }

    const templateDirectory = path.join(assetsDirectory, template) + path.sep;
    let linksToTemplate = false;
    for (const destination of markdownDestinations(referenceContent)) {
      if (/^(?:[a-z][a-z\d+.-]*:|#)/i.test(destination)) continue;
      const target = path.resolve(path.dirname(referencePath), destination.split('#', 1)[0]);
      if (!target.startsWith(templateDirectory)) continue;
      try {
        await stat(target);
        linksToTemplate = true;
        break;
      } catch {
        // All Markdown link targets are reported by the general link check above.
      }
    }
    if (!linksToTemplate) {
      failures.push(
        `${path.relative(root, referencePath)}: no existing example link into assets/${template}`,
      );
    }
  }
}

if (failures.length > 0) {
  console.error(failures.join('\n'));
  process.exitCode = 1;
} else {
  console.log(
    `Template links, reference links, and version pins are valid for ${templates.length} runnable templates. Semantic parity still requires code review.`,
  );
}
