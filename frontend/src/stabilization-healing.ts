import type {StabilizationCard} from './stabilization';
export type StabilizationHealingReview={expectedVersion:number;targetVersion:number;attemptId:string;amount:number;reviewed:boolean};
export type StabilizationHealingApply=StabilizationHealingReview&{requestId:string;planHash:string;reason:string};
export type StabilizationHealingPlan={id:string;version:number;targetVersion:number;attemptId:string;hash:string;effect:{profile:string;status:'CANDIDATE'|'MANUAL_REQUIRED';reasons:string[];hp:number|null;tempHp:number|null;healed:number|null;deathSuccesses:number|null;deathFailures:number|null;removed:string[]}};
export type StabilizationHealingEntry={id:string;version:number;attemptId:string;actor:string;at:string;reason:string;review:StabilizationHealingReview;planHash:string;before:StabilizationCard;after:StabilizationCard};
