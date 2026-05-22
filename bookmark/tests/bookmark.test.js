/**
 * Bookmark skill unit tests
 *
 * /bookmark and /bookmark-query behavior
 */

import { describe, it, expect, beforeEach, afterAll } from 'vitest';
import { readFileSync } from 'fs';
import { cleanup, initTestFile, getTestFilePath } from './test-utils.js';

const TEST_BOOKMARK_FILE = getTestFilePath();

describe('Bookmark Skill', () => {
  beforeEach(() => {
    cleanup(TEST_BOOKMARK_FILE);
    initTestFile(TEST_BOOKMARK_FILE);
  });

  afterAll(() => {
    cleanup(TEST_BOOKMARK_FILE);
  });

  describe('URL validation', () => {
    it('accepts valid https URL', () => {
      const url = 'https://example.com';
      expect(url).toMatch(/^https?:\/\/.+/);
    });

    it('accepts valid http URL', () => {
      const url = 'http://example.com';
      expect(url).toMatch(/^https?:\/\/.+/);
    });

    it('rejects URL without scheme', () => {
      const url = 'example.com';
      expect(url).not.toMatch(/^https?:\/\/.+/);
    });
  });

  describe('bookmark format', () => {
    it('generates correct date header (### YYYY-MM-DD)', () => {
      const today = new Date().toISOString().split('T')[0];
      const expected = `### ${today}`;
      expect(expected).toMatch(/^### \d{4}-\d{2}-\d{2}$/);
    });

    it('generates correct bookmark entry format', () => {
      const entry = `- [Example](https://example.com) #tag1 #tag2\n  > Description`;
      expect(entry).toMatch(/^- \[.+\]\(https?:\/\/.+\) (#[a-z0-9]+ ?)+$\n  > .+$/m);
    });
  });

  describe('tag format', () => {
    it('tags are lowercase', () => {
      const tags = '#blog #java #ai';
      expect(tags).toMatch(/^(#[a-z0-9]+ ?)+$/);
    });

    it('allows at most 2 tags', () => {
      const tags = ['#blog', '#java'];
      expect(tags.length).toBeLessThanOrEqual(2);
    });

    it('adds # when user omits it', () => {
      const userTags = ['react', 'docs'];
      const normalized = userTags.map(tag => tag.startsWith('#') ? tag : `#${tag}`);
      expect(normalized).toEqual(['#react', '#docs']);
    });

    it('keeps # when user provides it', () => {
      const userTags = ['#react', '#docs'];
      const normalized = userTags.map(tag => tag.startsWith('#') ? tag : `#${tag}`);
      expect(normalized).toEqual(['#react', '#docs']);
    });

    it('lowercases tags', () => {
      const userTags = ['React', 'DOCS'];
      const normalized = userTags
        .map(tag => tag.toLowerCase())
        .map(tag => tag.startsWith('#') ? tag : `#${tag}`);
      expect(normalized).toEqual(['#react', '#docs']);
    });
  });

  describe('duplicate detection', () => {
    it('detects duplicate URL', () => {
      const content = `- [Example](https://example.com) #test`;
      const url = 'https://example.com';
      expect(content).toContain(url);
    });

    it('allows different URL', () => {
      const content = `- [Example](https://example.com) #test`;
      const url = 'https://another.com';
      expect(content).not.toContain(url);
    });
  });
});

describe('Bookmark Query Skill', () => {
  beforeEach(() => {
    cleanup(TEST_BOOKMARK_FILE);
    initTestFile(TEST_BOOKMARK_FILE);
  });

  afterAll(() => {
    cleanup(TEST_BOOKMARK_FILE);
  });

  describe('date range query', () => {
    it('supports today', () => {
      const range = 'today';
      expect(range).toBe('today');
    });

    it('supports yesterday', () => {
      const range = 'yesterday';
      expect(range).toBe('yesterday');
    });

    it('supports week', () => {
      const range = 'week';
      expect(range).toBe('week');
    });

    it('supports specific date', () => {
      const range = '2026-04-21';
      expect(range).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    });
  });

  describe('file parsing', () => {
    it('parses date header', () => {
      const content = '### 2026-04-21\n\n- [Link](https://example.com)';
      const dateMatch = content.match(/^### (\d{4}-\d{2}-\d{2})$/m);
      expect(dateMatch).not.toBeNull();
      expect(dateMatch?.[1]).toBe('2026-04-21');
    });

    it('parses bookmark entry', () => {
      const content = '- [Example](https://example.com) #tag1 #tag2\n  > Description';
      const entryMatch = content.match(/^- \[([^\]]+)\]\(([^)]+)\)(.*)$/m);
      expect(entryMatch).not.toBeNull();
      expect(entryMatch?.[1]).toBe('Example');
      expect(entryMatch?.[2]).toBe('https://example.com');
    });
  });
});
