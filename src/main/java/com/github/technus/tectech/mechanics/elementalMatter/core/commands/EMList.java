package com.github.technus.tectech.mechanics.elementalMatter.core.commands;

import com.github.technus.tectech.mechanics.elementalMatter.core.definitions.EMPrimitiveTemplate;
import com.github.technus.tectech.mechanics.elementalMatter.core.definitions.IEMDefinition;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by danie_000 on 30.12.2017.
 */
public class EMList implements ICommand {
    ArrayList<String> aliases=new ArrayList<>();

    public EMList(){
        aliases.add("em_list");
        aliases.add("list_em");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!sender.getEntityWorld().isRemote) {
            if(args.length == 0) {
                sender.addChatMessage(new ChatComponentText("    Available Classes: tag - name"));
                Map<Byte,Method> binds= IEMDefinition.getBindsComplex();
                for (Map.Entry<Byte,Method> e:binds.entrySet()) {
                    sender.addChatMessage(new ChatComponentText((char) e.getKey().byteValue() +" - "+e.getValue().getReturnType().getSimpleName()));
                }
            }else if(args.length==1){
                sender.addChatMessage(new ChatComponentText("    Available Primitives: symbol - name"));
                if(args[0].equals(String.valueOf((char) EMPrimitiveTemplate.nbtType))){
                    Map<Integer, EMPrimitiveTemplate> bindsBO = IEMDefinition.getBindsPrimitive();
                    for (Map.Entry<Integer, EMPrimitiveTemplate> e:bindsBO.entrySet()) {
                        sender.addChatMessage(new ChatComponentText(e.getKey() + " - "+e.getValue().getLocalizedName()));
                    }
                }else{
                    sender.addChatMessage(new ChatComponentText("Complex definition - needs contents"));
                }
            }else{
                sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
            }
        }
    }

    @Override
    public boolean isUsernameIndex(String[] p_82358_1_, int p_82358_2_) {
        return false;
    }

    @Override
    public List<String> getCommandAliases() {
        return aliases;
    }

    @Override
    public String getCommandName() {
        return aliases.get(0);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if(args.length==0){
            Map<Byte,Method> binds= IEMDefinition.getBindsComplex();
            ArrayList<String> strings=new ArrayList<>(binds.size());
            for (Map.Entry<Byte,Method> e:binds.entrySet()) {
                strings.add(String.valueOf((char)e.getKey().byteValue())+' '+e.getValue().getReturnType().getSimpleName());
            }
            return strings;
        }
        return null;
    }

    @Override
    public String getCommandUsage(ICommandSender p_71518_1_) {
        return "em_list (optional class tag)";
    }

    @Override
    public int compareTo(Object o) {
        if(o instanceof ICommand){
            return getCommandName().compareTo(((ICommand) o).getCommandName());
        }
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
