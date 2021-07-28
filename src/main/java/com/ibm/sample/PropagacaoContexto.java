package com.ibm.sample;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.tag.Tags;
public class PropagacaoContexto {

	@Autowired
	public Tracer tracer;
	
    protected Span startServerSpan(String operationName, HttpServletRequest request) {
        
        return this.startServerSpan(operationName, request, null);
    }
    
    
    public void  preencheSpan(Span span, Object labels)
    {
    	Class classe = labels.getClass();
    	Method[] metodos = classe.getMethods();
    	for (Method metodo:metodos)
    	{
    		try 
    		{
        		String nome = metodo.getName();
        		if (nome.startsWith("get") && !nome.contentEquals("getClass"))
        		{
        			nome = nome.substring(3);
        			String valor = metodo.invoke(labels, null).toString();
            		span.setTag(nome, valor);
        		}
    		}
    		catch (Exception e)
    		{
    		}
    		
    	}
    }
    

    protected Span startServerSpan(String operationName, HttpServletRequest request, Object labels) {
        HttpServletRequestExtractAdapter carrier = new HttpServletRequestExtractAdapter(request);
        SpanContext parent = tracer.extract(Format.Builtin.HTTP_HEADERS, carrier);
        Span span = tracer.buildSpan(operationName).asChildOf(parent).start();
        Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_SERVER);
        if (labels!=null)
        {
        	preencheSpan(span, labels);
        }
        
        return span;
    }
    
    
    private static class HttpServletRequestExtractAdapter implements TextMap 
    {
        private final Map<String, String> headers;

        HttpServletRequestExtractAdapter(HttpServletRequest request) {
            this.headers = new LinkedHashMap<>();
            Enumeration<String> keys = request.getHeaderNames();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                String value = request.getHeader(key);
                headers.put(key, value);
            }
        }

        @Override
        public Iterator<Entry<String, String>> iterator() {
            return headers.entrySet().iterator();
        }

        @Override
        public void put(String key, String value) {
            throw new UnsupportedOperationException();
        }
    }



}
