package shop.zailushang.spring.boot.framework;

import javax.script.ScriptEngine;
import java.util.function.Function;

@FunctionalInterface
public interface ScriptEngineCreator extends Function<ClassLoader, ScriptEngine> {
    @Override
    default ScriptEngine apply(ClassLoader classLoader) {
        return createScriptEngine(classLoader);
    }

    ScriptEngine createScriptEngine(ClassLoader classLoader);
}
