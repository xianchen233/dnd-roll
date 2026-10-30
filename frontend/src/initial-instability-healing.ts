import type {StabilizationCard} from './stabilization';
export type InitialInstabilityHealingReview={expectedVersion:number;targetVersion:number;attemptId:string;amount:number;reviewed:boolean};
export type InitialInstabilityHealingApply=InitialInstabilityHealingReview&{requestId:string;planHash:string;reason:string};
export type InitialInstabilityHealingPlan={id:string;version:number;damageVersion:number;targetVersion:number;attemptId:string;hash:string;effect:{profile:string;status:'CANDIDATE'|'MANUAL_REQUIRED';reasons:string[];hp:number|null;tempHp:number|null;healed:number|null;deathSuccesses:number|null;deathFailures:number|null;removed:string[]}};
export type InitialInstabilityHealingEntry={id:string;version:number;damageVersion:number;attemptId:string;actor:string;at:string;reason:string;review:InitialInstabilityHealingReview;planHash:string;before:StabilizationCard;after:StabilizationCard};
