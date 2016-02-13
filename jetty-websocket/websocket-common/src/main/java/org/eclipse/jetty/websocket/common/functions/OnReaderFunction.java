//
//  ========================================================================
//  Copyright (c) 1995-2016 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.websocket.common.functions;

import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.common.InvalidSignatureException;
import org.eclipse.jetty.websocket.common.util.DynamicArgs;
import org.eclipse.jetty.websocket.common.util.ExactSignature;
import org.eclipse.jetty.websocket.common.util.ReflectUtils;

/**
 * Jetty {@link WebSocket} {@link OnWebSocketMessage} method {@link Function} for TEXT/{@link Reader} streaming types
 */
public class OnReaderFunction implements Function<Reader, Void>
{
    private static final DynamicArgs.Builder ARGBUILDER;
    private static final int SESSION = 1;
    private static final int STREAM = 2;

    static
    {
        ARGBUILDER = new DynamicArgs.Builder();
        ARGBUILDER.addSignature(new ExactSignature(Reader.class).indexedAs(STREAM));
        ARGBUILDER.addSignature(new ExactSignature(Session.class,Reader.class).indexedAs(SESSION,STREAM));
    }

    public static DynamicArgs.Builder getDynamicArgsBuilder()
    {
        return ARGBUILDER;
    }
    
    public static boolean hasMatchingSignature(Method method)
    {
        return ARGBUILDER.hasMatchingSignature(method);
    }

    private final Session session;
    private final Object endpoint;
    private final Method method;
    private final DynamicArgs callable;

    public OnReaderFunction(Session session, Object endpoint, Method method)
    {
        this.session = session;
        this.endpoint = endpoint;
        this.method = method;

        ReflectUtils.assertIsAnnotated(method,OnWebSocketMessage.class);
        ReflectUtils.assertIsPublicNonStatic(method);
        ReflectUtils.assertIsReturn(method,Void.TYPE);

        this.callable = ARGBUILDER.build(method);
        if (this.callable == null)
        {
            throw InvalidSignatureException.build(method,OnWebSocketMessage.class,ARGBUILDER);
        }
        this.callable.setArgReferences(SESSION,STREAM);
    }

    @Override
    public Void apply(Reader stream)
    {
        Object args[] = this.callable.toArgs(session,stream);
        try
        {
            method.invoke(endpoint,args);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            throw new WebSocketException("Unable to call text message method " + ReflectUtils.toString(endpoint.getClass(),method),e);
        }
        return null;
    }
}