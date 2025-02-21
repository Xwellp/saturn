package xwellp.saturn.modules.instructions;

import xwellp.saturn.modules.instructions.instructions.BaseInstruction;
import xwellp.saturn.modules.instructions.instructions.CommandInstruction;
import xwellp.saturn.modules.instructions.instructions.DelayInstruction;

import java.util.HashMap;
import java.util.Map;

public class InstructionFactory {
    private final Map<String, Factory> factories;

    public InstructionFactory() {
        factories = new HashMap<>();
        factories.put(CommandInstruction.type, CommandInstruction::new);
        factories.put(DelayInstruction.type, DelayInstruction::new);
    }

    public BaseInstruction createInstruction(String name) {
        if (factories.containsKey(name)) return factories.get(name).create();
        return null;
    }

    private interface Factory {
        BaseInstruction create();
    }
}
