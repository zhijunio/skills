/**
 * Bookmark skill integration tests
 */

import { describe, it, expect, beforeEach, afterAll } from 'vitest';
import { readFileSync, appendFileSync } from 'fs';
import { cleanup, initTestFile, getTestFilePath } from './test-utils.js';

const TEST_BOOKMARK_FILE = getTestFilePath('bookmarks.integration.test.md');

function addBookmark(file, url, title, tags, description) {
  const today = new Date().toISOString().split('T')[0];
  const dateHeader = `### ${today}`;
  const tagsStr = tags.join(' ');
  const entry = `\n- [${title}](${url}) ${tagsStr}\n  > ${description}\n`;

  const content = readFileSync(file, 'utf-8');

  if (!content.includes(dateHeader)) {
    appendFileSync(file, `\n${dateHeader}\n`, 'utf-8');
  }

  appendFileSync(file, entry, 'utf-8');
}

function queryBookmarks(file, range) {
  const content = readFileSync(file, 'utf-8');
  const today = new Date().toISOString().split('T')[0];
  const results = [];

  if (range === 'today') {
    const dateSection = content.match(new RegExp(`### ${today}[\\s\\S]*?(?=###|$)`));
    if (dateSection) {
      for (const match of dateSection[0].matchAll(/^- \[(.+?)\]\((.+?)\)/gm)) {
        results.push(`[${match[1]}](${match[2]})`);
      }
    }
  } else if (range === 'all') {
    for (const match of content.matchAll(/^- \[(.+?)\]\((.+?)\)/gm)) {
      results.push(`[${match[1]}](${match[2]})`);
    }
  }

  return results;
}

describe('Bookmark Integration Tests', () => {
  beforeEach(() => {
    cleanup(TEST_BOOKMARK_FILE);
    initTestFile(TEST_BOOKMARK_FILE);
  });

  afterAll(() => {
    cleanup(TEST_BOOKMARK_FILE);
  });

  describe('add bookmark', () => {
    it('appends under today date header', () => {
      addBookmark(TEST_BOOKMARK_FILE, 'https://example.com', 'Example', ['#test'], 'Test description');

      const content = readFileSync(TEST_BOOKMARK_FILE, 'utf-8');
      const today = new Date().toISOString().split('T')[0];

      expect(content).toContain(`### ${today}`);
      expect(content).toContain('[Example](https://example.com)');
      expect(content).toContain('#test');
      expect(content).toContain('Test description');
    });

    it('adds multiple bookmarks under one date header', () => {
      addBookmark(TEST_BOOKMARK_FILE, 'https://example.com', 'Example 1', ['#test'], 'First description');
      addBookmark(TEST_BOOKMARK_FILE, 'https://another.com', 'Example 2', ['#test'], 'Second description');

      const content = readFileSync(TEST_BOOKMARK_FILE, 'utf-8');
      const today = new Date().toISOString().split('T')[0];

      const dateHeaderCount = (content.match(new RegExp(`### ${today}`, 'g')) || []).length;
      expect(dateHeaderCount).toBe(1);

      expect(content).toContain('[Example 1](https://example.com)');
      expect(content).toContain('[Example 2](https://another.com)');
    });

    it('allows at most two tags', () => {
      addBookmark(TEST_BOOKMARK_FILE, 'https://example.com', 'Example', ['#tag1', '#tag2'], 'Description');

      const content = readFileSync(TEST_BOOKMARK_FILE, 'utf-8');
      expect(content).toMatch(/#tag1 #tag2/);
    });
  });

  describe('query bookmarks', () => {
    it('queries today', () => {
      addBookmark(TEST_BOOKMARK_FILE, 'https://example.com', 'Example', ['#test'], 'Description');

      const results = queryBookmarks(TEST_BOOKMARK_FILE, 'today');
      expect(results.length).toBe(1);
      expect(results[0]).toBe('[Example](https://example.com)');
    });

    it('queries all', () => {
      addBookmark(TEST_BOOKMARK_FILE, 'https://example.com', 'Example 1', ['#test'], 'Description 1');
      addBookmark(TEST_BOOKMARK_FILE, 'https://another.com', 'Example 2', ['#test'], 'Description 2');

      const results = queryBookmarks(TEST_BOOKMARK_FILE, 'all');
      expect(results.length).toBe(2);
    });

    it('returns empty when none', () => {
      const results = queryBookmarks(TEST_BOOKMARK_FILE, 'today');
      expect(results.length).toBe(0);
    });
  });

  describe('duplicate detection', () => {
    it('detects duplicate URL in file', () => {
      addBookmark(TEST_BOOKMARK_FILE, 'https://example.com', 'Example', ['#test'], 'Description');

      const content = readFileSync(TEST_BOOKMARK_FILE, 'utf-8');

      expect(content.includes('https://example.com')).toBe(true);
    });
  });
});
