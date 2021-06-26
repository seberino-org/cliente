package com.ibm.sample.cliente.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ibm.sample.cliente.dto.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

	@Query("from Cliente where nome LIKE %:nome%")    
	List<Cliente> findByNome(@Param("nome")String nome);
}
