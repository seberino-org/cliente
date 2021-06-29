package com.ibm.sample.cliente;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ibm.sample.cliente.dto.Cliente;
import com.ibm.sample.cliente.dto.RetornoCliente;
import com.ibm.sample.cliente.jpa.ClienteRepository;

@Controller
@RestController
public class ClienteRest {

	@Autowired
	private ClienteRepository clienteJpa;
	
	
	
	@GetMapping("/cliente/pesquisa/{nome}")
	public List<Cliente> recuperaClientes(@PathVariable String nome)
	{
		List<Cliente> lista = clienteJpa.findByNome(nome);
	
		
		return lista;
	}

	
	@DeleteMapping("/cliente/{cpf}")
	public ResponseEntity<RetornoCliente> excluirCliente(@PathVariable Long cpf)
	{
		Optional<Cliente> cliente= clienteJpa.findById(cpf);
	
		RetornoCliente retorno = new RetornoCliente();
		if (cliente.isEmpty())
		{
			return new ResponseEntity<>(HttpStatus.NOT_FOUND); 
		}
		else
		{
			Cliente cli = cliente.get();
			retorno.setCliente(cli);
			clienteJpa.delete(cli);
			retorno.setMensagem( "Cliente Excluido!");
			retorno.setCodigo("202-Excluido");
		}
		
		return ResponseEntity.ok(retorno);
	}
	
	@GetMapping("/cliente/{cpf}")
	public ResponseEntity<RetornoCliente> recuperaCliente(@PathVariable Long cpf)
	{
		Optional<Cliente> cliente= clienteJpa.findById(cpf);
	
		RetornoCliente retorno = new RetornoCliente();
		if (cliente.isEmpty())
		{
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		else
		{
			retorno.setCliente(cliente.get());
			retorno.setMensagem( "Cliente encontrado!");
			retorno.setCodigo("200-FOUND");
		}
		
		return ResponseEntity.ok(retorno);
	}
	
	@PostMapping("/cliente")
	public ResponseEntity<RetornoCliente> incluiCliente(@RequestBody Cliente cliente)
	{
		try
		{
			validaCliente(cliente);
			
			Optional<Cliente> clienteConsulta= clienteJpa.findById(cliente.getCpf());
			RetornoCliente retorno = new RetornoCliente();
			if (clienteConsulta.isPresent())
			{
				retorno.setCliente(clienteConsulta.get());
				retorno.setMensagem( "Já existe cliente cadastrado com esse CPF!");
				retorno.setCodigo("303-CLIENT EXIST");
				return new ResponseEntity<>(HttpStatus.FOUND);
			}
			
			clienteJpa.save(cliente);
			
			retorno.setCliente(cliente);
			retorno.setMensagem( "Cliente reigstrado com sucesso!");
			retorno.setCodigo("201-CREATED");
			
			return ResponseEntity.ok(retorno);
		}
		catch (Exception e)
		{
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}
	
	private void validaCliente(Cliente cliente) throws Exception
	{
		if (cliente==null)
		{
			throw new Exception("Payload inváido, não foram encontrados os dados do cliente");
		}
		if (cliente.getCpf()==null || cliente.getCpf()==0)
		{
			throw new Exception("CPF é um campo obrigatório");
		}
		if (cliente.getNome()==null || cliente.getNome().length()==0)
		{
			throw new Exception("Nome é um campo obrigatório");
		}
	}
}
