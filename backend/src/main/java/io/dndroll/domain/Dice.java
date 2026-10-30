package io.dndroll.domain;

import java.util.*;

/** Bounded notation, never eval. The application supplies server-owned entropy. */
public final class Dice {
    @FunctionalInterface public interface Entropy { int die(int sides); }
    public record Die(int sides,int value,int sign,boolean kept) {}
    public record Result(String expression,String mode,List<Die> dice,int constant,int total) {}
    private record Term(int sign,int count,int sides,int constant) {}
    private static List<Term> parse(String expression){
        CharacterSheet.text(expression,100,true);
        if(expression.chars().anyMatch(c->Character.isWhitespace(c)&&c!=' '))throw invalid();
        String text=expression.replace(" ","").toLowerCase(Locale.ROOT);var terms=new ArrayList<Term>();
        var matcher=java.util.regex.Pattern.compile("([+-]?)([0-9]*d[0-9]+|[0-9]+)").matcher(text);int end=0,dice=0;
        try{
            while(matcher.find()){
                if(matcher.start()!=end||end>0&&matcher.group(1).isEmpty()||terms.size()==20)throw invalid();
                int sign=matcher.group(1).equals("-")?-1:1;String value=matcher.group(2);int d=value.indexOf('d');
                if(d>=0){int count=d==0?1:Integer.parseInt(value.substring(0,d)),sides=Integer.parseInt(value.substring(d+1));
                    if(count<1||count>100||sides<2||sides>1000||(dice+=count)>100)throw invalid();terms.add(new Term(sign,count,sides,0));
                }else{int n=Integer.parseInt(value);if(n>10000)throw invalid();terms.add(new Term(sign,0,0,n));}
                end=matcher.end();
            }
        }catch(NumberFormatException e){throw invalid();}
        if(end!=text.length()||terms.isEmpty())throw invalid();return terms;
    }
    private static String notation(List<Term> terms){var out=new StringBuilder();for(var t:terms){if(t.sign()<0)out.append('-');else if(!out.isEmpty())out.append('+');out.append(t.count()>0?t.count()+"d"+t.sides():Integer.toString(t.constant()));}return out.toString();}
    public static String normalize(String expression){return notation(parse(expression));}
    public static void sources(List<String> values){if(values==null||values.size()>10)throw invalid();values.forEach(s->CharacterSheet.text(s,200,true));}
    public static void validate(String expression,List<String> advantage,List<String> disadvantage){
        var terms=parse(expression);sources(advantage);sources(disadvantage);
        if(!advantage.isEmpty()||!disadvantage.isEmpty()){
            var dice=terms.stream().filter(t->t.count()>0).toList();
            if(dice.size()!=1||dice.getFirst().count()!=1||dice.getFirst().sides()!=20||dice.getFirst().sign()!=1)throw invalid();
        }
    }
    public static Result roll(String expression,List<String> advantage,List<String> disadvantage,Entropy entropy){
        validate(expression,advantage,disadvantage);var terms=parse(expression);var dice=new ArrayList<Die>();int constant=0,total=0;
        String mode=advantage.isEmpty()==disadvantage.isEmpty()?"NORMAL":advantage.isEmpty()?"DISADVANTAGE":"ADVANTAGE";
        for(var t:terms){
            if(t.count()==0){constant+=t.sign()*t.constant();continue;}
            if(!mode.equals("NORMAL")){
                int a=draw(entropy,20),b=draw(entropy,20);boolean first=mode.equals("ADVANTAGE")?a>=b:a<=b;
                dice.add(new Die(20,a,1,first));dice.add(new Die(20,b,1,!first));total+=first?a:b;
            }else for(int i=0;i<t.count();i++){int n=draw(entropy,t.sides());dice.add(new Die(t.sides(),n,t.sign(),true));total+=t.sign()*n;}
        }
        return new Result(notation(terms),mode,List.copyOf(dice),constant,total+constant);
    }
    private static int draw(Entropy entropy,int sides){int n=entropy.die(sides);if(n<1||n>sides)throw new IllegalStateException("Invalid server entropy");return n;}
    private static DomainException invalid(){return new DomainException("INVALID_INPUT",400,"Invalid or unsupported dice notation");}
}
