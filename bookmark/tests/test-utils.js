/**
 * Bookmark skill test utilities
 */

import { writeFileSync, existsSync, rmSync } from 'fs';
import { join } from 'path';
import { homedir } from 'os';

export function getTestFilePath(filename = 'bookmarks.test.md') {
  return join(homedir(), '.claude', 'skills', 'bookmark', filename);
}

export function cleanup(filePath) {
  if (existsSync(filePath)) {
    rmSync(filePath);
  }
}

export function initTestFile(filePath, content = '# Bookmarks\n\nSaved links for later reference.\n\n---\n') {
  writeFileSync(filePath, content, 'utf-8');
}
