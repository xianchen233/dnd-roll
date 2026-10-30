package io.dndroll.modules;

import io.dndroll.domain.AccountPolicy;
import java.util.*;

/** Explicit choices change accepted sources; they never manufacture executable versions. */
public final class ModuleOperationSelection {
    public record Choice(String nodeId,String choice,long bindingVersion,long operationVersion) {}
    public record Option(String nodeId,ModuleBindings.Binding binding,ModuleOperations.Definition operation,ModulePortable.Definition incoming,String incomingName,boolean retired) {}
    public static List<ModulePortable.Definition> definitions(ModuleBlueprint.Document d){return d.operations()==null?List.of():d.operations().definitions();}
    public static ModuleBlueprint.Document select(ModuleBlueprint.Document before,ModuleBlueprint.Document incoming,List<Option> options,List<Choice> choices){
        if(choices==null||choices.size()>200||choices.size()!=options.size())throw ModuleBlueprint.invalid();
        var decisions=new HashMap<String,Choice>();for(var choice:choices){if(choice==null||choice.nodeId()==null||!Set.of("KEEP","INCOMING","RETIRE").contains(choice.choice())||decisions.put(choice.nodeId(),choice)!=null)throw ModuleBlueprint.invalid();}
        var nodes=new LinkedHashMap<String,ModuleArchive.Node>();incoming.nodes().forEach(n->nodes.put(n.id(),n));var definitions=new ArrayList<ModulePortable.Definition>();
        for(var option:options){var choice=decisions.remove(option.nodeId());if(choice==null)throw ModuleBlueprint.invalid();
            AccountPolicy.version(option.binding()==null?0:option.binding().version(),choice.bindingVersion());AccountPolicy.version(option.operation()==null?0:option.operation().version(),choice.operationVersion());
            if(choice.choice().equals("INCOMING")){if(option.incoming()==null)throw ModuleBlueprint.invalid();definitions.add(option.incoming());}
            else if(choice.choice().equals("KEEP")&&option.binding()!=null&&!option.retired()){
                definitions.add(option.binding().source());var node=before.nodes().stream().filter(n->n.id().equals(option.nodeId())).findFirst().orElseThrow(ModuleBlueprint::invalid);nodes.put(node.id(),node);
            }
        }
        if(!decisions.isEmpty())throw ModuleBlueprint.invalid();
        return new ModuleBlueprint.Document(List.copyOf(nodes.values()),incoming.play(),new ModulePortable.Block(1,List.copyOf(definitions)));
    }
}
