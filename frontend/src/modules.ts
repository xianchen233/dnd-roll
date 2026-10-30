export type ModuleDependency={id:string;version:string;rules:string};
export type ModuleManifest={format:string;formatVersion:number;moduleId:string;moduleVersion:string;name:string;rules:string;rights:string;dependencies:ModuleDependency[];content:{size:number;sha256:string};assets:{id:string;mediaType:string;size:number;sha256:string}[]};
export type ModuleSummary={id:string;manifest:ModuleManifest;sha256:string;missingDependencies:ModuleDependency[]};
export type ModuleListing={campaignVersion:number;packages:ModuleSummary[]};
export type ModulePreview={document?:ModuleDefinition;manifest:ModuleManifest;sha256:string;nodes:{id:string;kind:string;name:string;text:string;visibility:string;nodeIds:string[];assetIds:string[];dependencyIds:string[]}[];missingDependencies:ModuleDependency[];existingId:string|null;requiresLegacyConfirmation:boolean};
export type ModuleCommand={requestId:string;expectedVersion:number;createCopy:boolean;confirmLegacy:boolean};
export type ModuleNode=ModulePreview['nodes'][number];
export type ModulePortableStep=
 | {kind:'REVEAL_TOKEN';placementId:string}
 | {kind:'REVEAL_CHARACTER';unitId:string}
 | {kind:'PUBLISH_TEXT'|'PUBLISH_MATERIAL';documentId:string}
 | {kind:'PREPARE_ENCOUNTER';participants:{unitId:string;kind:string;group:string;surprised:boolean;advantage:string[];disadvantage:string[]}[];reason:string}
 | {kind:'START_ENCOUNTER';reason:string};
export type ModulePortableBlock={formatVersion:1;definitions:{nodeId:string;steps:ModulePortableStep[]}[]};
export type ModuleDefinition={nodes:ModuleNode[];play:{scenes:{nodeId:string;width:number;height:number;gridSize:number;map:{assetId:string;gridSize:number;offsetX:number;offsetY:number;distancePerCell:number}|null}[];units:{nodeId:string;sheet:import('./characters').Sheet;hidden:boolean;shareFull:boolean}[];placements:{id:string;sceneId:string;unitId:string;x:number;y:number;hidden:boolean}[];admissionMode:string;approvedDependencyIds:string[]};operations?:ModulePortableBlock};
export type CustomizationSummary={id:string;name:string;version:number;sourcePackageId:string};
export type CustomizationListing={campaignVersion:number;customizations:CustomizationSummary[]};
export type Customization=CustomizationSummary&{sourceSha256:string;baseline:ModuleDefinition;document:ModuleDefinition;missingDependencies:ModuleDependency[]};
export type ModuleInstance={version:number;source:{campaignId:string;packageId:string;sha256:string;manifest:ModuleManifest;customizationId:string;customizationVersion:number};document:ModuleDefinition;mapping:Record<string,Record<string,string>>;progress:Record<string,string>};
export type LifecycleCommand={requestId:string;expectedVersion:number};
export type ModuleUpdatePreviewResult={version:number;packageId:string;sha256:string;changes:{path:string;baseline:unknown;current:unknown;incoming:unknown;conflict:boolean}[];missingDependencies:ModuleDependency[]};
export type ModuleUpdateSelection={path:string;choice:'KEEP'|'INCOMING'};
export type ModuleUpdateImpactRequest={expectedVersion:number;packageId:string;selections:ModuleUpdateSelection[]};
export type ModuleUpdateApply=ModuleUpdateImpactRequest&{requestId:string;sha256:string;impactHash:string};
export type ModuleUpdateImpact=ModuleUpdatePreviewResult&{impactHash:string;document:ModuleDefinition;manifest:ModuleManifest};
export type ModuleUpdateEvidence={version:number;actor:string;requestId:string;packageId:string;sha256:string;impactHash:string;selections:ModuleUpdateSelection[];before:Omit<Customization,'missingDependencies'>};

