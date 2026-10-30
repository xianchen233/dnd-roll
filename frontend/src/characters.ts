export type Source = { book: string; revision: string; rules: '5R' | '2014' | 'CUSTOM'; detail: string };
export type Entry = { id: string; name: string; description: string; source: Source };
export type Check = { id: string; name: string; ability: string; proficiency: string; bonus: number; source: Source };
export type Resource = { id: string; name: string; kind: string; current: number; max: number; recovery: string; source: Source };
export type Condition = Entry & { icon: string; remainingRounds: number | null; expiresAt: string | null };
export type Sheet = {
  name: string; avatar: string; species: Entry;
  background: { entry: Entry; increases: Record<string, number>; originFeat: string; proficiencies: string[]; equipment: string[] };
  classes: { id: string; name: string; subclass: string; level: number; source: Source }[];
  abilities: Record<string, number>; skills: Check[]; saves: Check[];
  combat: { maxHp: number; hp: number; tempHp: number; speed: number; deathSuccesses: number; deathFailures: number; acMode: string; manualAc: number; source: Source };
  attacks: { id: string; name: string; ability: string; proficient: boolean; hitBonus: number; damageBonus: number; dice: string; damageType: string; mastery: string; source: Source }[];
  casting: { id: string; name: string; ability: string; attackBonus: number; dcBonus: number; source: Source }[];
  spells: { id: string; name: string; level: number; prepared: boolean; castingId: string; description: string; source: Source }[];
  features: { id: string; name: string; kind: string; choices: string; description: string; source: Source }[];
  resources: Resource[]; equipment: { id: string; name: string; quantity: number; equipped: boolean; description: string; source: Source }[];
  money: { cp: number; sp: number; ep: number; gp: number; pp: number }; conditions: Condition[];
  modifiers: { target: string; amount: number; source: Source }[]; overrides: { target: string; value: number; reason: string }[]; notes: string;
};
export type Calculation = { baseValue: number; value: number; components: { label: string; value: number; source: Source }[]; overrideReason: string };
export type CharacterSummary = { id: string; name: string; avatar: string; version: number; canEdit: boolean; canReadFull: boolean; health: string; ac: number; speed: number; conditions: string[] };
export type CharacterList = { campaignVersion: number; characters: CharacterSummary[] };
export type CharacterDetail = { id: string; version: number; canEdit: boolean; canManage: boolean; sheet: Sheet; calculations: Record<string, Calculation>; expiredConditionIds: string[] };
export type CharacterAccess = { version: number; hidden: boolean; shareFull: boolean; importReview: boolean; controllers: string[] };
export type CharacterAck = { id: string; version: number };
export type CharacterDocument = { format: 'dnd-roll.character'; formatVersion: 1; rules: '5R'; sheet: Sheet };
export type CharacterHistory = { version: number; actor: string; kind: string; reason: string; at: string; before: unknown; after: unknown };
export const abilities = ['STR', 'DEX', 'CON', 'INT', 'WIS', 'CHA'];
export const skillAbilities: Record<string, string> = { acrobatics: 'DEX', animalHandling: 'WIS', arcana: 'INT', athletics: 'STR', deception: 'CHA', history: 'INT', insight: 'WIS', intimidation: 'CHA', investigation: 'INT', medicine: 'WIS', nature: 'INT', perception: 'WIS', performance: 'CHA', persuasion: 'CHA', religion: 'INT', sleightOfHand: 'DEX', stealth: 'DEX', survival: 'WIS' };
export const skillNames: Record<string, string> = { acrobatics: '特技', animalHandling: '驯兽', arcana: '奥秘', athletics: '运动', deception: '欺瞒', history: '历史', insight: '洞悉', intimidation: '威吓', investigation: '调查', medicine: '医药', nature: '自然', perception: '察觉', performance: '表演', persuasion: '游说', religion: '宗教', sleightOfHand: '巧手', stealth: '隐匿', survival: '求生' };
export const source = (): Source => ({ book: '手动填写', revision: '1', rules: 'CUSTOM', detail: '' });
const entry = (id: string): Entry => ({ id, name: '', description: '', source: source() });
export function emptySheet(name = ''): Sheet {
  return { name, avatar: '', species: entry('species'), background: { entry: entry('background'), increases: Object.fromEntries(abilities.map(a => [a, 0])), originFeat: '', proficiencies: [], equipment: [] }, classes: [{ id: 'class-1', name: '冒险者', subclass: '', level: 1, source: source() }], abilities: Object.fromEntries(abilities.map(a => [a, 10])),
    skills: Object.entries(skillAbilities).map(([id, ability]) => ({ id, name: skillNames[id], ability, proficiency: 'NONE', bonus: 0, source: source() })), saves: abilities.map(id => ({ id, name: id, ability: id, proficiency: 'NONE', bonus: 0, source: source() })),
    combat: { maxHp: 1, hp: 1, tempHp: 0, speed: 30, deathSuccesses: 0, deathFailures: 0, acMode: 'UNARMORED', manualAc: 10, source: source() }, attacks: [], casting: [], spells: [], features: [], resources: [], equipment: [], money: { cp: 0, sp: 0, ep: 0, gp: 0, pp: 0 }, conditions: [], modifiers: [], overrides: [], notes: '' };
}
export function itemTemplate(key: string): unknown {
  const id = crypto.randomUUID(), s = source();
  return ({ classes: { id, name: '新职业', subclass: '', level: 1, source: s }, attacks: { id, name: '新攻击', ability: 'STR', proficient: false, hitBonus: 0, damageBonus: 0, dice: '1d6', damageType: '', mastery: '', source: s }, casting: { id, name: '新施法来源', ability: 'INT', attackBonus: 0, dcBonus: 0, source: s }, spells: { id, name: '新法术', level: 0, prepared: false, castingId: '', description: '', source: s }, features: { id, name: '新能力', kind: 'FEATURE', choices: '', description: '', source: s }, resources: { id, name: '新资源', kind: 'CUSTOM', current: 0, max: 1, recovery: '', source: s }, equipment: { id, name: '新物品', quantity: 1, equipped: false, description: '', source: s }, conditions: { id, name: '新状态', icon: '◇', description: '', source: s, remainingRounds: null, expiresAt: null }, modifiers: { target: 'initiative', amount: 0, source: s }, overrides: { target: 'initiative', value: 0, reason: '' } } as Record<string, unknown>)[key] ?? '';
}
