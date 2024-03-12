package me.xidentified.devotions.util;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class JavaScriptEngine {

    public static boolean evaluateExpression(String expression) {
        Context rhinoContext = Context.enter();
        try {
            rhinoContext.setOptimizationLevel(-1); // Interpretive mode
            Scriptable scope = rhinoContext.initStandardObjects();
            Object result = rhinoContext.evaluateString(scope, expression, "JavaScript", 1, null);
            return Context.toBoolean(result);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            Context.exit();
        }
    }
}
