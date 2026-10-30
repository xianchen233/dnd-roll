import type {StabilizationCard} from './stabilization';
export type InstabilityHealingReview={expectedVersion:number;originVersion:number;targetVersion:number;attemptId:string;amount:number;reviewed:boolean};
export type InstabilityHealingApply=InstabilityHealingReview&{requestId:string;planHash:string;reason:string};
export type InstabilityHealingPlan={id:string;version:number;originVersion:number;damageVersion:number;targetVersion:number;attemptId:string;hash:string;effect:{profile:string;status:'CANDIDATE'|'MANUAL_REQUIRED';reasons:string[];hp:number|null;tempHp:number|null;healed:number|null;deathSuccesses:number|null;deathFailures:number|null;removed:string[]}};
export type InstabilityHealingEntry={id:string;version:number;originVersion:number;damageVersion:number;attemptId:string;actor:string;at:string;reason:string;review:InstabilityHealingReview;planHash:string;before:StabilizationCard;after:StabilizationCard};
