package com.ibm.sample.cliente;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

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

import io.opentracing.Span;

import com.ibm.sample.PropagacaoContexto;
import com.ibm.sample.cliente.dto.Cliente;
import com.ibm.sample.cliente.dto.RetornoCliente;
import com.ibm.sample.cliente.jpa.ClienteRepository;

@Controller
@RestController
public class ClienteRest extends PropagacaoContexto {

	@Autowired
	private ClienteRepository clienteJpa;
	Logger logger = LoggerFactory.getLogger(ClienteRest.class);
	
	
	@GetMapping("/cliente/pesquisa/{nome}")
	public List<Cliente> recuperaClientes(@PathVariable String nome, HttpServletRequest request)
	{
		logger.debug("[recuperaClientes] " + nome);
		Span span = this.startServerSpan("consultaBaseDados", request);
		List<Cliente> lista = clienteJpa.findByNome(nome);
		logger.debug("Encontrado: " + lista.size() + " clientes na pesquisa pelo nome " + nome);
		span.finish();
		return lista;
	}

	
	@DeleteMapping("/cliente/{cpf}")
	public ResponseEntity<RetornoCliente> excluirCliente(@PathVariable Long cpf, HttpServletRequest request)
	{
		logger.debug("[excluirCliente] " + cpf);
		Span span = this.startServerSpan("exclusaoClienteBaseDados", request);
		span.setTag("cpf", cpf);
		Optional<Cliente> cliente= clienteJpa.findById(cpf);
		RetornoCliente retorno = new RetornoCliente();
		if (cliente.isEmpty())
		{
			logger.info("Cliente não encontrado para exclusão: " + cpf);
			span.finish();
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
		span.finish();
		return ResponseEntity.ok(retorno);
	}
	
	@GetMapping("/cliente/{cpf}")
	public ResponseEntity<RetornoCliente> recuperaCliente(@PathVariable Long cpf, HttpServletRequest request)
	{
		logger.debug("[recuperaCliente] " + cpf);
		Span span = this.startServerSpan("consultaBaseDados", request);
		span.setTag("cpf", cpf);
		Optional<Cliente> cliente= clienteJpa.findById(cpf);
	
		RetornoCliente retorno = new RetornoCliente();
		if (cliente.isEmpty())
		{
			logger.info("Cliente não encontrado com o CPF: " + cpf);
			span.log( "cliente não encontrado com esse cpf");
			span.finish();
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		else
		{
			retorno.setCliente(cliente.get());
			logger.debug("Cliente encontrado: " + retorno.getCliente().toString());
			retorno.setMensagem( "Cliente encontrado!");
			retorno.setCodigo("200-FOUND");
			span.setTag("name", retorno.getCliente().getNome());
		}
		span.finish();
		return ResponseEntity.ok(retorno);
	}
	
	@PostMapping("/cliente")
	public ResponseEntity<RetornoCliente> incluiCliente(@RequestBody Cliente cliente, HttpServletRequest request)
	{
		logger.debug("[incluiCliente] ");
		Span span = this.startServerSpan("inclusaoClienteBaseDados", request);

		try
		{
			logger.debug("Vai validar os dados do cliente para cadastro!");
			validaCliente(cliente);
			span.setTag("cpf", cliente.getCpf());
			span.setTag("nome", cliente.getNome());
			logger.debug("Dados validados com sucesso!");
			
			logger.debug("Vai pesquisar se já não existe cliente cadastrado com esse CPF");
			Optional<Cliente> clienteConsulta= clienteJpa.findById(cliente.getCpf());
			RetornoCliente retorno = new RetornoCliente();
			if (clienteConsulta.isPresent())
			{
				span.log("Já existe cliente cadastrado com esse CPF");
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
		finally
		{
			span.finish();
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
