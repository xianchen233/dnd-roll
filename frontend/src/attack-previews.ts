import type {Sheet,Source} from './characters';
import type {CombatChange} from './combat';
import type {Roll} from './dice';
export type AttackFactor={reason:string;source:Omit<Source,'rules'>&{rules:string}};
export type AttackPreviewCreate={requestId:string;characterId:string;sourceVersion:number;targetId:string;targetVersion:number;attackId:string;reviewed:boolean;exceptions:AttackFactor[];advantage:AttackFactor[];disadvantage:AttackFactor[];immunities:AttackFactor[];resistances:AttackFactor[];vulnerabilities:AttackFactor[]};
export type AttackPreviewCommand={requestId:string;expectedVersion:number;kind:'CONFIRM'|'CONFIRM_DAMAGE'|'APPLY'|'CORRECT'|'PUBLISH';previewHash:string;changes:CombatChange[];correctionOf:string;reason:string;summary:string};
export type DamageEffect={profile:'5R-SRD-5.2.1-DAMAGE-HP-V1';status:'CANDIDATE'|'MANUAL_REQUIRED';reasons:string[];hp:number|null;tempHp:number|null;absorbed:number|null;hpLoss:number|null};
export type AmmunitionReview={expectedVersion:number;previewHash:string;resourceId:string;reviewed:boolean};
export type AmmunitionConfirmation=AmmunitionReview&{requestId:string;reason:string};
export type AmmunitionEffect={profile:'5R-SRD-5.2.1-SHORTBOW-AMMO-V1';status:'CANDIDATE'|'MANUAL_REQUIRED';reasons:string[];resourceId:string;before:number|null;after:number|null;hp:number|null;tempHp:number|null};
export type AttackCard={id:string;version:number;sheet:Sheet};
export type AttackPreviewEvent={id:string;kind:string;actor:string;at:string;changes:CombatChange[];correctionOf:string;reason:string;summary:string;combatEventId:string};
export type AttackPreview={id:string;version:number;status:'MANUAL_REQUIRED'|'PREVIEW'|'CONFIRMED'|'APPLIED';actor:string;at:string;hash:string;events:AttackPreviewEvent[];evidence:{review:AttackPreviewCreate;source:AttackCard;target:AttackCard;entry:unknown;manualReasons:string[];attackRoll:Roll|null;damageRoll:Roll|null;actionId:string|null;resolution:{review:{candidate:{verdict:string;natural:number;total:number;damageCount:number;damageSides:number}};dice:number[];rawDamage:number;damage:number;explanation:{rule:string;calculation:string;source:Source}[]}|null}};
export type AttackPublicResult={id:string;name:string;targetName:string;summary:string;actor:string;at:string};

export type MonsterDeathReview={expectedVersion:number;previewHash:string;treatment:'MONSTER_DIES_AT_ZERO'|'MANUAL';reviewed:boolean};
export type MonsterDeathConfirmation=MonsterDeathReview&{requestId:string;reason:string};
export type MonsterDeathEffect=Omit<DamageEffect,'profile'>&{profile:'5R-SRD-5.2.1-MONSTER-DEATH-V1'};

export type KnockoutReview={expectedVersion:number;previewHash:string;treatment:'KNOCK_OUT_MELEE'|'MANUAL';reviewed:boolean};
export type KnockoutConfirmation=KnockoutReview&{requestId:string;reason:string};
export type KnockoutEffect=Omit<DamageEffect,'profile'>&{profile:'5R-SRD-5.2.1-KNOCKOUT-V1'};

export type DaggerKnockoutReview={expectedVersion:number;previewHash:string;treatment:'KNOCK_OUT_MELEE'|'THROWN'|'MANUAL';reviewed:boolean};
export type DaggerKnockoutConfirmation=DaggerKnockoutReview&{requestId:string;reason:string};
export type DaggerKnockoutEffect=Omit<DamageEffect,'profile'>&{profile:'5R-SRD-5.2.1-DAGGER-KNOCKOUT-V1'};

export type KnockoutHealingReview={expectedVersion:number;previewHash:string;targetVersion:number;amount:number;reviewed:boolean};
export type KnockoutHealingApply=KnockoutHealingReview&{requestId:string;planHash:string;reason:string};
export type KnockoutHealingPlan={profile:'5R-SRD-5.2.1-KNOCKOUT-HEALING-V1';status:'CANDIDATE'|'MANUAL_REQUIRED';reasons:string[];hp:number|null;tempHp:number|null;healed:number|null;conditionId:string|null;targetVersion:number;hash:string};

export type KnockoutRestReview={expectedVersion:number;previewHash:string;targetVersion:number;minutes:number;interrupted:boolean;reviewed:boolean};
export type KnockoutRestComplete=KnockoutRestReview&{requestId:string;planHash:string;reason:string};
export type KnockoutRestPlan={profile:'5R-SRD-5.2.1-KNOCKOUT-REST-V1';status:'CANDIDATE'|'MANUAL_REQUIRED';reasons:string[];hp:number|null;tempHp:number|null;conditionId:string|null;targetVersion:number;hash:string};

export type FirstAidAttempt={requestId:string;expectedVersion:number;previewHash:string;targetVersion:number;helperId:string;helperVersion:number;reviewed:boolean;reason:string};
export type FirstAidReview=Omit<FirstAidAttempt,'requestId'|'reason'>&{rollId:string};
export type FirstAidComplete=FirstAidReview&{requestId:string;planHash:string;reason:string};
export type FirstAidPlan={profile:'5R-SRD-5.2.1-KNOCKOUT-FIRST-AID-V1';status:'CANDIDATE'|'MANUAL_REQUIRED';reasons:string[];dc:10;total:number|null;hp:number|null;tempHp:number|null;conditionId:string|null;targetVersion:number;hash:string};
