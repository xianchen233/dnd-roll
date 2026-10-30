import type {Sheet,CharacterDetail} from './characters';
import type {Roll} from './dice';
export type CombatEvent={id:string;actor:string;at:string;kind:string;verdict:string;correctionOf:string;reason?:string;roll?:Roll;privateOnly?:boolean;changes?:{before:{sheet:Sheet};after:{sheet:Sheet}}[]};
export type CombatAction={id:string;version?:number;mutationVersion?:number;characterId:string;name:string;targetId:string;targetName:string;attack:Sheet['attacks'][number];verdict:string;actor:string;at:string;events:CombatEvent[]};
export type CombatState={version?:number;endVersion?:number;status:string;round?:number;order:(string|{characterId:string;name:string})[];currentId:string;grants?:string[];reason?:string};
export type CombatChange={characterId:string;expectedVersion:number;hp:number;tempHp:number;resources:{id:string;current:number}[]};
export type CombatDeclaration={requestId:string;expectedVersion:number;characterId:string;targetId:string;attackId:string;visibility:string};
export type CombatCommand={requestId:string;expectedVersion:number;kind:string;rollId:string;verdict:string;changes:CombatChange[];correctionOf:string;reason:string};
export type TurnCommand={requestId:string;expectedVersion:number;kind:string;characterId:string;order:string[];reason:string};
export type TurnHistory={actor:string;kind:string;at:string;before:CombatState;after:CombatState};
export function changeFrom(detail:CharacterDetail):CombatChange{return {characterId:detail.id,expectedVersion:detail.version,hp:detail.sheet.combat.hp,tempHp:detail.sheet.combat.tempHp,resources:detail.sheet.resources.map(r=>({id:r.id,current:r.current}))};}
