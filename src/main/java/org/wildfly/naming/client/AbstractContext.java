/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.naming.client;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.OperationNotSupportedException;

import org.wildfly.common.Assert;
import org.wildfly.naming.client._private.Messages;
import org.wildfly.naming.client.util.FastHashtable;

/**
 * An abstract {@code Context} implementation which does not support federation of naming systems (i.e. binding
 * contexts from other naming systems).  Such contexts can use names that do not follow the {@linkplain CompositeName composite name}
 * syntax constraints, and may perform better.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractContext implements Context, AutoCloseable {
    private static final NameParser DEFAULT_NAME_PARSER = SimpleName::new;

    private final FastHashtable<String, Object> environment;

    /**
     * Construct a new instance.  The given environment map is used directly; the caller is responsible for creating
     * a copy if a copy is needed.
     *
     * @param environment the environment map to use
     */
    protected AbstractContext(final FastHashtable<String, Object> environment) {
        Assert.checkNotNullParam("environment", environment);
        this.environment = environment;
    }

    /**
     * Construct a new instance.  A new, empty environment map is created for this context.
     */
    protected AbstractContext() {
        this(new FastHashtable<>());
    }

    /**
     * Get the native name parser for this context.  The name parser should always yield a compound name; returning a
     * {@linkplain CompositeName composite name} may cause undefined results or infinite loops or recursion.  The
     * default implementation returns a name parser for {@link SimpleName} instances but may be overridden as desired.
     *
     * @return the name parser
     */
    public NameParser getNativeNameParser() {
        return DEFAULT_NAME_PARSER;
    }

    public Object lookup(final String name) throws NamingException {
        Assert.checkNotNullParam("name", name);
        return lookupNative(getNativeNameParser().parse(name));
    }

    public Object lookup(final Name name) throws NamingException {
        Assert.checkNotNullParam("name", name);
        if (name instanceof CompositeName) {
            return lookupNative(decomposeName((CompositeName) name));
        } else {
            return lookupNative(name);
        }
    }

    /**
     * Look up a compound name within this naming system.  The given name is guaranteed not to be a
     * {@link CompositeName}.
     *
     * @param name the (compound) name (not {@code null})
     * @return the binding value
     * @throws NamingException if an error occurs
     */
    protected abstract Object lookupNative(Name name) throws NamingException;

    public Object lookupLink(final String name) throws NamingException {
        Assert.checkNotNullParam("name", name);
        return lookupLinkNative(getNativeNameParser().parse(name));
    }

    public Object lookupLink(final Name name) throws NamingException {
        Assert.checkNotNullParam("name", name);
        if (name instanceof CompositeName) {
            return lookupLinkNative(decomposeName((CompositeName) name));
        } else {
            return lookupLinkNative(name);
        }
    }

    protected abstract Object lookupLinkNative(Name name) throws NamingException;

    public void bind(final String name, final Object obj) throws NamingException {
        Assert.checkNotNullParam("name", name);
        // reuse empty check below
        bind(getNativeNameParser().parse(name), obj);
    }

    public void bind(final Name name, final Object obj) throws NamingException {
        Assert.checkNotNullParam("name", name);
        if (name.isEmpty()) {
            throw Messages.log.invalidEmptyName();
        }
        if (name instanceof CompositeName) {
            // re-check for empty
            bind(decomposeName((CompositeName) name), obj);
        } else {
            bindNative(name, obj);
        }
    }

    protected void bindNative(Name name, final Object obj) throws NamingException {
        throw readOnlyContext();
    }

    public void rebind(final String name, final Object obj) throws NamingException {
        Assert.checkNotNullParam("name", name);
        // reuse empty check below
        rebind(getNativeNameParser().parse(name), obj);
    }

    public void rebind(final Name name, final Object obj) throws NamingException {
        Assert.checkNotNullParam("name", name);
        if (name.isEmpty()) {
            throw Messages.log.invalidEmptyName();
        }
        if (name instanceof CompositeName) {
            // re-check for empty
            rebind(decomposeName((CompositeName) name), obj);
        } else {
            rebindNative(name, obj);
        }
    }

    protected void rebindNative(Name name, final Object obj) throws NamingException {
        throw readOnlyContext();
    }

    public void unbind(final String name) throws NamingException {
        Assert.checkNotNullParam("name", name);
        // reuse empty check below
        unbind(getNativeNameParser().parse(name));
    }

    public void unbind(final Name name) throws NamingException {
        Assert.checkNotNullParam("name", name);
        if (name.isEmpty()) {
            throw Messages.log.invalidEmptyName();
        }
        if (name instanceof CompositeName) {
            // re-check for empty
            unbind(decomposeName((CompositeName) name));
        } else {
            unbindNative(name);
        }
    }

    protected void unbindNative(Name name) throws NamingException {
        throw readOnlyContext();
    }

    public void rename(final String oldName, final String newName) throws NamingException {
        Assert.checkNotNullParam("oldName", oldName);
        Assert.checkNotNullParam("newName", newName);
        renameNative(getNativeNameParser().parse(oldName), getNativeNameParser().parse(newName));
    }

    public void rename(final Name oldName, final Name newName) throws NamingException {
        Assert.checkNotNullParam("oldName", oldName);
        Assert.checkNotNullParam("newName", newName);
        if (oldName.isEmpty() || newName.isEmpty()) {
            throw Messages.log.invalidEmptyName();
        }
        final Name nativeOldName, nativeNewName;
        if (oldName instanceof CompositeName) {
            nativeOldName = decomposeName((CompositeName) oldName);
        } else {
            nativeOldName = oldName;
        }
        if (newName instanceof CompositeName) {
            nativeNewName = decomposeName((CompositeName) newName);
        } else {
            nativeNewName = newName;
        }
        // re-check for empty
        if (nativeOldName.isEmpty() || nativeNewName.isEmpty()) {
            throw Messages.log.invalidEmptyName();
        }
        renameNative(nativeOldName, nativeNewName);
    }

    protected void renameNative(Name oldName, Name newName) throws NamingException {
        throw readOnlyContext();
    }

    public CloseableNamingEnumeration<NameClassPair> list(final String name) throws NamingException {
        Assert.checkNotNullParam("name", name);
        return listNative(getNativeNameParser().parse(name));
    }

    public CloseableNamingEnumeration<NameClassPair> list(final Name name) throws NamingException {
        Assert.checkNotNullParam("name", name);
        if (name instanceof CompositeName) {
            return listNative(decomposeName((CompositeName) name));
        } else {
            return listNative(name);
        }
    }

    protected abstract CloseableNamingEnumeration<NameClassPair> listNative(Name name) throws NamingException;

    public CloseableNamingEnumeration<Binding> listBindings(final String name) throws NamingException {
        Assert.checkNotNullParam("name", name);
        return listBindingsNative(getNativeNameParser().parse(name));
    }

    public CloseableNamingEnumeration<Binding> listBindings(final Name name) throws NamingException {
        Assert.checkNotNullParam("name", name);
        if (name instanceof CompositeName) {
            return listBindingsNative(decomposeName((CompositeName) name));
        } else {
            return listBindingsNative(name);
        }
    }

    protected abstract CloseableNamingEnumeration<Binding> listBindingsNative(Name name) throws NamingException;

    public void destroySubcontext(final String name) throws NamingException {
        Assert.checkNotNullParam("name", name);
        // reuse empty check below
        destroySubcontext(getNativeNameParser().parse(name));
    }

    public void destroySubcontext(final Name name) throws NamingException {
        Assert.checkNotNullParam("name", name);
        if (name.isEmpty()) {
            throw Messages.log.invalidEmptyName();
        }
        if (name instanceof CompositeName) {
            // re-check for empty
            destroySubcontext(decomposeName((CompositeName) name));
        } else {
            destroySubcontextNative(name);
        }
    }

    protected void destroySubcontextNative(final Name name) throws NamingException {
        throw readOnlyContext();
    }

    public Context createSubcontext(final String name) throws NamingException {
        Assert.checkNotNullParam("name", name);
        // reuse empty check below
        return createSubcontext(getNativeNameParser().parse(name));
    }

    public Context createSubcontext(final Name name) throws NamingException {
        Assert.checkNotNullParam("name", name);
        if (name.isEmpty()) {
            throw Messages.log.invalidEmptyName();
        }
        if (name instanceof CompositeName) {
            // re-check for empty
            return createSubcontext(decomposeName((CompositeName) name));
        } else {
            return createSubcontextNative(name);
        }
    }

    protected Context createSubcontextNative(Name name) throws NamingException {
        throw readOnlyContext();
    }

    public Name composeName(final Name name, final Name prefix) throws NamingException {
        Name base;
        if (prefix instanceof CompositeName) {
            base = decomposeName((CompositeName) prefix);
        } else {
            base = (Name) prefix.clone();
        }
        if (name instanceof CompositeName) {
            base.addAll(decomposeName((CompositeName) name));
        } else {
            base.addAll(name);
        }
        return base;
    }

    public String composeName(final String name, final String prefix) throws NamingException {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("prefix", prefix);
        return composeName(getNativeNameParser().parse(name), getNativeNameParser().parse(prefix)).toString();
    }

    public NameParser getNameParser(final Name name) throws NamingException {
        return getNativeNameParser();
    }

    public NameParser getNameParser(final String name) throws NamingException {
        return getNativeNameParser();
    }

    protected Name decomposeName(CompositeName compositeName) throws NamingException {
        final NameParser parser = getNativeNameParser();
        if (compositeName.isEmpty()) {
            return parser.parse("");
        }
        final Name name = parser.parse(compositeName.get(0));
        if(name.isEmpty())name.add("");
        for (int i = 1; i < compositeName.size(); i ++) {
            final String part = compositeName.get(i);
            final Name parsed = parser.parse(part);
            // make sure empty segments are preserved
            if (parsed.isEmpty()) parsed.add("");
            name.addAll(parsed);
        }
        return name;
    }

    public Object addToEnvironment(final String propName, final Object propVal) {
        return environment.put(propName, propVal);
    }

    public Object removeFromEnvironment(final String propName) {
        return environment.remove(propName);
    }

    public FastHashtable<String, Object> getEnvironment() throws NamingException {
        return environment;
    }

    protected static NamingException nameNotFound(final Name name) {
        return Messages.log.nameNotFound(name, name);
    }

    protected static NoPermissionException readOnlyContext() {
        return Messages.log.readOnlyContext();
    }

    protected static OperationNotSupportedException notSupported() {
        return Messages.log.notSupported();
    }
}
