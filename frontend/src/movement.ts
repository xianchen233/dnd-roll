import type {Token} from './types';
export type MovementStep={x:number;y:number;mode:'WALK'|'CRAWL'|'CLIMB'|'SWIM'|'MANUAL';difficult:boolean};
export type MovementView={token:Token;sceneVersion:number;characterId:string;characterVersion:number;budgetVersion:string;turnKey:number;tracked:boolean;allowance:number;used:number;complete:boolean;history:MovementJournal[]};
export type MovementPath={requestId:string;expectedVersion:number;sceneVersion:number;characterVersion:number;budgetVersion:string;turnKey:number;reviewed:boolean;path:MovementStep[];override:boolean;cost:number;reason:string};
export type MovementAdjustment=Omit<MovementPath,'reviewed'|'path'|'override'|'cost'>&{used:number;allowance:number};
export type MovementPreview={cost:number;usedAfter:number;remaining:number;overBudget:boolean;complete:boolean;tracked:boolean};
export type MovementResult={token:Token;calculation:MovementPreview};
export type MovementRead=MovementView|{available:false};
export type MovementJournal={id:string;kind:'PATH'|'ADJUST';actor:string;at:string;tokenId:string;command:MovementPath|MovementAdjustment;before:Omit<MovementView,'history'>&{history:[]};calculation:MovementPreview;source:{book:string;revision:string;rules:string;detail:string}};
