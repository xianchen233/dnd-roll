export type ModuleDependency={id:string;version:string;rules:string};
export type ModuleManifest={format:string;formatVersion:number;moduleId:string;moduleVersion:string;name:string;rules:string;rights:string;dependencies:ModuleDependency[];content:{size:number;sha256:string};assets:{id:string;mediaType:string;size:number;sha256:string}[]};
export type ModuleSummary={id:string;manifest:ModuleManifest;sha256:string;missingDependencies:ModuleDependency[]};
export type ModuleListing={campaignVersion:number;packages:ModuleSummary[]};
export type ModulePreview={manifest:ModuleManifest;sha256:string;nodes:{id:string;kind:string;name:string;text:string;visibility:string;nodeIds:string[];assetIds:string[];dependencyIds:string[]}[];missingDependencies:ModuleDependency[];existingId:string|null;requiresLegacyConfirmation:boolean};
export type ModuleCommand={requestId:string;expectedVersion:number;createCopy:boolean;confirmLegacy:boolean};
