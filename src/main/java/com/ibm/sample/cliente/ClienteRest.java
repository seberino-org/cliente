package com.ibm.sample.cliente;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
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
	
	@GetMapping("/cliente/{cpf}")
	public RetornoCliente recuperaCliente(@PathVariable Long cpf)
	{
		Optional<Cliente> cliente= clienteJpa.findById(cpf);
	
		RetornoCliente retorno = new RetornoCliente();
		if (cliente.isEmpty())
		{
			retorno.setCliente(null);
			retorno.setMensagem("Cliente Não encontrado!");
			retorno.setCodigo("404-NOT FOUND");
		}
		else
		{
			retorno.setCliente(cliente.get());
			retorno.setMensagem( "Cliente encontrado!");
			retorno.setCodigo("200-FOUND");
		}
		
		return retorno;
	}
	
	@PostMapping("/cliente")
	public RetornoCliente incluiCliente(@RequestBody Cliente cliente)
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
				return retorno;
			}
			
			clienteJpa.save(cliente);
			retorno.setCliente(cliente);
			retorno.setMensagem( "Cliente reigstrado com sucesso!");
			retorno.setCodigo("201-CREATED");
			
			return retorno;
		}
		catch (Exception e)
		{
			RetornoCliente retorno = new RetornoCliente();
			retorno.setCliente(cliente);
			retorno.setMensagem( "Problema com os dados informados: " + e.getMessage());
			retorno.setCodigo("400-BAD REQUEST");
			
			return retorno;
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
