import React from 'react';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import { Appearance } from './Appearance';
afterEach(()=>{cleanup();localStorage.clear();document.documentElement.removeAttribute('data-palette');vi.restoreAllMocks();});
it('changes palette and restores only a valid local preference after remount',()=>{
  const first=render(<Appearance/>);
  fireEvent.change(screen.getByLabelText('界面配色'),{target:{value:'parchment'}});
  expect(document.documentElement.dataset.palette).toBe('parchment');
  expect(localStorage.getItem('dnd-roll.palette')).toBe('parchment');
  first.unmount();render(<Appearance/>);
  expect(screen.getByLabelText('界面配色')).toHaveValue('parchment');
});
it('rejects unknown stored palettes and works when browser storage is unavailable',()=>{
  localStorage.setItem('dnd-roll.palette','unknown');
  const first=render(<Appearance/>);expect(screen.getByLabelText('界面配色')).toHaveValue('obsidian');first.unmount();
  vi.spyOn(Storage.prototype,'getItem').mockImplementation(()=>{throw new Error('blocked');});
  vi.spyOn(Storage.prototype,'setItem').mockImplementation(()=>{throw new Error('blocked');});
  render(<Appearance/>);fireEvent.change(screen.getByLabelText('界面配色'),{target:{value:'moon'}});
  expect(document.documentElement.dataset.palette).toBe('moon');
});
