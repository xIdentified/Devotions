package me.xidentified.devotions.util;

import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class JavaScriptEngine {
    private static final ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine();
    public static boolean evaluateExpression(String expression) {
        try {
            Object result = engine.eval(expression);
            return (result instanceof Boolean) && (Boolean) result;
        } catch (ScriptException e) {
            e.printStackTrace();
            return false;
        }
    }
}
