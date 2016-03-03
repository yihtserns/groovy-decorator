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

    private MetaMethod method;

    public Function(MetaMethod method) {
        super(null);
        this.method = method;
    }

    public Object doCall(Object instance, Object... args) {
        return method.doMethodInvoke(instance, args);
    }

    /**
     * @return method name
     */
    public String getName() {
        return method.getName();
    }

    @Override
    public Object getProperty(String property) {
        if ("name".equals(property)) {
            return getName();
        }
        return super.getProperty(property);
    }

    public static Function create(Class clazz, String methodName, List<Class> parameterTypes) {
        MetaMethod method = InvokerHelper.getMetaClass(clazz).pickMethod(
                methodName,
                parameterTypes.toArray(new Class[parameterTypes.size()]));

        return new Function(method);
    }
}
