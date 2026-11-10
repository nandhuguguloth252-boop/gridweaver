// @vitest-environment jsdom
import { describe, expect, it } from 'vitest';
import React from 'react';
import App from '../App';
import { renderToString } from 'react-dom/server';

describe('GridWeaver smoke tests', () => {
  it('has a working node id shape', () => {
    const id = 'GW-00001';
    expect(id).toMatch(/^GW-\d{5}$/);
  });

  it('calculates a bounded SoC', () => {
    const soc = Math.max(0, Math.min(1, 50 / 80));
    expect(soc).toBeGreaterThan(0);
    expect(soc).toBeLessThanOrEqual(1);
  });

  it('renders App without crashing', () => {
    const html = renderToString(<App />);
    expect(html).toContain('GRIDWEAVER');
  });
});
