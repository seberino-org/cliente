package com.ibm.sample.cliente;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	Logger logger = LoggerFactory.getLogger(ClienteRest.class);
	
	
	@GetMapping("/cliente/pesquisa/{nome}")
	public List<Cliente> recuperaClientes(@PathVariable String nome)
	{
		logger.debug("[recuperaClientes] " + nome);
		List<Cliente> lista = clienteJpa.findByNome(nome);
		logger.debug("Encontrado: " + lista.size() + " clientes na pesquisa pelo nome " + nome);
		
		return lista;
	}

	
	@DeleteMapping("/cliente/{cpf}")
	public ResponseEntity<RetornoCliente> excluirCliente(@PathVariable Long cpf)
	{
		logger.debug("[excluirCliente] " + cpf);
		Optional<Cliente> cliente= clienteJpa.findById(cpf);
		RetornoCliente retorno = new RetornoCliente();
		if (cliente.isEmpty())
		{
			logger.info("Cliente não encontrado para exclusão: " + cpf);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND); 
		}
		else
		{
			Cliente cli = cliente.get();
			retorno.setCliente(cli);
			logger.debug("enviando comando para a base de dados para exclusao dos dados do cliente: " + cli.toString());
			clienteJpa.delete(cli);
			retorno.setMensagem( "Cliente Excluido!");
			logger.debug("Cliente excluido com sucesso " + cli.toString());
			retorno.setCodigo("202-Excluido");
		}
		
		return ResponseEntity.ok(retorno);
	}
	
	@GetMapping("/cliente/{cpf}")
	public ResponseEntity<RetornoCliente> recuperaCliente(@PathVariable Long cpf)
	{
		logger.debug("[recuperaCliente] " + cpf);
		Optional<Cliente> cliente= clienteJpa.findById(cpf);
	
		RetornoCliente retorno = new RetornoCliente();
		if (cliente.isEmpty())
		{
			logger.info("Cliente não encontrado com o CPF: " + cpf);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		else
		{
			retorno.setCliente(cliente.get());
			logger.debug("Cliente encontrado: " + retorno.getCliente().toString());
			retorno.setMensagem( "Cliente encontrado!");
			retorno.setCodigo("200-FOUND");
		}
		
		return ResponseEntity.ok(retorno);
	}
	
	@PostMapping("/cliente")
	public ResponseEntity<RetornoCliente> incluiCliente(@RequestBody Cliente cliente)
	{
		logger.debug("[incluiCliente] ");
		try
		{
			logger.debug("Vai validar os dados do cliente para cadastro!");
			validaCliente(cliente);
			logger.debug("Dados validados com sucesso!");
			
			logger.debug("Vai pesquisar se já não existe cliente cadastrado com esse CPF")
			Optional<Cliente> clienteConsulta= clienteJpa.findById(cliente.getCpf());
			RetornoCliente retorno = new RetornoCliente();
			if (clienteConsulta.isPresent())
			{
				logger.info("Já existe cliente cadastrado com o CPF: " + cliente.getCpf());
				retorno.setCliente(clienteConsulta.get());
				retorno.setMensagem( "Já existe cliente cadastrado com esse CPF!");
				retorno.setCodigo("303-CLIENT EXIST");
				return new ResponseEntity<>(HttpStatus.FOUND);
			}
			
			clienteJpa.save(cliente);
			logger.info("Cliente armazenado na base de dados com sucesso! " + cliente.toString());

			retorno.setCliente(cliente);
			retorno.setMensagem( "Cliente registrado com sucesso!");
			retorno.setCodigo("201-CREATED");
			
			return ResponseEntity.ok(retorno);
		}
		catch (Exception e)
		{
			logger.error("Falha ao cadastrar cliente " + e.getMessage(), e);
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
