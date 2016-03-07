/*
 * Copyright 2016 yihtserns.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.yihtserns.groovy.deco;

import groovy.lang.Closure;
import groovy.lang.MetaMethod;
import java.util.List;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * Since I can't set any property to a {@code Closure}, I use this wrapper class to do it.
 *
 * @author yihtserns
 */
public class Function extends Closure {

    protected Closure delegate;
    private String methodName;
    private Class<?> returnType;

    private Function(Closure delegate, String methodName, Class<?> returnType) {
        super(null);
        this.delegate = delegate;
        this.methodName = methodName;
        this.returnType = returnType;
        setResolveStrategy(TO_SELF);
    }

    public Object doCall(List<Object> args) {
        return delegate.call(args.toArray(new Object[args.size()]));
    }

    /**
     * @return method name
     */
    public String getName() {
        return methodName;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    @Override
    public Object getProperty(String property) {
        if ("name".equals(property)) {
            return getName();
        }
        if ("returnType".equals(property)) {
            return getReturnType();
        }
        return super.getProperty(property);
    }

    public Closure decorateWith(Closure<Closure> decorator) {
        return decorateWith(null, decorator);
    }

    public Closure decorateWith(Object baggage, Closure<Closure> decorator) {
        Closure decorated = decorator.getMaximumNumberOfParameters() == 1
                ? decorator.call(this)
                : decorator.call(this, baggage);

        return new DecoratedFunction(decorated, methodName, returnType);
    }

    public static Function create(Object instance, String methodName, Class<?> returnType, Class[] parameterTypes) {
        MetaMethod method = InvokerHelper.getMetaClass(instance).pickMethod(
                methodName,
                parameterTypes);

        return create(new MetaMethodClosure(method, instance), methodName, returnType);
    }

    public static Function create(Closure delegate, String methodName, Class<?> returnType) {
        return new Function(delegate, methodName, returnType);
    }

    private static final class MetaMethodClosure extends Closure {

        private MetaMethod method;
        private Object instance;

        public MetaMethodClosure(MetaMethod method, Object instance) {
            super(null);
            this.method = method;
            this.instance = instance;
        }

        public Object doCall(Object[] args) {
            return method.doMethodInvoke(instance, args);
        }
    }

    private static final class DecoratedFunction extends Function {

        public DecoratedFunction(Closure delegate, String methodName, Class<?> returnType) {
            super(delegate, methodName, returnType);
        }

        @Override
        public Object doCall(List<Object> args) {
            return delegate.call(args);
        }
    }
}
