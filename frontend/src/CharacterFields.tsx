import React, { createContext, useContext } from 'react';
import { abilities, itemTemplate, type Sheet } from './characters';
const SheetContext = createContext<Sheet | undefined>(undefined);
const labels: Record<string, string> = { name: '名称', avatar: '头像标记', species: '物种', background: '背景', entry: '条目', increases: '已选属性提升（仅记录）', originFeat: '起源专长关联ID', proficiencies: '熟练关联', equipment: '装备与物品', classes: '职业与子职', subclass: '子职', level: '等级', abilities: '六项属性', skills: '技能', saves: '豁免', ability: '关联属性', proficiency: '熟练情况', bonus: '额外修正', combat: '生命与战斗', maxHp: '生命上限', hp: '当前生命', tempHp: '临时生命', speed: '速度', deathSuccesses: '死亡豁免成功', deathFailures: '死亡豁免失败', acMode: 'AC计算方式', manualAc: '手动AC', attacks: '攻击', proficient: '具有熟练', hitBonus: '命中额外加值', damageBonus: '伤害额外加值', dice: '伤害骰式', damageType: '伤害类型', mastery: '已取得的武器精通选择', casting: '施法来源', attackBonus: '法术攻击额外加值', dcBonus: 'DC额外加值', spells: '法术', prepared: '已准备', castingId: '施法来源ID', features: '专长与能力', kind: '类别', choices: '已选选项', resources: '资源', current: '当前值', max: '上限', recovery: '恢复说明', quantity: '数量', equipped: '已装备', money: '金钱', cp: '铜币', sp: '银币', ep: '银金币', gp: '金币', pp: '铂金币', conditions: '状态', icon: '图标', remainingRounds: '剩余回合（空为不计）', expiresAt: '到期时间（UTC ISO；可空）', modifiers: '有来源的额外修正', target: '计算键', amount: '修正量', overrides: 'DM覆盖', value: '覆盖值', reason: '覆盖原因', notes: '角色备注', description: '说明', source: '来源', book: '来源书/自定义', revision: '来源版本', rules: '适用规则体系', detail: '来源定位/说明' };
const options: Record<string, string[]> = { ability: abilities, rules: ['5R', '2014', 'CUSTOM'], proficiency: ['NONE', 'PROFICIENT', 'EXPERT'], acMode: ['UNARMORED', 'MANUAL'] };
const optionNames: Record<string, string> = { NONE: '无熟练', PROFICIENT: '熟练', EXPERT: '专精', UNARMORED: '无甲：10 + 敏捷', MANUAL: '手动AC（填写来源）', HIT_DICE: '生命骰', SPELL_SLOT: '法术位', PACT_SLOT: '契约法术位', FEATURE: '能力次数', CUSTOM: '自定义', ORIGIN_FEAT: '起源专长', SPECIES: '物种特质', CLASS: '职业特性', FEAT: '专长' };
export const groups: { label: string; keys: (keyof Sheet)[] }[] = [
  { label: '基本资料', keys: ['name', 'avatar', 'species', 'background', 'classes'] }, { label: '属性与检定', keys: ['abilities', 'skills', 'saves'] }, { label: '生命与战斗', keys: ['combat'] }, { label: '攻击与施法', keys: ['attacks', 'casting', 'spells'] }, { label: '能力、资源与状态', keys: ['features', 'resources', 'conditions', 'modifiers', 'overrides'] }, { label: '装备与物品', keys: ['equipment', 'money', 'notes'] },
];
export function CharacterFields({ sheet, group, disabled, isDm, onChange }: { sheet: Sheet; group: number; disabled: boolean; isDm: boolean; onChange(sheet: Sheet): void }) {
  return <SheetContext.Provider value={sheet}><fieldset disabled={disabled} className="sheet-fields"><legend>{groups[group].label}</legend>{groups[group].keys.map(key => <Field key={key} field={key} path={key === 'name' ? '角色名称' : labels[key]} value={sheet[key]} disabled={key === 'overrides' && !isDm} onChange={v => onChange({ ...sheet, [key]: v })} />)}</fieldset></SheetContext.Provider>;
}
function Field({ field, path, value, onChange, disabled = false }: { field: string; path: string; value: unknown; onChange(value: unknown): void; disabled?: boolean }) {
  const sheet = useContext(SheetContext);
  if (sheet && (field === 'castingId' || field === 'originFeat')) {
    const candidates = field === 'castingId' ? sheet.casting : sheet.features.filter(f => f.kind === 'ORIGIN_FEAT');
    return <label>{path.replace('施法来源ID', '施法来源').replace('起源专长关联ID', '起源专长')}<select disabled={disabled} value={String(value)} onChange={e => onChange(e.target.value)}><option value="">尚未关联</option>{candidates.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label>;
  }
  if (Array.isArray(value)) {
    const fixed = field === 'skills' || field === 'saves';
    return <fieldset disabled={disabled} className="sheet-array"><legend>{labels[field] ?? field}</legend>{value.map((v, i) => {
      const record = typeof v === 'object' && v !== null ? v as Record<string, unknown> : undefined, title = record?.name || `${i + 1}`, id = typeof record?.id === 'string' ? record.id : String(i);
      return <div className="sheet-entry" key={id}><Field field={fixed ? 'check' : 'item'} path={`${path} · ${title}`} value={v} onChange={next => onChange(value.map((old, n) => n === i ? next : old))} />{!fixed && <button type="button" onClick={() => onChange(value.filter((_, n) => n !== i))}>移除 {path} {i + 1}</button>}</div>;
    })}{!fixed && <button type="button" onClick={() => onChange([...value, path.startsWith('背景') ? '' : itemTemplate(field)])}>添加{labels[field] ?? field}</button>}</fieldset>;
  }
  if (value !== null && typeof value === 'object') {
    const record = value as Record<string, unknown>;
    const contents = Object.entries(record).map(([key, v]) => key === 'id' ? <small key={key} className="muted">条目ID：{String(v)}</small> : <Field key={key} field={key} path={`${path} · ${labels[key] ?? key}`} value={v} disabled={disabled || (path.startsWith('豁免') && key === 'ability')} onChange={next => onChange({ ...record, [key]: next })} />);
    if (field === 'source') return <details className="sheet-source"><summary>{path}</summary>{contents}</details>;
    return <fieldset disabled={disabled} className="sheet-object"><legend>{path}</legend>{contents}</fieldset>;
  }
  if (typeof value === 'boolean') return <label className="check-label"><input type="checkbox" checked={value} disabled={disabled} onChange={e => onChange(e.target.checked)} />{path}</label>;
  let choices = options[field];
  if (field === 'kind' && path.startsWith('资源')) choices = ['HIT_DICE', 'SPELL_SLOT', 'PACT_SLOT', 'FEATURE', 'CUSTOM'];
  if (field === 'kind' && path.startsWith('专长')) choices = ['ORIGIN_FEAT', 'SPECIES', 'CLASS', 'FEAT', 'FEATURE', 'CUSTOM'];
  if (field === 'proficiency' && path.startsWith('豁免')) choices = ['NONE', 'PROFICIENT'];
  if (choices) return <label>{path}<select value={String(value)} disabled={disabled} onChange={e => onChange(e.target.value)}>{choices.map(v => <option key={v} value={v}>{optionNames[v] ?? v}</option>)}</select></label>;
  if (typeof value === 'number' || field === 'remainingRounds') return <label>{path}<input type="number" step="1" required={field !== 'remainingRounds'} disabled={disabled} value={value === null ? '' : String(value)} onChange={e => onChange(e.target.value === '' && field === 'remainingRounds' ? null : Number(e.target.value))} /></label>;
  if (['description', 'notes', 'detail', 'choices'].includes(field)) return <label>{path}<textarea value={String(value ?? '')} disabled={disabled} onChange={e => onChange(e.target.value)} /></label>;
  return <label>{path}<input value={String(value ?? '')} required={path === '角色名称'} disabled={disabled} maxLength={path === '角色名称' ? 100 : undefined} onChange={e => onChange(field === 'expiresAt' && e.target.value === '' ? null : e.target.value)} /></label>;
}
