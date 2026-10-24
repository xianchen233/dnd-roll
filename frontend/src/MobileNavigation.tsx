import React, { useRef } from 'react';

const tabs = [
  ['map', '地图'], ['characters', '角色卡'], ['dice', '掷骰'],
  ['timeline', 'RP 与日志'], ['notes', '团队笔记'], ['combat', '战斗记录'],
] as const;
export type MobilePanel = typeof tabs[number][0];

export function MobileNavigation({ selected, disabled, onSelect }: { selected: MobilePanel; disabled: boolean; onSelect(panel: MobilePanel): void }) {
  const buttons = useRef<(HTMLButtonElement | null)[]>([]);
  return <nav className="mobile-navigation" role="tablist" aria-label="手机工作区">{tabs.map(([id, label], index) =>
    <button key={id} ref={button => { buttons.current[index] = button; }} type="button" role="tab"
      aria-selected={selected === id} tabIndex={selected === id ? 0 : -1} disabled={disabled}
      onClick={() => onSelect(id)} onKeyDown={event => {
        const next = event.key === 'ArrowRight' ? (index + 1) % tabs.length : event.key === 'ArrowLeft' ? (index + tabs.length - 1) % tabs.length : event.key === 'Home' ? 0 : event.key === 'End' ? tabs.length - 1 : undefined;
        if (next === undefined) return;
        event.preventDefault(); onSelect(tabs[next][0]); buttons.current[next]?.focus();
      }}>{label}</button>)}</nav>;
}
