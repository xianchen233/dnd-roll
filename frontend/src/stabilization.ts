import type { Sheet,Condition } from './characters';
import type { Roll } from './dice';
export type StabilizationCard={id:string;version:number;sheet:Sheet};
export type StabilizationAttempt={id:string;status:'ROLLED'|'APPLIED'|'HEALED'|'UNSTABLE'|'RESTABILIZED'|'RESTABILIZATION_UNSTABLE';target:StabilizationCard;helper:StabilizationCard;roll:Roll;reviewed:boolean;reason:string;actor:string;at:string;completion:{actor:string;reason:string;at:string}|null};
export type StabilizationState={id:string;version:number;latest:StabilizationAttempt|null};
export type StabilizationReview={expectedVersion:number;targetVersion:number;helperId:string;helperVersion:number;rollId:string;reviewed:boolean};
export type StabilizationRollInput=Omit<StabilizationReview,'rollId'>&{requestId:string;reason:string};
export type StabilizationApply=StabilizationReview&{requestId:string;reason:string;planHash:string};
export type StabilizationPlan={id:string;version:number;targetVersion:number;helperVersion:number;rollId:string;hash:string;effect:{profile:string;status:'CANDIDATE'|'MANUAL_REQUIRED';reasons:string[];dc:number;total:number|null;hp:number|null;tempHp:number|null;deathSuccesses:number|null;deathFailures:number|null;annotations:Condition[]}};