export type ModuleLiveReview={expectedVersion:number;customizationVersion:number};
export type ModuleLiveApply=ModuleLiveReview&{requestId:string;impactHash:string};
export type ModuleLiveImpact={version:number;customizationVersion:number;impactHash:string;changes:ModuleUpdatePreviewResult['changes'];blockers:string[]};
export type ModuleLiveEvidence={version:number;actor:string;requestId:string;impactHash:string;before:ModuleInstance};
export type ModuleMigrationApply=ModuleLiveApply&{confirmations:string[]};
export type ModuleMigrationImpact={version:number;customizationVersion:number;impactHash:string;confirmations:string[];blockers:string[];changes:ModuleUpdatePreviewResult['changes'];runtime:Record<string,unknown>};
export type ModuleMigrationEvidence=ModuleLiveEvidence&{recoveryCampaignId:string;runtime:Record<string,unknown>};

export type ModuleOperationStep =
  | {kind:'REVEAL_TOKEN';sceneId:string;tokenId:string;expectedVersion:number}
  | {kind:'REVEAL_CHARACTER';characterId:string;expectedVersion:number}
  | {kind:'PUBLISH_TEXT';noteId:string;expectedVersion:number}
  | {kind:'PUBLISH_MATERIAL';noteId:string;expectedVersion:number}
  | {kind:'PREPARE_ENCOUNTER';expectedVersion:number;participants:import('./dice').InitiativeGenerate['participants'];reason:string}
  | {kind:'START_ENCOUNTER';expectedVersion:number;initiativeVersion:number;reason:string};
export type ModuleOperation={id:string;version:number;name:string;steps:ModuleOperationStep[]};
export type ModuleOperationSave=Omit<ModuleOperation,'id'|'version'>&{requestId:string;expectedVersion:number};
export type ModuleOperationReview={expectedVersion:number;campaignVersion:number};
export type ModuleOperationExecute=ModuleOperationReview&{requestId:string;impactHash:string};
export type ModuleOperationImpact={id:string;version:number;campaignVersion:number;impactHash:string;definition:ModuleOperation;targets:unknown[]};
export type ModuleOperationExecution={id:string;requestId:string;actor:string;at:string;impact:ModuleOperationImpact;results:unknown[]};
export type ModuleBinding={nodeId:string;operationId:string;version:number;name:string;source:{nodeId:string;steps:ModulePortableStep[]};boundVersion:number};
export type ModuleBindingListing={instanceVersion:number;campaignVersion:number;bindings:{binding:ModuleBinding;operation:ModuleOperation|null;locallyModified:boolean}[]};
export type ModuleBindingReview={expectedVersion:number;operationVersion:number;instanceVersion:number;campaignVersion:number};
export type ModuleBindingPreview={binding:ModuleBinding;operationVersion:number;instanceVersion:number;campaignVersion:number;candidate:{requestId:string;expectedVersion:number;name:string;steps:ModuleOperationStep[]};targets:unknown[];impactHash:string};
export type ModuleBind=ModuleBindingReview&{requestId:string;impactHash:string;reason:string};
export type ModuleBindingAck={nodeId:string;operationId:string;version:number;operationVersion:number};
export type ModuleOperationChoice={nodeId:string;choice:'KEEP'|'INCOMING'|'RETIRE';bindingVersion:number;operationVersion:number};
export type ModuleOperationOptions={version:number;customizationVersion:number;campaignVersion:number;options:{nodeId:string;binding:ModuleBinding|null;operation:ModuleOperation|null;incoming:ModuleBinding['source']|null;incomingName:string;retired:boolean}[]};
export type ModuleOperationMigrationReview=ModuleLiveReview&{campaignVersion:number;selections:ModuleOperationChoice[]};
export type ModuleOperationMigrationImpact={migration:ModuleMigrationImpact;document:ModuleDefinition;selections:ModuleOperationChoice[]};
export type ModuleOperationMigrationApply=ModuleOperationMigrationReview&{requestId:string;impactHash:string;confirmations:string[]};
