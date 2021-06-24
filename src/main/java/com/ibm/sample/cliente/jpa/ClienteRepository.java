package com.ibm.sample.cliente.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ibm.sample.cliente.dto.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

}
