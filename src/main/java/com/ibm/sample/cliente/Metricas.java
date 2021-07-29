package com.ibm.sample.cliente;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class Metricas {
    
    @Autowired
    MeterRegistry registry;

    @Bean
    public Counter contadorNovosCadastros()
    {
        return registry.counter("cliente.cadastro", "status", "200");
    }

    @Bean
    public Counter contadorNovosCadastrosFalha()
    {
        return registry.counter("cliente.cadastro", "status", "500");
    }
    
    @Bean
    public Counter contadorNovosCadastrosJaExiste()
    {
        return registry.counter("cliente.cadastro", "status", "208");
    }
    @Bean
    public Counter contadorExclusaoCliente()
    {
        return registry.counter("cliente.exclusao", "status", "200");
    }
}

